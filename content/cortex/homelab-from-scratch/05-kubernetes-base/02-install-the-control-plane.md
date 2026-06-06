---
title: Install the control-plane
summary: K3s server on `ms-1` with the right flags to disable Flannel, Traefik, network policy, and ServiceLB. The DNS resolv.conf trick that keeps systemd-resolved from clobbering CoreDNS, and the post-install state your kubeconfig should look like.
---

## What "control-plane install" means

```mermaid
---
config:
  theme: base
  themeVariables:
    primaryColor: "#dbeafe"
    primaryBorderColor: "#3b82f6"
    primaryTextColor: "#1e3a5f"
    lineColor: "#64748b"
    secondaryColor: "#ede9fe"
    tertiaryColor: "#fef9c3"
---
sequenceDiagram
  participant Op as You (root@ms-1)
  participant Curl as get.k3s.io
  participant Sys as systemd
  participant K3s as k3s.service
  participant Kubectl as kubectl

  Op->>Curl: curl -sfL https://get.k3s.io
  Curl-->>Op: install script (bash)
  Op->>Op: K3S_KUBECONFIG_MODE=644 INSTALL_K3S_EXEC="server ..." sh -
  Op->>Sys: install /usr/local/bin/k3s + k3s.service
  Sys->>K3s: enable + start
  K3s-->>K3s: spin up apiserver / scheduler / controller-mgr / kubelet
  K3s-->>Sys: ready
  Op->>Kubectl: kubectl get nodes
  Kubectl->>K3s: GET /api/v1/nodes
  K3s-->>Kubectl: ms-1 NotReady (no CNI yet)
```

The K3s install script is one of the friendlier ones in the Kubernetes ecosystem: a Bash script that installs the binary, writes a systemd unit, generates a kubeconfig, starts the service. We pass it our flags as a `INSTALL_K3S_EXEC` environment variable.

After this chapter, the control-plane is up but `kubectl get nodes` reports `NotReady` — Calico, the network plugin we asked K3s to *not* install, doesn't exist yet. That's expected. The next chapter installs it.

## Step 1: the resolv.conf trick

Ubuntu 24.04 uses `systemd-resolved` for DNS. Its symlink at `/etc/resolv.conf` points at a stub resolver that sometimes returns weird answers when CoreDNS (the cluster DNS) tries to look up upstream names. The fix is to point K3s at the *real* upstream resolver list directly:

```bash
sudo mkdir -p /etc/rancher/k3s
sudo ln -sf /run/systemd/resolve/resolv.conf /etc/rancher/k3s/k3s-resolv.conf

# Confirm: should show real nameservers, not 127.0.0.53
readlink -f /etc/rancher/k3s/k3s-resolv.conf
grep -E '^(nameserver|search|options)' /etc/rancher/k3s/k3s-resolv.conf
```

If `grep` shows `nameserver 127.0.0.53`, the resolv.conf is wrong. Use `/etc/resolv.conf.original` or `/etc/systemd/resolved.conf` upstream nameservers explicitly.

## Step 2: the install command

On `ms-1`, as root:

```bash
export INSTALL_K3S_VERSION="v1.35.1+k3s1"

curl -sfL https://get.k3s.io | \
  K3S_KUBECONFIG_MODE="644" \
  INSTALL_K3S_EXEC="server \
    --node-ip=172.27.15.12 \
    --advertise-address=172.27.15.12 \
    --tls-san=172.27.15.12 \
    --flannel-backend=none \
    --disable-network-policy \
    --disable=traefik \
    --disable=servicelb \
    --resolv-conf=/etc/rancher/k3s/k3s-resolv.conf \
    --cluster-cidr=10.42.0.0/16 \
    --service-cidr=10.43.0.0/16 \
    --node-label homelab.kakde.eu/role=server" \
  sh -
```

Each flag, line by line:

