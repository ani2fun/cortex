---
title: '12. Security context'
summary: By default a container can run as root with broad Linux privileges — a powerful foothold for an attacker who breaks in. A security context hardens it: non-root, dropped capabilities, no privilege escalation, locked-down syscalls.
---

# 12. Security context

## TL;DR

> A container is an isolated process, but isolation isn't invincible — and by *default* a container often runs as **root** with broad Linux **capabilities**, so an attacker who exploits your app starts with a dangerously powerful foothold. A **security context** hardens the container by *removing* privileges it doesn't need: **run as non-root**, **drop ALL capabilities**, forbid **privilege escalation**, restrict syscalls with **seccomp**, and (ideally) a **read-only root filesystem**. This is *defense in depth* — the assumption that your app *will* be breached someday, and the goal of making that breach as useless as possible. Cortex sets `allowPrivilegeEscalation: false`, drops all capabilities, and uses the `RuntimeDefault` seccomp profile.

## 1. Motivation

Part 3 was about keeping attackers *out* (authentication, authorization, hardening). This chapter is about the complementary, humbler assumption: *someday, something will get in* — a vulnerability in your app, a dependency, the JVM — and the question becomes "how much damage can they do from inside the container?" That's **defense in depth**: don't rely on a single perfect wall; layer defenses so that breaching one still leaves the attacker constrained. A compromised app inside a *locked-down* container is a frustrated attacker in a bare cell; a compromised app inside a *default* container is an attacker handed root keys to the building.

