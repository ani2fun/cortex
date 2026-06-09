---
title: Swap Flannel for Calico
summary: The Tigera operator pattern, why we pick VXLAN over BGP, and the MTU math that ends with `1370`. Three encapsulations stacked (VXLAN over WireGuard inside Ethernet) leaves exactly that much room for payload — get this number wrong and TCP everywhere goes mysteriously slow.
---

## Why Calico

In a homelab specifically, Calico buys you three things Flannel doesn't:

1. **NetworkPolicy that actually does something.** Postgres-internal-only is a NetworkPolicy. eBPF-accelerated DNS lookup is Calico-only. Egress restrictions to specific external IPs are Calico-only.
2. **A CNI that runs cleanly over an underlay you don't control.** Calico's VXLAN mode encapsulates pod traffic into UDP packets that the WireGuard mesh carries opaquely. Flannel's VXLAN does this too, but Flannel's BGP mode (which is what most production deployments use) requires layer-2 adjacency between nodes — incompatible with a multi-site WireGuard mesh.
3. **The Tigera operator pattern.** Calico's installation is *one CRD* (`Installation`) that the operator reconciles into hundreds of manifests. When you upgrade Calico, you change the version field on the operator deployment and walk away. Flannel upgrades are still mostly "delete the old DaemonSet, apply the new one" — fine, but lower abstraction.

What we *don't* use Calico for: BGP peering, IPv6, eBPF data plane, IPSec encryption (we already have WireGuard underneath). The full Calico feature set is enterprise-grade. We're using maybe 10% of it.

## The packet path

Three encapsulations stacked on top of each other:

```d2
direction: down

app: App in pod on wk-1
pod_iface: pod network interface (cali...)
veth: veth pair (host side)
host_route: host routing table

vxlan: VXLAN encap\nUDP :4789\n+50 bytes
wg: WireGuard encap\nUDP :51820\n+60 bytes (incl. ip+udp hdrs)
phys: Physical NIC\n(Ethernet 1500 MTU)

dest: Pod on wk-2

app -> pod_iface
pod_iface -> veth
veth -> host_route
host_route -> vxlan: hits ipPool 10.42.0.0/16
vxlan -> wg: target IP is 172.27.15.13
wg -> phys
phys -> dest
```

Reading bottom-up:

- The physical Ethernet frame can carry **1500 bytes** of payload (the standard MTU). 
- WireGuard adds **60 bytes** of headers, leaving **1440 bytes** of payload inside the tunnel. (Some sources cite 80 bytes; the actual overhead depends on whether IPv4 or IPv6 carries the encrypted UDP, but on this homelab WG inside IPv4 is 60.)
- We set `MTU = 1420` on `wg0` to be safe (a 20-byte buffer for occasional path quirks).
- Calico VXLAN inside that adds another **50 bytes**, leaving **1370 bytes** of payload for actual pod-to-pod traffic.

If we set the Calico MTU too high (e.g., the default 1450), TCP packets get fragmented at the WireGuard layer, the receiver's reassembly fails sometimes, retransmits cascade, and TCP-but-not-ping goes weird. This is one of the most maddening homelab bugs because everything *kind of* works.

The fix is the line in the Calico Installation CRD that says `mtu: 1370`.

## Install the Tigera operator

Two manifests from the upstream Calico project, then our custom Installation CRD:

```bash
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
CALICO_VERSION="v3.31.4"

kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/${CALICO_VERSION}/manifests/operator-crds.yaml
kubectl create -f https://raw.githubusercontent.com/projectcalico/calico/${CALICO_VERSION}/manifests/tigera-operator.yaml
```

The first file installs the CRDs the operator reconciles (Installation, IPPool, BGPConfiguration, …). The second file installs the operator itself — a Deployment in `tigera-operator` namespace that watches those CRDs.

`kubectl get pods -n tigera-operator` should show one running pod within ~30 seconds.

## Apply the Installation CRD

