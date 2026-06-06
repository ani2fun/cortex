---
title: PostgreSQL on a pinned node
summary: A StatefulSet pinned to `wk-1` with a `local-path` PVC. The init script that bootstraps an app database on first boot. Why we don't use a Postgres operator yet — and what we'd switch to (CloudNativePG) if the homelab outgrew this.
---

## What "stateful" means in homelab terms

The earlier chapters dealt with **stateless** workloads: whoami, Argo CD, Traefik. Pods can be killed and recreated freely; nothing is lost. Postgres is the opposite — its data lives on disk, on a specific node, and any change to where the pod runs is a change to where the data lives.

There are three ways to deploy Postgres in Kubernetes, ordered by ambition:

| Pattern | Storage | Replication | Pinning | Operational cost |
|---|---|---|---|---|
| **StatefulSet on local-path PVC** | One node's disk | None | Hard pin via nodeSelector | Lowest |
| **StatefulSet on networked storage** (Longhorn, Ceph, NFS) | Replicated across nodes | Storage layer handles it | Soft (pod can move) | Medium |
| **Postgres operator** (CloudNativePG, Zalando, Crunchy) | Whatever the operator orchestrates | Full streaming replication | Operator handles it | Highest |

For a four-node homelab with a single Postgres instance, the first option is simplest and has the best performance. The cluster behind these docs runs option 1. The "what to do when this isn't enough" answer is option 3 — covered briefly at the end.

## The architecture

```d2
direction: down

ns: databases-prod namespace {
  shape: rectangle

  ss: StatefulSet postgresql {
    shape: rectangle
    pod: Pod postgresql-0\nimage: postgres:17.9
  }

  svc: Service postgresql\nClusterIP\n:5432
  hl: Service postgresql-hl\nHeadless\n(StatefulSet uses)

  pvc: PVC data-postgresql-0\nstorageClassName: local-path\n80 Gi

  cm: ConfigMap postgresql-init\n01-create-app-db.sh

  sec: Secret postgresql-auth\nfrom Sealed Secret\nsuperuser pw + app db pw
}

wk1: wk-1 (label kakde.eu/postgresql=true) {
  shape: rectangle
  disk: /var/lib/rancher/k3s/storage/...
}

ns.pvc -> wk1.disk: bound to local-path provisioner
ns.ss.pod -> ns.pvc: mounts /var/lib/postgresql/data
ns.ss.pod -> ns.cm: mounts /docker-entrypoint-initdb.d
ns.ss.pod -> ns.sec: env from secretKeyRef
ns.svc -> ns.ss.pod: forwards 5432

other: Other namespaces
other -> ns.svc: only if labeled\n(see next chapter)
```

Six pieces. Each has a single job; together they're a Postgres database that survives node reboots, gets bootstrapped automatically on first install, and is consumable from any namespace authorised to talk to it.

## Step 1: Namespace + label

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: databases-prod
  labels:
    kubernetes.io/metadata.name: databases-prod
```

The label `kubernetes.io/metadata.name` is automatic on Kubernetes 1.21+, but let's be explicit. It's used by NetworkPolicy in the next chapter.

## Step 2: The Sealed Secret

You'll provision Postgres' superuser password and the app-DB credentials via Sealed Secrets. Generate them once, never share them again, never put them in plaintext anywhere except the password manager:

```bash
SUPERUSER_PASS="$(openssl rand -base64 32)"
APP_DB_PASS="$(openssl rand -base64 32)"

kubectl create secret generic postgresql-auth \
  --namespace databases-prod \
  --from-literal=postgres-superuser-password="${SUPERUSER_PASS}" \
  --from-literal=app-db-name="keycloak" \
  --from-literal=app-db-user="keycloak" \
  --from-literal=app-db-password="${APP_DB_PASS}" \
  --dry-run=client -o yaml | \
kubeseal --cert /tmp/sealed-secrets-cert.pem --format yaml \
  > infra/deploy/postgresql/sealedsecret-postgresql-auth.yaml