The defaults are the problem. Historically, containers often run their process as **root** (uid 0), and the container runtime grants a set of Linux **capabilities** (fine-grained pieces of root's power — changing file ownership, binding low ports, etc.). Your ZIO server doesn't *need* any of that — it just listens on a port and serves requests — but if it's compromised, the attacker *inherits* whatever privileges the container has. Running as root inside a container is especially dangerous because a container escape (a kernel bug that breaks isolation) from a root process can mean root *on the host*. The security context lets you *strip away* every privilege the app doesn't use, so that even a full app compromise yields an attacker who can't write files, can't gain new privileges, can't make dangerous syscalls, and isn't root. The breach still matters — but it's contained. This is the cheapest, highest-leverage security work in Kubernetes, and it's pure subtraction: remove powers nobody needs.

## 2. Intuition (Analogy)

The **principle of least privilege at a workplace.** A new contractor doesn't get a master key to every room, admin on every system, and the keys to the safe "just in case." They get *exactly* the access their job requires and nothing more. Why? Because if that contractor's badge is *stolen* (or they turn out to be malicious), the thief inherits only the contractor's narrow access — they can't waltz into the server room or empty the safe. The *less* each identity can do, the *less* a compromise of that identity is worth.

A security context applies least privilege to a *container*. Your app needs to listen on a port and read its files — so give it *only* that. Strip root, strip the Linux capabilities it never uses, forbid it from *gaining* new privileges, lock down which syscalls it can make. Now if the app is compromised, the attacker inherits a near-powerless identity — a stolen badge that opens almost nothing. You're not trusting the app to be unbreakable; you're ensuring that breaking it is barely worth the effort.

## 3. Formal Definition

- **Security context.** A set of security settings applied to a Pod (`spec.securityContext`) or a container (`spec.containers[].securityContext`), constraining its privileges. Key settings:
  - **`runAsNonRoot: true` / `runAsUser: <non-zero uid>`** — refuse to run as root; run as an unprivileged user. (The image must support a non-root user.)
  - **`allowPrivilegeEscalation: false`** — the process can't gain *more* privileges than it started with (blocks setuid-style escalation).
  - **`capabilities: { drop: [ALL] }`** — remove *all* Linux capabilities (then `add` back only any you truly need — usually none). Capabilities are slices of root's power; dropping all means the app has none of them.
  - **`readOnlyRootFilesystem: true`** — the container's filesystem is read-only (the app writes only to explicitly-mounted writable volumes), so an attacker can't modify binaries or drop tools.
  - **`seccompProfile: { type: RuntimeDefault }`** — apply a **seccomp** filter restricting which *system calls* the process may make to a safe default set, shrinking the kernel attack surface.
  - **`privileged: false`** (default; never `true` for app workloads) — a privileged container has near-host-level access; reserve only for special infra.
- **Defense in depth.** Layering independent defenses so no single failure is catastrophic. The security context is the *innermost* layer — it assumes the app is breached and limits the blast radius.
- **Pod Security Standards / admission.** Clusters can *enforce* these (e.g. the "restricted" profile), rejecting Pods that don't drop privileges — so hardening isn't just per-app discipline but a cluster policy.

> A security context strips a container down to least privilege: non-root, no extra capabilities, no privilege escalation, restricted syscalls, read-only filesystem. It assumes a breach and minimizes what an attacker inherits — defense in depth's innermost layer.

## 4. Worked Example — Cortex's hardening

Cortex's [`deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml) applies a security context at both the container and Pod level:

```yaml
# Container-level: strip the app's own privileges.
securityContext:
  allowPrivilegeEscalation: false       # can't gain MORE privileges than it starts with
  capabilities:
    drop:
      - ALL                             # remove EVERY Linux capability — the app needs none

# Pod-level: restrict the syscalls the process may make.
securityContext:
  seccompProfile:
    type: RuntimeDefault                # the container runtime's safe default syscall filter
```

Map each line to the threat it removes. `allowPrivilegeEscalation: false` means a compromised Cortex process *cannot* escalate to more power than it began with — closing the setuid-style escalation path. `capabilities: drop: [ALL]` strips every Linux capability: Cortex's server binds port 8080 (a high port, no special capability needed) and reads its content files — it needs *none* of root's powers, so it's given *none*; an attacker who owns the process can't, say, change file ownership or manipulate the network stack at a low level. `seccompProfile: RuntimeDefault` filters the *system calls* the process can make to a vetted safe set, so even kernel-level exploitation has a far smaller surface. 

Together these embody the chapter's thesis: *assume Cortex will be breached, and make the breach worthless.* An attacker who fully compromises the ZIO server finds themselves as a non-privileged-capability process that can't escalate, can't make dangerous syscalls, and (with `readOnlyRootFilesystem`, a natural next step) couldn't even write a tool to disk. This complements Part 3 perfectly: Part 3 made it *hard to break in*; the security context makes *breaking in nearly useless*. And because the cluster can *enforce* a restricted Pod Security Standard, this hardening becomes a *policy* every workload must meet, not a thing one team remembers — least privilege as a cluster-wide default.

## 5. Build It

Run this. It models a container compromise and shows how dropping privileges shrinks what the attacker can do.

```python run
# What an attacker can do INSIDE a compromised container depends on its privileges.
ALL_ACTIONS = {
    "read_app_files":      "needed by the app (allowed)",
    "change_file_owner":   "requires CAP_CHOWN capability",
    "bind_low_port_<1024": "requires CAP_NET_BIND_SERVICE",
    "escalate_to_root":    "requires privilege escalation",
    "write_binaries":      "requires a writable root filesystem",
    "dangerous_syscalls":  "requires syscalls seccomp would block",
}

def attacker_can_do(security_context):
    can = []
    for action in ALL_ACTIONS:
        if action == "read_app_files":
            can.append(action)                                       # always (the app needs it)
        elif action == "change_file_owner" and "CHOWN" in security_context["capabilities"]:
            can.append(action)
        elif action == "bind_low_port_<1024" and "NET_BIND_SERVICE" in security_context["capabilities"]:
            can.append(action)
        elif action == "escalate_to_root" and security_context["allowPrivilegeEscalation"]:
            can.append(action)
        elif action == "write_binaries" and not security_context["readOnlyRootFilesystem"]:
            can.append(action)
        elif action == "dangerous_syscalls" and security_context["seccomp"] != "RuntimeDefault":
            can.append(action)
    return can

default_ctx = {"capabilities": {"CHOWN", "NET_BIND_SERVICE", "SETUID", "..."},
               "allowPrivilegeEscalation": True, "readOnlyRootFilesystem": False, "seccomp": "Unconfined"}
hardened_ctx = {"capabilities": set(),  # drop ALL
                "allowPrivilegeEscalation": False, "readOnlyRootFilesystem": True, "seccomp": "RuntimeDefault"}

print("DEFAULT container — attacker who compromises the app can:")
for a in attacker_can_do(default_ctx): print("   -", a)
print("\nHARDENED container (Cortex-style) — attacker can only:")
for a in attacker_can_do(hardened_ctx): print("   -", a)
print("\nSame breach, vastly smaller blast radius.")
```

**Now break it.** Flip the hardened context's `allowPrivilegeEscalation` back to `True` or add a capability back — each toggle hands the attacker another power. That's the lesson in reverse: every privilege you *grant* is a privilege a compromise *inherits*, so the secure default is to grant *nothing* and add back only what the app provably needs (for most web apps: nothing). Note that the *app still works identically* in the hardened container — it never needed those privileges. Hardening here is pure subtraction with no functional cost, which is exactly why it's the highest-leverage security work you can do: free defense, just by removing powers nobody uses.

## 6. Trade-offs & Complexity

| Hardened security context | Default (root, full caps) |
|---|---|
| Breach yields a near-powerless attacker | Breach yields broad privileges |
| Least privilege, defense in depth | Maximum blast radius |
| Often zero functional cost (apps don't need root) | "Works" but dangerous |
| Image must support non-root | Lazy but exploitable |
| Cluster can enforce as policy | Each team must remember |

The cost is mostly *getting the image right* — your container image must be *able* to run as a non-root user (some images assume root; you may need to adjust the Dockerfile to create and use a non-root user, and ensure writable paths are mounted volumes). And occasionally an app genuinely needs *one* capability (e.g. binding a port below 1024) — then you `drop: [ALL]` and `add` back exactly that one. But for the overwhelming majority of web apps, the hardened context has *no functional downside*: the app never used root or those capabilities, so removing them changes nothing except the attacker's prospects. That makes this the rare security measure that's almost free — the main barrier is awareness and the discipline to set it (or a cluster policy that enforces it).

## 7. Edge Cases & Failure Modes

- **Image that requires root.** If the image's process insists on root (writes to system paths, assumes uid 0), `runAsNonRoot` will fail to start it. Fix the *image* (non-root user, writable volumes) rather than abandoning hardening.
- **Dropping a capability the app needs.** Rare for web apps, but e.g. binding port 80 directly needs `NET_BIND_SERVICE`. Drop ALL, then add back the *one* needed capability — never leave them all.
- **Read-only filesystem breaking writes.** `readOnlyRootFilesystem: true` blocks the app from writing to disk; if it needs scratch space (temp files, caches), mount a writable `emptyDir` volume at that path. Don't disable the protection wholesale.
- **Hardening one Pod but not enforcing it.** A single hardened Deployment doesn't help if other workloads run privileged. Use Pod Security Standards (the "restricted" profile) to enforce least privilege cluster-wide.

## 8. Practice

> **Exercise 1 — Why drop ALL?** Explain why Cortex drops *all* Linux capabilities, given that its server only listens on port 8080 and reads files. What does the app lose? What does an attacker lose?

<details>
<summary><strong>Answer</strong></summary>

Capabilities are slices of root's power (changing file ownership, binding low ports, manipulating the network stack, etc.). The reasoning is *least privilege* (§2): grant only what the app provably needs, and add back nothing else — because every privilege you grant is one a compromise *inherits*.

- **What the app loses: nothing.** Cortex's server binds **port 8080** — a *high* port (≥1024), which needs *no* special capability — and *reads* its content files, which needs no capability either. It never used `CHOWN`, `NET_BIND_SERVICE`, `SETUID`, or any of them. So `drop: [ALL]` removes only powers the app already wasn't using; it runs *identically*. Hardening here is pure subtraction with zero functional cost.
- **What an attacker loses: everything those capabilities grant.** An attacker who fully compromises the ZIO process inherits the *container's* privileges — and now that's *none of them*. They can't change file ownership, can't bind privileged ports, can't manipulate the network at a low level. Same breach, vastly smaller blast radius.

That asymmetry — *no cost to you, big cost to the attacker* — is why this is the highest-leverage security work in Kubernetes: you give up nothing and take away the attacker's toolkit. (Were the app to *need* one capability, e.g. binding port 80 directly, you'd still `drop: [ALL]` and `add` back *only* that one.)

</details>

> **Exercise 2 — Defense in depth.** Connect this chapter to Part 3: Part 3 keeps attackers out; this chapter assumes one gets in. Describe the layered defense and why you want both.

<details>
<summary><strong>Answer</strong></summary>

The two parts guard *different assumptions*, and **defense in depth** (§3) is the principle of layering independent defenses so no single failure is catastrophic:

- **Part 3 — keep them out (the outer wall).** Authentication (who are you?), authorization (what may you do?), OAuth/OIDC token verification — all aimed at *preventing* an attacker from getting in at all.
- **This chapter — assume one gets in (the inner cell).** The security context makes the humbler bet: *someday something will be breached* — a bug in your app, a dependency, the JVM. So it *strips the container to least privilege* (non-root, drop ALL capabilities, no privilege escalation, seccomp, read-only filesystem) so that a *successful* breach yields a near-powerless attacker.

**Why both:** no single wall is perfect — assume any one layer *can* fail. If you only keep attackers out and they get in anyway (root, full capabilities), they own the building. If you only harden the container but make it trivial to break in, you're relying on the inner cell alone. Together: Part 3 makes breaking in *hard*; the security context makes breaking in *nearly useless*. The security context is defense-in-depth's *innermost* layer — it doesn't trust the app to be unbreakable, it ensures breaking it is barely worth the effort. (And a cluster can *enforce* this via Pod Security Standards, so it's policy, not per-team memory.)

