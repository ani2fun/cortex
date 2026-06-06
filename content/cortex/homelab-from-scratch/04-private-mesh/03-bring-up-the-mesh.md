---
title: Bring up the mesh
summary: Four `wg0.conf` files, one `wg-quick` service per node, persistent keepalive on the home side, and a verification ladder that ends with every node pinging every other node over the tunnel. The full-mesh diagram, with router port-forwards explained.
---

## The full mesh, with all the moving parts

```d2
direction: down

internet: Public internet {
  shape: cloud
}

router: Home router\n203.0.113.10\nport-forwards 51820/51821/51822/udp {
  shape: rectangle
}

vm1: vm-1\n198.51.100.25\n172.27.15.31 {
  shape: rectangle
}

ms1: ms-1\n192.168.15.2\n172.27.15.12 {
  shape: rectangle
}
wk1: wk-1\n192.168.15.3\n172.27.15.11 {
  shape: rectangle
}
wk2: wk-2\n192.168.15.4\n172.27.15.13 {
  shape: rectangle
}

internet -> vm1: 51820/udp directly
internet -> router: 51820 → wk-1\n51821 → ms-1\n51822 → wk-2
router -> wk1: 51820/udp
router -> ms1: 51820/udp
router -> wk2: 51820/udp

vm1 -> ms1: WG
vm1 -> wk1: WG
vm1 -> wk2: WG
ms1 -> wk1: WG (LAN)
ms1 -> wk2: WG (LAN)
wk1 -> wk2: WG (LAN)
```

Two parts to read:

- **Bottom half** — six bidirectional WireGuard tunnels. Every node is a peer of every other node. Inside the home LAN, the three boxes talk to each other over their LAN IPs (no NAT involved). To the cloud edge, they reach across the public internet through the home router's port-forwards.
- **Top half** — three different public UDP ports on the same home WAN IP, each forwarded to a *different* home node's WireGuard listener. This is how `vm-1` reaches each of the three home nodes individually. Each home node listens on the same port (`51820`) inside the home network; the home router's NAT table maps the differing public ports to the right LAN IP.

If your home router can't do per-port forwarding to different LAN IPs, you can simplify by giving only one home node (e.g. `wk-1`) a port-forward and routing the other home nodes' WireGuard traffic *through* `wk-1`. Slightly more complex; ask in chapter 4 if you need it.

## The router port-forward

On your home router's admin page, in the **Port Forwarding** section:

| Public port | Protocol | Internal IP | Internal port |
|---|---|---|---|
| `51820` | UDP | `192.168.15.3` (`wk-1`) | `51820` |
| `51821` | UDP | `192.168.15.2` (`ms-1`) | `51820` |
| `51822` | UDP | `192.168.15.4` (`wk-2`) | `51820` |

If your router asks "TCP, UDP, or both", pick **UDP only**. WireGuard never uses TCP.

If your router treats *the source IP that triggered the port-forward* as part of the rule (some carrier-grade NATs do this), forward unconditionally — `vm-1` is the source, but you don't want to hard-code its IP.

## The four `wg0.conf` files

Make the keys substitutions from `keys.txt`. Edit each file on the right node, install at `/etc/wireguard/wg0.conf`, mode `600`.

### `ms-1` — `/etc/wireguard/wg0.conf`

```ini
[Interface]
Address    = 172.27.15.12/32
ListenPort = 51820
PrivateKey = <MS_1_PRIVATE_KEY>
MTU        = 1420
SaveConfig = false

# Route the admin laptop over wg0
PostUp   = ip route replace 172.27.15.50/32 dev wg0
PostDown = ip route del 172.27.15.50/32 dev wg0 || true

[Peer]
# vm-1 (cloud edge, public)
PublicKey           = <VM_1_PUBLIC_KEY>
AllowedIPs          = 172.27.15.31/32
Endpoint            = 198.51.100.25:51820
PersistentKeepalive = 25

[Peer]
# wk-1 (LAN)
PublicKey  = <WK_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.11/32
Endpoint   = 192.168.15.3:51820

[Peer]
# wk-2 (LAN)
PublicKey  = <WK_2_PUBLIC_KEY>
AllowedIPs = 172.27.15.13/32
Endpoint   = 192.168.15.4:51820

[Peer]
# laptop / admin
PublicKey  = <ADMIN_PUBLIC_KEY>
AllowedIPs = 172.27.15.50/32
```

### `wk-1` — `/etc/wireguard/wg0.conf`