```

Save `${SUPERUSER_PASS}` and `${APP_DB_PASS}` to your password manager. Commit only the SealedSecret YAML to Git.

## Step 3: The init ConfigMap

When `postgres:17.9` starts on a fresh PVC, it executes any `*.sh` or `*.sql` files in `/docker-entrypoint-initdb.d/`. We mount a ConfigMap there:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-init
  namespace: databases-prod
data:
  01-create-app-db.sh: |
    #!/bin/sh
    set -eu
    export PGPASSWORD="${POSTGRES_PASSWORD}"

    psql -v ON_ERROR_STOP=1 \
      --username "${POSTGRES_USER}" \
      --dbname postgres \
      --set=app_db_name="${APP_DB_NAME}" \
      --set=app_db_user="${APP_DB_USER}" \
      --set=app_db_password="${APP_DB_PASSWORD}" <<'EOSQL'
    DO $do$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'app_db_user') THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', :'app_db_user', :'app_db_password');
      END IF;
    END $do$;

    SELECT format('CREATE DATABASE %I OWNER %I', :'app_db_name', :'app_db_user')
    WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'app_db_name') \gexec

    REVOKE ALL ON DATABASE :"app_db_name" FROM PUBLIC;
    GRANT ALL PRIVILEGES ON DATABASE :"app_db_name" TO :"app_db_user";
    EOSQL
```

Idempotent: `IF NOT EXISTS` and `WHERE NOT EXISTS` mean it's safe to re-run if the pod restarts. The script is a no-op after first boot.

## Step 4: The Services

Two of them. One `ClusterIP` for the canonical address, one headless `Service` for the StatefulSet's pod identity:

```yaml
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: databases-prod
  labels:
    app.kubernetes.io/name: postgresql
spec:
  selector:
    app.kubernetes.io/name: postgresql
  ports:
    - name: postgres
      port: 5432
      targetPort: 5432
---
apiVersion: v1
kind: Service
metadata:
  name: postgresql-hl
  namespace: databases-prod
  labels:
    app.kubernetes.io/name: postgresql
spec:
  clusterIP: None
  selector:
    app.kubernetes.io/name: postgresql
  ports:
    - name: postgres
      port: 5432
      targetPort: 5432
```

Apps will dial `postgresql.databases-prod.svc.cluster.local:5432`. The StatefulSet uses `postgresql-hl` for its `serviceName` field, which gives the pod a stable DNS name `postgresql-0.postgresql-hl.databases-prod.svc.cluster.local` — useful if we ever scale to multi-replica.

## Step 5: The StatefulSet

The big one. The cluster's actual manifest is ~115 lines; the load-bearing pieces:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql
  namespace: databases-prod
spec:
  serviceName: postgresql-hl
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: postgresql
  template:
    metadata:
      labels:
        app.kubernetes.io/name: postgresql
    spec:
      nodeSelector:
        kakde.eu/postgresql: "true"             # ← pins to wk-1
      terminationGracePeriodSeconds: 120
      containers:
        - name: postgresql
          image: postgres:17.9
          ports:
            - name: postgres
              containerPort: 5432
          env:
            - name: POSTGRES_USER
              value: postgres
            - name: POSTGRES_DB
              value: postgres
            - name: POSTGRES_PASSWORD
              valueFrom: { secretKeyRef: { name: postgresql-auth, key: postgres-superuser-password } }
            - name: APP_DB_NAME
              valueFrom: { secretKeyRef: { name: postgresql-auth, key: app-db-name } }
            - name: APP_DB_USER
              valueFrom: { secretKeyRef: { name: postgresql-auth, key: app-db-user } }
            - name: APP_DB_PASSWORD
              valueFrom: { secretKeyRef: { name: postgresql-auth, key: app-db-password } }
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          startupProbe:
            exec: { command: [sh, -c, 'pg_isready -h 127.0.0.1 -p 5432 -U "$POSTGRES_USER" -d postgres'] }
            periodSeconds: 5
            failureThreshold: 60          # 5 minutes to start
          readinessProbe:
            exec: { command: [sh, -c, 'pg_isready -h 127.0.0.1 -p 5432 -U "$POSTGRES_USER" -d postgres'] }
            periodSeconds: 10
          livenessProbe:
            exec: { command: [sh, -c, 'pg_isready -h 127.0.0.1 -p 5432 -U "$POSTGRES_USER" -d postgres'] }
            periodSeconds: 20
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
            - name: initdb
              mountPath: /docker-entrypoint-initdb.d
      volumes:
        - name: initdb
          configMap:
            name: postgresql-init
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: [ReadWriteOnce]
        storageClassName: local-path
        resources:
          requests:
            storage: 80Gi
