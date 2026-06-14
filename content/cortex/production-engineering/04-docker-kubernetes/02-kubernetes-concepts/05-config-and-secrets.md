---
title: '9. Config and Secrets'
summary: A container image is immutable and identical everywhere, so per-environment settings — the database URL, the Keycloak issuer, the DB password — must come from outside. Kubernetes injects them via env vars, ConfigMaps, and Secrets.
---

# 9. Config and Secrets

## TL;DR

> The *same* immutable image runs in dev, staging, and prod (Chapter 1) — so anything that *differs* between them must be injected from *outside* the image at runtime. Kubernetes injects config three ways: **env vars** set directly in the Deployment, **ConfigMaps** (reusable non-secret config), and **Secrets** (sensitive values like passwords, referenced via `secretKeyRef` and ideally encrypted at rest). Cortex's Deployment sets `DB_URL` as a plain env var but pulls `DB_PASSWORD` from a `cortex-db` **Secret** — never hard-coding the password into the manifest. This is exactly the env-override mechanism Part 1's ZIO Config reads.

## 1. Motivation

Two principles we've established now collide productively. First (Chapter 1): the image is **immutable and identical everywhere** — you ship the same bytes to every environment. Second (Part 1, Chapter 14): the app needs **different values per environment** — a localhost database on your laptop, a cluster database in production; `AUTH_ENABLED=false` in dev, `true` in prod. If config were baked *into* the image, you'd need a different image per environment, destroying the "build once, run everywhere" guarantee. So config must live *outside* the image and be injected when the container starts.

Kubernetes provides the injection mechanisms, and critically distinguishes **ordinary config** from **secrets**. The database *URL* is not sensitive — fine to put in plain text in a manifest committed to git. The database *password* is sensitive — it must *not* sit in git in plain text, where anyone with repo access (or a leaked clone) reads it. Kubernetes' **Secret** object exists for exactly this separation: sensitive values are stored apart, referenced indirectly, access-controlled, and (properly configured) encrypted at rest. Getting this right is both a *correctness* concern (the same image must adapt per environment) and a *security* one (secrets must not leak). Cortex's Deployment is a clean example of both.

## 2. Intuition (Analogy)

