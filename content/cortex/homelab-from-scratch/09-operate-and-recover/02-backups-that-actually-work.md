---
title: Backups that actually work
summary: What to back up (Postgres dumps, the Sealed Secrets master key, the Keycloak realm export), where to put it (off-cluster, off-site, encrypted), and the only test that matters — restoring it on a fresh machine and watching the cluster come back.
---

## What "actually works" means

A backup that has never been restored is a *hope*. Backups that work means: the day the cluster catches fire, you have a tarball you've successfully restored on test hardware in the last six months, with a documented procedure that takes under an hour.

Three artefacts cover the whole homelab:

```d2
direction: down

cluster: Live cluster (the source) {
  shape: rectangle
  pg: PostgreSQL\n(databases-prod)
  ss: Sealed Secrets\nmaster key\n(kube-system)
  kc: Keycloak realm\n(in postgres + JSON export)
}

local: Local backup dir\n(operator's laptop) {
  shape: rectangle
  pgtar: postgres-backup-<ts>.tar.gz
  sskey: sealed-secrets-master-key.<ts>.yaml
  kcjson: keycloak-realm-export.<ts>.json
}

offsite: Off-site encrypted storage {
  shape: cylinder
  bn: Backblaze B2 / S3 / etc.\nencrypted
}

pm: Password manager {
  shape: rectangle
  smallsecrets: WireGuard private keys\nadmin passwords\nGitHub PAT\n(for cluster rebuild)
}

cluster.pg -> local.pgtar: pg_dump via kubectl exec
cluster.ss -> local.sskey: kubectl get secret -o yaml
cluster.kc -> local.kcjson: kc.sh export
local.pgtar -> offsite.bn: rclone copy
local.sskey -> offsite.bn: rclone copy
local.kcjson -> offsite.bn: rclone copy
local.sskey -> pm.smallsecrets: as belt-and-braces
```

Three artefacts × two destinations (off-site encrypted storage + a password manager copy of the small ones). Everything else in the cluster — workload manifests, Argo CD config, Ingresses, NetworkPolicies — is in the Git repo, which is itself a backup if your Git host is reasonably paranoid.

## Postgres — the daily backup

The `infra/scripts/dr/postgres-backup.sh` script in the cluster behind these docs does the right thing: it discovers every non-template database, dumps each in custom format, captures role-globals, bundles into a single timestamped tarball.

Running it from the operator laptop:

```bash
# One-off run
scripts/dr/postgres-backup.sh ~/backups/postgres/
# ==> backup complete
# file:    ~/backups/postgres/postgres-backup-20260509T0814Z.tar.gz
# size:    24M
# sha256:  abc...
```

The bones of what it does:

```bash
ssh ms-1 'kubectl -n databases-prod exec postgresql-0 -- \
  pg_dumpall -U postgres --globals-only' > globals.sql

ssh ms-1 'kubectl -n databases-prod exec postgresql-0 -- \
  pg_dump -Fc -U postgres -d keycloak' > keycloak.dump

tar -czf postgres-backup-<ts>.tar.gz globals.sql keycloak.dump inventory.txt
```

`pg_dump -Fc` (custom format) is what you want — it's compressed, parallel-restorable, and stable across Postgres minor versions.

Schedule it with cron on your laptop, or on `ms-1` if you have a NAS mount:

```cron
# Nightly at 03:00
0 3 * * * /home/me/infra/scripts/dr/postgres-backup.sh /home/me/backups/postgres/
```

Then a second cron that ships them off-site:

```bash
# rclone is the simplest off-site shipper for a homelab
rclone copy ~/backups/postgres/ b2:my-bucket/homelab-backups/postgres/ \
  --include "postgres-backup-*.tar.gz" \
  --max-age 24h
```

Backblaze B2 is ~€0.005/GB/month — a year of nightly Postgres backups is under €1.

### Test the restore

Six months in, you'll forget how. Test now, while you remember:

```bash
# Spin up a throwaway Postgres pod
kubectl run pg-restore-test --rm -it --restart=Never \
  --image=postgres:17.9 \
  --env=POSTGRES_PASSWORD=test \
  -- bash

# Inside the pod (in another terminal, copy the tarball in)
kubectl cp ~/backups/postgres/postgres-backup-<ts>.tar.gz pg-restore-test:/tmp/
kubectl exec -it pg-restore-test -- bash
cd /tmp && tar -xzf postgres-backup-*.tar.gz

psql -U postgres -f globals.sql
pg_restore -U postgres -d postgres -C keycloak.dump
psql -U postgres -d keycloak -c '\dt'   # tables exist?
```