```

Things worth highlighting:

- **`nodeSelector: kakde.eu/postgresql: "true"`** — this is what pins it to `wk-1`. Match the label you set in [Where things are allowed to run](/cortex/homelab-from-scratch/kubernetes-base-where-things-are-allowed-to-run).
- **`PGDATA=/var/lib/postgresql/data/pgdata`** — Postgres complains if `/var/lib/postgresql/data` itself is the data directory because the directory has files (the lost+found) Postgres doesn't recognise. Always use a subdirectory.
- **Three probes** that all run the same `pg_isready` check. The startupProbe gives Postgres up to 5 minutes to come up; the readinessProbe takes over after, so traffic doesn't hit Postgres mid-startup; the livenessProbe restarts the pod if it goes silent.
- **`storageClassName: local-path`** — K3s' bundled `local-path-provisioner` creates a directory on the node's filesystem and binds the PVC to it. Simple, fast, single-node.
- **`80Gi`** — sized for a homelab. K3s' local-path-provisioner doesn't enforce quotas; this is documentation.

## Apply and verify

```bash
kubectl apply -f deploy/postgresql/  # whole directory

# Watch the pod come up — startup is ~30 seconds
kubectl -n databases-prod get pods -w
# postgresql-0   0/1   Pending      (waiting for PVC)
# postgresql-0   0/1   ContainerCreating
# postgresql-0   0/1   Running   (startupProbe failing during init)
# postgresql-0   1/1   Running   (probes pass)

# Test connection from a throwaway pod
kubectl run -it --rm -n databases-prod psql-client --image=postgres:17.9 --restart=Never -- \
  psql 'postgresql://postgres:<password>@postgresql.databases-prod.svc.cluster.local:5432/keycloak' \
  -c '\l'
# Lists databases — should include "keycloak"
```

## What about a Postgres operator?

A reasonable question. CloudNativePG (CNPG) is the modern recommendation: it manages clusters of Postgres pods with streaming replication, automatic failover, scheduled backups, point-in-time recovery, and certificate rotation. It's excellent.

For a homelab specifically:

- **Single replica.** A homelab doesn't have the network bandwidth or operational headroom for multi-replica Postgres failover. The DR path is "restore from backup," not "fail over."
- **Operator complexity.** CNPG adds a dozen CRDs, a webhook controller, dozens of new YAML fields. Worth it at scale; overkill at one node.
- **Backups.** CNPG's biggest feature is automated WAL archiving to S3. We're going to do this manually with cron in chapter 9 — fine for the homelab, would be unacceptable for a real product.

Promise yourself: **migrate to CNPG when you outgrow this**, not before. The trigger is usually "I now have two services that absolutely cannot lose data" — at one (Keycloak realm), this StatefulSet is fine.

## What you should have now

- `databases-prod` namespace
- A `postgresql-auth` SealedSecret applied
- `postgresql-init` ConfigMap with the bootstrap script
- Two Services and one StatefulSet
- `postgresql-0` pod Running on `wk-1`, with `1/1 Ready`
- A `psql -c '\l'` from inside the cluster shows the `keycloak` database exists

The next chapter locks down access — only the apps that should reach Postgres can.

→ Next: [Network policies keep Postgres internal](/cortex/homelab-from-scratch/stateful-services-network-policies-keep-postgres-internal)