</details>

> **Exercise 3 — Make an image non-root.** A container fails with `runAsNonRoot: true` because the image runs as root and writes to `/app/tmp`. Outline the Dockerfile/manifest changes to fix it without disabling hardening.

<details>
<summary><strong>Answer</strong></summary>

The fix is to make the *image* able to run as non-root and to give it a *writable place to write* — never to weaken the hardening (§7: "Fix the image, not the policy"). Two problems, two fixes:

**1. Make the image run as a non-root user (Dockerfile).** Create an unprivileged user, give it ownership of what it needs, and switch to it:

```text
RUN addgroup -S app && adduser -S app -G app   # create a non-root user
RUN chown -R app:app /app                       # let it own its app dir
USER app                                         # process now runs as non-root (uid != 0)
```

Now `runAsNonRoot: true` (and a `runAsUser: <non-zero uid>`) is satisfied — the process no longer starts as root.

**2. Give it a writable path for `/app/tmp` (manifest).** If you also want `readOnlyRootFilesystem: true`, the root FS is read-only, so mount a writable volume *at exactly that path* rather than disabling the protection:

```yaml
volumeMounts:
  - name: tmp
    mountPath: /app/tmp        # writable scratch space, even with a read-only root FS
volumes:
  - name: tmp
    emptyDir: {}               # ephemeral, per-Pod writable volume
```

