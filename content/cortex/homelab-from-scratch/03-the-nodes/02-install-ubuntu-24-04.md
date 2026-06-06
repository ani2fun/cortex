---
title: Install Ubuntu 24.04
summary: Install Ubuntu 24.04 LTS on every node — fresh ISO on the home boxes, cloud-init image on the edge VM. SSH key bootstrap, an unprivileged admin user, and a verified first login from your laptop.
---

## Why Ubuntu 24.04 specifically

K3s, Calico, and WireGuard all run on most Linux distributions. Ubuntu 24.04 is the right choice for this book because:

1. It's an **LTS** release with five years of security patches and a stable kernel for the lifetime of this homelab.
2. The **package versions** (WireGuard 1.0.20210914, nftables 1.0.9, kernel 6.8) are recent enough that everything in the rest of the book Just Works.
3. Every cloud provider ships a **certified Ubuntu 24.04 image** with cloud-init pre-configured.
4. The LXC + Snap idiosyncrasies are mostly out of the way by 24.04.

If you're a Debian / Fedora / Arch person, this all maps over with minor file-path changes. The package names are the same.

## The two install paths

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
  participant Laptop as Your laptop
  participant USB as USB stick
  participant Box as Home box
  participant Cloud as Cloud provider
  participant Edge as vm-1

  Note over Laptop,Box: Path A — home boxes
  Laptop->>USB: Flash Ubuntu 24.04 server ISO
  USB->>Box: Boot, install
  Box-->>Box: Reboot to disk
  Laptop->>Box: ssh root@<lan-ip>

  Note over Laptop,Edge: Path B — cloud edge
  Laptop->>Cloud: Provision VPS w/ cloud-init userdata
  Cloud->>Edge: First boot applies userdata
  Laptop->>Edge: ssh root@<public-ip>
```

Two paths because home boxes need a real installer (you have physical access; a USB stick is fastest) while cloud machines come with a pre-baked image (you don't have physical access; you submit cloud-init config and the provider boots it).

## Path A: the home boxes

For `ms-1`, `wk-1`, `wk-2`:

1. **Download the ISO.** [Ubuntu Server 24.04.4 LTS amd64](https://ubuntu.com/download/server) — or arm64 if you went with Pis. Get the *server* image, not desktop.
2. **Flash a USB stick.** macOS: `brew install balenaetcher` and use the GUI. Linux: `dd if=ubuntu-24.04.4-live-server-amd64.iso of=/dev/sdX bs=4M status=progress` (replace `/dev/sdX` with the actual stick — check `lsblk` first; getting the wrong device wipes it).
3. **Boot the box from the stick.** F12, F8, F2, or Esc at power-on, depending on the BIOS — pick *Boot from USB*.
4. **In the installer**:
   - **Layout / language** — defaults are fine.
   - **Network** — let it DHCP for now. We'll set static addresses in the next chapter.
   - **Storage** — *Use entire disk*, no LVM, no encryption. This is a homelab; LUKS is optional and adds boot-time pain. ZFS is overkill. Plain ext4 is the right answer.
   - **Profile** — set the hostname to **`ms-1`**, **`wk-1`**, or **`wk-2`** depending on which machine this is. The hostname matters — `prepare-host.sh` later keys behaviour off it. Set the username to whatever you like (we'll mostly use root from here on).
   - **SSH** — install OpenSSH server. **Import SSH keys from GitHub**. Type your GitHub username and the installer pulls your public keys. (If you don't have keys on GitHub yet: `ssh-keygen -t ed25519` on your laptop, paste `~/.ssh/id_ed25519.pub` into your GitHub *Settings → SSH and GPG keys* page.)
   - **Featured server snaps** — skip them all. We don't want random snaps on the cluster nodes.
5. **Install, reboot, remove the USB stick.** Wait for the box to come up.
6. **From your laptop**: `ssh <username>@<lan-ip>` should drop you into a shell with no password prompt. If it asks for a password, the SSH key import didn't work — debug with `ssh -v`.

### Set static IPs now

DHCP is fine for the install, but the WireGuard mesh assumes the home boxes have predictable LAN addresses. Edit `/etc/netplan/00-installer-config.yaml` (or whatever the installer wrote — `ls /etc/netplan/`):

```yaml
network:
  version: 2
  ethernets:
    eno1:                # whatever your interface is — `ip -br link` to check
      dhcp4: false
      addresses:
        - 192.168.15.2/24    # ms-1; .3 for wk-1, .4 for wk-2
      routes:
        - to: default
          via: 192.168.15.1   # your home router
      nameservers:
        addresses: [1.1.1.1, 8.8.8.8]
```

Apply:

```bash
sudo netplan apply
ip -br addr           # confirm the new IP is on the right interface
```

You'll lose the SSH connection. Re-`ssh` to the new static IP.

## Path B: the cloud edge

For `vm-1` on Contabo (or Hetzner / OCI / similar), the provider's UI exposes an *Initialization Script* or *cloud-init userdata* field. Paste this in before first boot:

```yaml
#cloud-config
hostname: ctb-edge-1   # or vm-1, or whatever you used in nodes.yaml
manage_etc_hosts: true
users:
  - name: root
    ssh_authorized_keys:
      - ssh-ed25519 AAAA...your_public_key_here... you@laptop
package_update: true
package_upgrade: true
packages:
  - openssh-server
  - curl
  - jq
ssh_pwauth: false
disable_root: false
```

Three things this does:

- Sets the hostname so `prepare-host.sh` can detect the role.
- Installs your SSH public key for root login.
- Disables password SSH, so the moment cloud-init finishes there's no password attack surface.

Provision the VM, wait ~60 seconds for first boot, then:

```bash
ssh root@<public-ip>
```

You should be in. If not, the provider's web console will show cloud-init's progress in `/var/log/cloud-init-output.log`. Common failures: malformed YAML (cloud-init silently skips a misformatted block), wrong SSH key format (must be a single line, no wrapping).

## Verify before you move on

On every node:

```bash
# 1. Identity
hostnamectl status | grep -E 'Static hostname|Operating System|Kernel'
# Static hostname: ms-1 (or wk-1, wk-2, ctb-edge-1)
# Operating System: Ubuntu 24.04.x LTS
# Kernel: Linux 6.8.x-generic

# 2. SSH from laptop
# (run this from your laptop, not the node)
ssh root@<node-address> 'echo ok'
# → ok

# 3. Internet works from the node
curl -s https://api.cloudflare.com/client/v4/ips | jq .success
# → true
```

If all three succeed on all four nodes, you're done with installation.

## What you should have now

- Four Ubuntu 24.04.x LTS hosts: `ms-1`, `wk-1`, `wk-2`, `ctb-edge-1`
- SSH key login as root from your laptop, no passwords
- Static IPs on the three home boxes (`192.168.15.2`, `.3`, `.4`)
- The cloud edge reachable on its public IP
- A non-trivial `apt update && apt upgrade -y` recently completed on each

Don't run `prepare-host.sh` yet — it'll run in the next chapter, where we explain what each piece of it does.

→ Next: [Baseline host prep](/cortex/homelab-from-scratch/the-nodes-baseline-host-prep)