A **rental car and its key versus the glovebox manual.** The car (the image) is identical to thousands of others off the line. What makes *your* rental usable is configured from outside: the *key* unique to you (a secret — guard it, don't tape it to the windshield), and the *settings* like mirror and seat position (ordinary config — adjust freely, no harm if seen). You'd never manufacture a *different car* for each renter; you ship identical cars and *configure* each at handover. And you'd never leave the key in the glovebox of a car parked on the street.

Kubernetes config is that handover. The image is the identical car. **ConfigMaps/env vars** are the seat-and-mirror settings — environment-specific but not sensitive, fine to keep in plain manifests. **Secrets** are the keys — sensitive, kept separately, handed over carefully, never left lying in the (git) glovebox in plain text. Same car everywhere; different (and differently-protected) configuration per renter.

## 3. Formal Definition

- **Why external config.** The image is immutable; per-environment values are injected at container start, so one image serves all environments. (The app reads them — Cortex via ZIO Config, Part 1 Chapter 14.)
- **Env vars (`env:`).** Set directly in the Deployment's container spec: `name`/`value` pairs. Simplest; good for non-sensitive, deployment-specific values.
- **ConfigMap.** A named object holding non-secret key/value config, *decoupled* from any one Deployment so it's reusable and editable without changing the Deployment. Injected as env vars (`valueFrom.configMapKeyRef`) or mounted as files.
- **Secret.** Like a ConfigMap but for *sensitive* data. Stored base64-encoded (note: base64 is **encoding, not encryption** — it's not secret by itself), referenced via `valueFrom.secretKeyRef`. Real protection comes from: **encryption at rest** (etcd encryption), **RBAC** (who can read Secrets), and not committing raw Secrets to git. Production setups use **Sealed Secrets** or an external manager (Vault, cloud secret stores) so git only holds *encrypted* secrets.
- **`valueFrom.secretKeyRef`** — inject one key from a Secret as an env var, *without* the value ever appearing in the Deployment manifest. The manifest references the Secret by name; the value lives only in the Secret object.
- **Mutability.** ConfigMaps/Secrets can change without rebuilding the image — but a running Pod usually needs a restart (or a rollout) to pick up new env-var values.

> The immutable image is configured from outside at runtime: env vars / ConfigMaps for ordinary config, Secrets for sensitive values (referenced via `secretKeyRef`, encrypted at rest, never in git as plaintext). One image, environment-specific and securely-injected configuration.

## 4. Worked Example — Cortex's Deployment config

Cortex's Deployment injects everything its app needs via `env`, mixing plain values and a secret reference (from the real [`deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)):

```yaml
env:
  - name: PORT
    value: "8080"
  - name: DB_URL                              # NOT sensitive -> plain value, fine in git
    value: jdbc:postgresql://postgresql.databases-prod.svc.cluster.local:5432/cortex
  - name: DB_USER
    value: cortex
  - name: DB_PASSWORD                         # SENSITIVE -> pulled from a Secret, NOT in the manifest
    valueFrom:
      secretKeyRef:
        name: cortex-db                       # the Secret object's name
        key: postgres-password                # the key within it
  - name: REDIS_URL
    value: redis://cortex-redis:6379
  - name: AUTH_ENABLED
    value: "true"                             # prod: auth ON (dev default was false — Part 3, Ch 19)
  - name: KEYCLOAK_ISSUER_URL
    value: https://keycloak.kakde.eu/realms/apps-prod   # prod realm (Part 3)
  - name: KEYCLOAK_CLIENT_ID
    value: cortex-web
```

Read the security boundary. The `DB_URL` points at the cluster's Postgres (`postgresql.databases-prod.svc.cluster.local` — a Service name, Chapter 7!) — not sensitive, so it sits as a plain `value` in a manifest that can live in git. But `DB_PASSWORD` is *not* in the manifest at all: `valueFrom.secretKeyRef` says "inject the `postgres-password` key from the `cortex-db` Secret." The actual password lives only in that Secret object — kept out of git (via Sealed Secrets in Cortex's GitOps setup, see [Homelab from Scratch → Sealed Secrets](/cortex/homelab-from-scratch/secrets-and-gitops/sealed-secrets)), access-controlled by RBAC, encrypted at rest. Anyone reading the Deployment manifest sees *that* a DB password is needed and *where it comes from*, but never the password itself.

And notice these are *exactly* the env vars from Part 1, Chapter 14: `DB_URL`, `AUTH_ENABLED`, `KEYCLOAK_ISSUER_URL`. The `application.conf` had localhost defaults with `${?DB_URL}` overrides; here Kubernetes *supplies those overrides* with production values. Same immutable image (Chapter 3), same ZIO Config code reading the env — only the injected values differ. The whole "config selects behavior, environment supplies config" arc, completed: the app declares what it reads (Part 1), and Kubernetes injects it per environment, with secrets handled separately (this chapter).

## 5. Build It

Run this. It models env/ConfigMap/Secret injection into an immutable image and shows why the password must come from a Secret reference, not the manifest.

```python run
IMAGE = {"name": "cortex:latest"}            # immutable — identical in every environment

# A Secret object (stored separately, access-controlled, NOT in the git manifest).
secrets = {"cortex-db": {"postgres-password": "S3cr3t-prod-pw"}}

def deploy(image, env_plain, secret_refs):
    # Build the container's runtime environment by injecting plain values + secret references.
    env = dict(env_plain)
    for var, ref in secret_refs.items():
        env[var] = secrets[ref["name"]][ref["key"]]      # resolved at runtime, inside the cluster
    return {"image": image["name"], "env": env}

# The Deployment manifest (committed to git) holds plain config + REFERENCES, never the password.
manifest_env_plain = {
    "DB_URL": "jdbc:postgresql://postgresql.databases-prod:5432/cortex",
    "AUTH_ENABLED": "true",
}
manifest_secret_refs = {
    "DB_PASSWORD": {"name": "cortex-db", "key": "postgres-password"},   # a reference, not the value
}

pod = deploy(IMAGE, manifest_env_plain, manifest_secret_refs)
print("the git manifest contains:", manifest_env_plain, "+ secret REFERENCES", list(manifest_secret_refs))
print("the running pod's env (resolved):", pod["env"])
print("\nthe password appears in the running pod, but NEVER in the manifest you commit.")
```

**Now break it.** Put the password directly in `manifest_env_plain` (`"DB_PASSWORD": "S3cr3t-prod-pw"`) "for simplicity." It works — but now the secret is in the manifest, which means it's in git, which means it's in every clone, every fork, every CI log, forever (and Chapter 2's lesson: git history is permanent). The `secretKeyRef` indirection exists precisely so the *manifest* (public-ish) only ever holds a *pointer* to the secret, while the *value* lives in a protected Secret object. Mixing them up is one of the most common — and most damaging — Kubernetes security mistakes. Config that's safe to read goes in the manifest; anything that isn't goes through a Secret reference.

## 6. Trade-offs & Complexity

| External config + Secrets | Baked-in / hard-coded config |
|---|---|
| One image, all environments | A different image per env (or worse) |
| Secrets kept out of git | Passwords leak into the repo |
| Change config without rebuilding | Rebuild to change a setting |
| ConfigMaps/Secrets reusable across apps | Duplicated everywhere |
| More objects + the secrets problem | "Simpler" but insecure and rigid |

The cost has two parts. *Operational:* more objects to manage (ConfigMaps, Secrets) and the discipline to route the right value to the right one. *The hard part — secrets:* base64 is *not* encryption, so a naive Secret in git is barely better than plaintext; doing secrets *properly* (Sealed Secrets, encryption at rest, external managers, RBAC) is genuinely involved and easy to get subtly wrong. But the alternative — passwords in images or manifests — is a breach waiting to happen. The investment is mandatory for anything handling real credentials, and Kubernetes' separation of ConfigMap (open) from Secret (protected) at least gives you the right *structure* to build on.

## 7. Edge Cases & Failure Modes

- **Secrets in git as plaintext.** Base64 is encoding, not encryption — a raw Secret YAML in git exposes the value. Use Sealed Secrets / an external manager so git holds only *encrypted* secrets.
- **Secret in the wrong object.** Putting a password in a ConfigMap (or a plain env `value`) leaks it. Sensitive → Secret + `secretKeyRef`, always.
- **Config changes not picked up.** Updating a ConfigMap/Secret doesn't automatically restart Pods consuming it via env vars — you need a rollout (Chapter 14) for the new values to take effect. (Mounted-file config can update live, with caveats.)
- **Over-broad Secret access.** Any workload that can *read* a Secret can see its value. Lock down with RBAC so only the Pods that need a secret can read it.

## 8. Practice

> **Exercise 1 — Plain or secret?** For each, choose env-value/ConfigMap or Secret: (a) the database URL; (b) the database password; (c) `AUTH_ENABLED`; (d) a third-party API key; (e) the Keycloak issuer URL.

<details>
<summary><strong>Answer</strong></summary>

The single test from §1: *is this value sensitive — would leaking it into git harm you?* If no, it's fine as a plain env `value` or ConfigMap; if yes, it goes in a Secret via `secretKeyRef`.

- **(a) database URL — plain (env-value/ConfigMap).** A hostname like `postgresql.databases-prod.svc.cluster.local` is not a credential; it's safe in a git-committed manifest (exactly what Cortex does).
- **(b) database password — Secret.** A credential. It must never sit in git as plaintext; keep it in a `cortex-db` Secret, referenced (never written) in the manifest.
- **(c) `AUTH_ENABLED` — plain (env-value/ConfigMap).** A boolean feature flag, not a secret. `"true"` in the manifest is fine.
- **(d) third-party API key — Secret.** A credential — anyone holding it can act as you against that service. Secret, always.
- **(e) Keycloak issuer URL — plain (env-value/ConfigMap).** A public endpoint (`https://keycloak.kakde.eu/realms/apps-prod`), not a secret. Plain value.

The rule of thumb: *URLs, flags, and identifiers are config; passwords, keys, and tokens are secrets.* Base64 in a Secret is encoding, not encryption — the real protection is keeping it out of git (Sealed Secrets), RBAC, and encryption at rest.

</details>

> **Exercise 2 — Trace the reference.** Explain what `valueFrom.secretKeyRef: {name: cortex-db, key: postgres-password}` does, and why the password never appears in the Deployment manifest.

<details>
<summary><strong>Answer</strong></summary>

`valueFrom.secretKeyRef` is a *pointer*, not a value. It tells Kubernetes: "when this container starts, look up the Secret object named `cortex-db`, read its `postgres-password` key, and inject that as the `DB_PASSWORD` env var." The lookup happens at runtime, inside the cluster — the value is resolved into the *running Pod's* environment, never into the YAML.

That indirection is the whole point. The manifest holds only the *reference* — the Secret's name and a key — so the manifest can safely live in git (every clone, fork, and CI log) while the actual password lives only in the protected `cortex-db` Secret object: access-controlled by RBAC, encrypted at rest, and (in Cortex's GitOps setup) committed to git only in *encrypted* Sealed-Secret form. Anyone reading the Deployment learns *that* a DB password is needed and *where it comes from*, but never the password itself. Hard-code it as a plain `value` instead and it's in git forever (§2's "git history is permanent") — which is the indirection's reason to exist.