```ini
[Interface]
Address    = 172.27.15.11/32
ListenPort = 51820
PrivateKey = <WK_1_PRIVATE_KEY>
MTU        = 1420
SaveConfig = false

PostUp   = ip route replace 172.27.15.50/32 dev wg0
PostDown = ip route del 172.27.15.50/32 dev wg0 || true

[Peer]
# vm-1 (cloud edge, public)
PublicKey           = <VM_1_PUBLIC_KEY>
AllowedIPs          = 172.27.15.31/32
Endpoint            = 198.51.100.25:51820
PersistentKeepalive = 25

[Peer]
# ms-1 (LAN)
PublicKey           = <MS_1_PUBLIC_KEY>
AllowedIPs          = 172.27.15.12/32
Endpoint            = 192.168.15.2:51820
PersistentKeepalive = 25

[Peer]
# wk-2 (LAN)
PublicKey           = <WK_2_PUBLIC_KEY>
AllowedIPs          = 172.27.15.13/32
Endpoint            = 192.168.15.4:51820
PersistentKeepalive = 25

[Peer]
# laptop / admin
PublicKey  = <ADMIN_PUBLIC_KEY>
AllowedIPs = 172.27.15.50/32
```

### `wk-2` — `/etc/wireguard/wg0.conf`

```ini
[Interface]
Address    = 172.27.15.13/32
ListenPort = 51820
PrivateKey = <WK_2_PRIVATE_KEY>
MTU        = 1420
SaveConfig = false

[Peer]
PublicKey = <VM_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.31/32
Endpoint = 198.51.100.25:51820
PersistentKeepalive = 25

[Peer]
PublicKey = <MS_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.12/32
Endpoint = 192.168.15.2:51820
PersistentKeepalive = 25

[Peer]
PublicKey = <WK_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.11/32
Endpoint = 192.168.15.3:51820
PersistentKeepalive = 25
```

### `vm-1` — `/etc/wireguard/wg0.conf`

```ini
[Interface]
Address    = 172.27.15.31/32
ListenPort = 51820
PrivateKey = <VM_1_PRIVATE_KEY>
MTU        = 1420
SaveConfig = false

[Peer]
# wk-1, reached via router forward 51820
PublicKey  = <WK_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.11/32
Endpoint   = 203.0.113.10:51820

[Peer]
# ms-1, reached via router forward 51821
PublicKey  = <MS_1_PUBLIC_KEY>
AllowedIPs = 172.27.15.12/32
Endpoint   = 203.0.113.10:51821

[Peer]
# wk-2, reached via router forward 51822
PublicKey  = <WK_2_PUBLIC_KEY>
AllowedIPs = 172.27.15.13/32
Endpoint   = 203.0.113.10:51822
```

The `vm-1` config has no `PersistentKeepalive` — that field tells the home node to send a packet every 25 seconds to keep its NAT entry on the home router alive. The cloud node has a static public IP and doesn't sit behind NAT, so the home nodes initiate; `vm-1` never needs to.

## Bring it up

On each node, as root:

```bash
chmod 600 /etc/wireguard/wg0.conf
systemctl enable --now wg-quick@wg0
systemctl status wg-quick@wg0    # should be active (running)
ip addr show wg0                  # should show the right 172.27.15.x address
```

`wg-quick` is the friendly wrapper. The "quick" is doing some real work: it adds the WireGuard interface, sets the IP, applies routes from the `AllowedIPs` lines, runs `PostUp`, and sets up firewall rules.

## Verify

The verification ladder, in order:

```bash
# 1. Local interface is up and has the right address
ip addr show wg0
# wg0: inet 172.27.15.12/32 scope global wg0

# 2. The kernel knows about the peers and recent handshakes
wg show
# peer: <vm-1 pubkey>
#   endpoint: 198.51.100.25:51820
#   allowed ips: 172.27.15.31/32
#   latest handshake: 12 seconds ago
#   transfer: 12.3 KiB received, 8.5 KiB sent

# 3. Ping over the tunnel
ping -c3 172.27.15.31    # vm-1 from any home node
ping -c3 172.27.15.11    # wk-1
ping -c3 172.27.15.12    # ms-1
ping -c3 172.27.15.13    # wk-2

# 4. From the laptop (admin peer), ping ms-1
ping -c3 172.27.15.12
```

When all four steps work on every node, the mesh is up and the rest of the book builds on it. If any step fails, the next chapter ([When the mesh misbehaves](/cortex/homelab-from-scratch/private-mesh-when-the-mesh-misbehaves)) is the playbook for finding out why.

→ Next: [When the mesh misbehaves](/cortex/homelab-from-scratch/private-mesh-when-the-mesh-misbehaves)