```yaml
# /tmp/calico-installation.yaml
apiVersion: operator.tigera.io/v1
kind: Installation
metadata:
  name: default
spec:
  calicoNetwork:
    bgp: Disabled
    mtu: 1370
    nodeAddressAutodetectionV4:
      kubernetes: NodeInternalIP
    ipPools:
      - cidr: 10.42.0.0/16
        encapsulation: VXLAN
        natOutgoing: Enabled
        nodeSelector: all()
---
apiVersion: operator.tigera.io/v1
kind: APIServer
metadata:
  name: default
spec: {}
```

Field by field:

- **`bgp: Disabled`** — we're not peering with anything. WireGuard is the underlay; we don't need Calico to advertise routes via BGP.
- **`mtu: 1370`** — see the math above. This is the Calico-VXLAN MTU.
- **`nodeAddressAutodetectionV4: kubernetes: NodeInternalIP`** — use the node IP K3s reported (which is the WireGuard IP, because of `--node-ip=172.27.15.12` in the K3s flags). Without this, Calico picks the first non-loopback IP on the host, which on home boxes is the LAN IP — and we want pod traffic over WireGuard, not the LAN.
- **`ipPools[0].cidr: 10.42.0.0/16`** — matches K3s' `--cluster-cidr=10.42.0.0/16`. Pod IPs come from here.
- **`encapsulation: VXLAN`** — UDP encapsulation, MTU-aware, fine over WireGuard. Other options (IPIP, IPIPCrossSubnet, None) require BGP or layer-2 adjacency.
- **`natOutgoing: Enabled`** — pods hitting the internet get SNAT'd to the node IP. Without this, return traffic for pod egress wouldn't find its way back.

Apply:

```bash
kubectl apply -f /tmp/calico-installation.yaml
```

The operator picks up the Installation, reconciles it into a calico-node DaemonSet plus calico-kube-controllers, and starts pulling images. First-time install takes 1–2 minutes. Watch:

```bash
watch -n2 'kubectl get pods -n calico-system'
```

You'll see images pulling, then `Init: 0/3`, then `Running 1/1` for `calico-node-<random>` and `calico-kube-controllers-<random>`. When all are running, `ms-1` should flip to Ready:

```bash
kubectl get nodes
# NAME   STATUS   ROLES                  AGE   VERSION
# ms-1   Ready    control-plane,master   5m    v1.35.1+k3s1
```

## The first sanity check

Spin up a throwaway pod and confirm it gets a pod IP from the right pool:

```bash
kubectl run -it --rm test --image=alpine --restart=Never -- sh
# inside the pod:
ip addr show eth0
# eth0    inet 10.42.0.5/32   ← in cluster CIDR ✓
ping -c1 1.1.1.1
# 64 bytes from 1.1.1.1 ... time=2.5 ms       ← egress works ✓
exit
```

If both work, Calico is healthy.

## What about Cilium?

A reasonable question. Cilium is the cool kid: eBPF data plane, no kube-proxy, beautiful service map dashboard, every "modern" Kubernetes shop is on it. Why not Cilium?

For a homelab specifically:

- **Stability over flair.** Cilium's eBPF programs require kernel features that are still being added; Calico's VXLAN + iptables data plane works on every kernel from 5.x onwards.
- **One less moving piece.** Cilium replaces kube-proxy; Calico complements it. With Calico we keep the standard Kubernetes service implementation, which is well-known when something goes wrong.
- **Cilium over WireGuard is double-encrypted.** Cilium can do its own encryption, but layering it under WireGuard is wasteful. Calico has no encryption layer of its own, so it's a clean overlay.

If you're running this professionally on bare metal in one datacenter, Cilium is the better call. For a homelab on a residential ISP, Calico is a more boring (= more reliable) choice.

## What you should have now

- `kubectl get nodes` shows `ms-1 Ready`
- `kubectl get pods -n calico-system` shows all pods Running
- `kubectl get installation default -o yaml` shows MTU 1370 and the right IP pool
- A throwaway pod can ping the internet

Next, the workers join.

→ Next: [Join the workers](/cortex/homelab-from-scratch/kubernetes-base/join-the-workers)