| Flag | Why |
|---|---|
| `--node-ip=172.27.15.12` | Bind kubelet's reported address to the WireGuard IP. Pod-to-pod traffic will use this. |
| `--advertise-address=172.27.15.12` | Tell other components where to reach the apiserver. Same address. |
| `--tls-san=172.27.15.12` | Add this IP as a Subject Alternative Name on the apiserver's TLS cert. Without this, agents fail TLS verification. |
| `--flannel-backend=none` | Don't install Flannel. We'll install Calico in the next chapter. |
| `--disable-network-policy` | Don't install K3s' bundled NetworkPolicy implementation. Calico provides one. |
| `--disable=traefik` | Don't install the bundled Traefik. We'll install our own on the edge. |
| `--disable=servicelb` | Don't install ServiceLB. Public traffic terminates at Traefik on the edge, not at NodePorts. |
| `--resolv-conf=...` | Use the upstream resolv.conf instead of systemd-resolved's stub. |
| `--cluster-cidr=10.42.0.0/16` | Pod IPs will come from this range. Default is also `10.42.0.0/16`; set explicitly so future-you can grep. |
| `--service-cidr=10.43.0.0/16` | Service VIPs (ClusterIP) come from this range. |
| `--node-label homelab.kakde.eu/role=server` | A non-functional label that makes `kubectl get nodes --show-labels` self-documenting. |
| `K3S_KUBECONFIG_MODE=644` | Make `/etc/rancher/k3s/k3s.yaml` readable by non-root users on this host. Convenient for SSH-as-non-root. |

The install script downloads the binary, writes `/etc/systemd/system/k3s.service`, starts it, and exits. Total time: 30–60 seconds.

## Step 3: confirm the service is healthy

```bash
systemctl status k3s.service
# ● k3s.service - Lightweight Kubernetes
#      Loaded: loaded (/etc/systemd/system/k3s.service; enabled)
#      Active: active (running)

journalctl -u k3s.service -n 50 --no-pager | tail -10
# Look for "Node controller sync successful" near the end.
```

If `Active: failed`, read the journalctl output. The most common failures:

- **"connection refused" on :6443** — usually means another process already holds the port. Check `ss -lntp | grep 6443`.
- **"x509: certificate signed by unknown authority"** — usually means the system clock is wrong. Re-check `timedatectl status`.
- **"failed to bind to 172.27.15.12"** — WireGuard interface isn't up. Re-check `ip addr show wg0`.

## Step 4: the first kubectl

K3s wrote `/etc/rancher/k3s/k3s.yaml`. That's a kubeconfig that points at `https://127.0.0.1:6443`. To use it from `ms-1`:

```bash
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl get nodes
```

You should see:

```
NAME   STATUS     ROLES                  AGE   VERSION
ms-1   NotReady   control-plane,master   30s   v1.35.1+k3s1
```

`NotReady` is expected — there's no CNI yet, so the kubelet refuses to mark itself ready. Calico in the next chapter fixes this.

To use `kubectl` from your laptop instead, copy the kubeconfig and change the server URL to the WireGuard IP:

```bash
# On your laptop
scp root@172.27.15.12:/etc/rancher/k3s/k3s.yaml ~/.kube/homelab.yaml
sed -i.bak 's@server: https://127\.0\.0\.1:6443@server: https://172.27.15.12:6443@' ~/.kube/homelab.yaml
chmod 600 ~/.kube/homelab.yaml

export KUBECONFIG=~/.kube/homelab.yaml
kubectl get nodes
```

(For this to work, your laptop must have a `wg0` interface that can reach `172.27.15.12` — the admin peer setup from the previous section.)

## Step 5: the node token

When you join the workers in two chapters, they need a token to authenticate to this server. K3s generated one already:

```bash
cat /var/lib/rancher/k3s/server/node-token
# K10c8...::server:5d40...
```

Save it. You'll paste it into the agent install commands as `K3S_TOKEN=...`.

## What you should have now

- `k3s.service` running on `ms-1`
- `kubectl get nodes` returns `ms-1 NotReady` — exactly one node, in NotReady because no CNI
- The kubeconfig copied to your laptop with the WireGuard server URL
- The node-token saved somewhere safe

Next: install Calico. After that, `ms-1` will go to `Ready` and we can join the workers.

→ Next: [Swap Flannel for Calico](/cortex/homelab-from-scratch/kubernetes-base-swap-flannel-for-calico)