</details>

> **Exercise 3 — Tie it together.** Connect this chapter to Part 1, Chapter 14: how does the same image read `DB_URL` differently in dev vs prod, and who supplies the value in each?

<details>
<summary><strong>Answer</strong></summary>

The image is *immutable and identical everywhere* (Chapter 3) — the same bytes run in dev and prod — so the only thing that can differ is the value *injected from outside* at container start. The app's ZIO Config (Part 1, Ch 14) reads `DB_URL` from the environment with a localhost default and a `${?DB_URL}` override:

- **In dev:** no override is set, so the config falls back to its baked-in default (`localhost`). *You* supply it implicitly by *not* overriding — the laptop database.
- **In prod:** *Kubernetes* supplies the override. The Deployment's `env` sets `DB_URL` to the cluster Postgres (`jdbc:postgresql://postgresql.databases-prod...`), and that env var wins over the default.

So the *code* that reads `DB_URL` is identical in both — same image, same ZIO Config logic. Only the *injected environment* changes: nobody in dev, Kubernetes in prod. That completes the arc: the app *declares what it reads* (Part 1), and the environment *supplies the value per deployment* (this chapter) — "build once, run everywhere" preserved, because config lives outside the image.

</details>

```quiz
{
  "prompt": "Why does Cortex put `DB_URL` as a plain env value in its Deployment but pull `DB_PASSWORD` from a Secret via `secretKeyRef`?",
  "input": "Choose one:",
  "options": [
    "The URL isn't sensitive (fine in a git-committed manifest), but the password is — so it's kept in a separate, access-controlled Secret and only referenced, never written, in the manifest",
    "secretKeyRef makes the database faster",
    "Passwords can't be stored as strings",
    "The URL is encrypted and the password isn't"
  ],
  "answer": "The URL isn't sensitive (fine in a git-committed manifest), but the password is — so it's kept in a separate, access-controlled Secret and only referenced, never written, in the manifest"
}
```

## In the Wild

- **[Kubernetes — ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)** & **[Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)** — injecting config and the (important) caveats about Secret security.
- **[Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets)** — encrypting Secrets so they can safely live in git (Cortex's GitOps approach).
- **[Cortex `deployment.yaml`](https://github.com/ani2fun/cortex/blob/main/../infra/deploy/apps/cortex/base/deployment.yaml)** + **[Homelab from Scratch — Sealed Secrets](/cortex/homelab-from-scratch/secrets-and-gitops/sealed-secrets)** — the real env/secret config and how secrets stay out of git.

---

**Next:** Kubernetes restarts dead Pods and routes traffic only to ready ones — but how does it *know* a container is alive or ready? Through probes. → [10. Probes: liveness, readiness, startup](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/probes)