If that works, the backup is real. Schedule a quarterly reminder to do this against actual fresh hardware.

## Sealed Secrets master key — the one-shot artefact

The Sealed Secrets controller has a private key it uses to decrypt every committed `SealedSecret`. Lose it and every committed sealed secret is undecryptable noise — you'd have to regenerate every credential by hand.

Capture it:

```bash
kubectl get secret -n kube-system \
  -l sealedsecrets.bitnami.com/sealed-secrets-key=active \
  -o yaml \
  > ~/sealed-secrets-master-key.$(date +%Y%m%d).yaml
```

Two destinations:

1. **Your password manager** as a secure note (it's a few KB of YAML).
2. **Off-site encrypted storage** alongside the Postgres backups.

`scripts/dr/sealed-secrets-key-restore.sh` brings it back on a freshly installed cluster:

```bash
# After installing the controller fresh
kubectl apply -f ~/sealed-secrets-master-key.<ts>.yaml
kubectl -n kube-system delete pod -l app.kubernetes.io/name=sealed-secrets-controller
```

The controller picks the restored key as active on next start. Every previously committed `SealedSecret` decrypts.

Capture the key once, refresh the backup whenever the controller's key rotates (rare; usually only when you reinstall the controller). The old key file is still useful — keep at least the last two.

## Keycloak realm — the third backup

Keycloak's realm config (clients, identity providers, custom flows, role mappings) lives in Postgres. The Postgres backup captures it. But Keycloak also offers a JSON export that's a much friendlier rebuild path:

```bash
ssh ms-1 'kubectl -n identity exec deployment/keycloak -- \
  /opt/keycloak/bin/kc.sh export \
    --dir /tmp/realm-export \
    --realm homelab'

ssh ms-1 'tar -czf /tmp/realm-export.tar.gz -C /tmp/realm-export .'
scp ms-1:/tmp/realm-export.tar.gz ~/backups/keycloak/
```

Restore:

```bash
# After Keycloak comes back up on a rebuilt cluster
kubectl cp ~/backups/keycloak/realm-export.tar.gz identity/keycloak-<podid>:/tmp/
kubectl -n identity exec deployment/keycloak -- bash -c \
  'cd /tmp && tar -xzf realm-export.tar.gz && /opt/keycloak/bin/kc.sh import --dir /tmp'
```

The JSON export captures everything *except* user passwords (Keycloak hashes them with the realm's secret). Users created via GitHub IdP are recreated transparently on next login; users with local passwords would need to reset them.

## What you don't need to back up

- **Workload manifests** — they're in Git already.
- **Argo CD config** — same.
- **TLS certs from cert-manager** — they re-issue automatically; faster to wait 60 seconds than to back them up.
- **Pod images** — pulled from registries on demand.
- **K3s installation files** — `/var/lib/rancher/k3s/server/db/` is rebuilt by the install script.

The shrinking list of "things actually unique to this cluster" is exactly the three artefacts above. That's the point of building it this way.

## What "off-site" really means

The threats to plan for, in rough order of likelihood:

1. **Operator error** — you `kubectl delete pvc data-postgresql-0` instead of `--dry-run=client`. Off-site protects against you.
2. **Hardware failure** — `wk-1`'s SSD dies. Off-site protects against this trivially.
3. **House fire / theft / flood** — backups on the operator laptop and on the cluster don't help. *Truly* off-site (S3, B2, OneDrive, a friend's NAS) does.
4. **Ransomware** — modern ransomware hunts cloud storage credentials and encrypts those too. Object lock / immutability on the bucket is the answer; B2 and S3 both support it.

A homelab probably doesn't need ransomware-grade defences. But "encrypted on a third-party object store with versioning enabled" is roughly two `rclone` flags, and it solves all four.

## What you should have now

- A `postgres-backup-*.tar.gz` from running the script today
- A `sealed-secrets-master-key-*.yaml` saved to your password manager and one off-site location
- A `realm-export.tar.gz` from Keycloak
- A cron job (or a calendar reminder) to run the Postgres backup nightly
- Notes on which off-site destination you used so future-you knows where to look

The next chapter assumes all of this. It's the runbook for "the cluster is gone; here's how to bring it back."

→ Next: [The recovery runbook](/cortex/homelab-from-scratch/operate-and-recover-the-recovery-runbook)