The principle (§7): when hardening "breaks" the app, the failure is telling you the *image* assumes privileges it shouldn't — fix the image (non-root user) and *scope* writability to a mounted volume. You keep `runAsNonRoot`, `drop: [ALL]`, and `readOnlyRootFilesystem` *all on*; you just adapt the app to live within them. Disabling the protection "to make it work" throws away the cheapest security you have.

</details>

```quiz
{
  "prompt": "What is the purpose of a Kubernetes security context (non-root, drop ALL capabilities, no privilege escalation, seccomp)?",
  "input": "Choose one:",
  "options": [
    "Defense in depth: assume the app will eventually be breached, and strip the container to least privilege so an attacker who compromises it inherits almost no power",
    "To make the container start faster",
    "To encrypt the container's network traffic",
    "To give the container root access for convenience"
  ],
  "answer": "Defense in depth: assume the app will eventually be breached, and strip the container to least privilege so an attacker who compromises it inherits almost no power"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Kubernetes — Configure a Security Context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/)** — `runAsNonRoot`, capabilities, seccomp, read-only filesystem.
- **[Kubernetes — Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)** — the baseline/restricted profiles a cluster can enforce.
- **[Cortex `deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)** — the real `allowPrivilegeEscalation: false`, dropped capabilities, and `RuntimeDefault` seccomp.

---

**Next:** we've met every piece. Now follow a single change all the way through — from a `git push` on your laptop to a new Pod serving `cortex.kakde.eu`. → [13. The full path: git push → pod](/cortex/production-engineering/docker-kubernetes/cortex-in-production/the-full-path)
