---
title: '4. docker-compose: the whole stack on your laptop'
summary: A real app is many containers — Cortex needs Postgres, Redis, Mongo, Keycloak, and more. docker-compose declares the whole multi-service stack in one YAML file and brings it all up, networked together, with one command.
---

# 4. docker-compose: the whole stack on your laptop

## TL;DR

> One container is rarely a whole system. Cortex's server needs **Postgres** (data), **Redis** (rate limits), **Mongo** (events), **go-judge** (the code sandbox), **LikeC4** (diagrams), and **Keycloak** (auth) — *seven* containers that must run together and talk to each other. **docker-compose** declares them all in one `docker-compose.yml`, on a shared network with friendly hostnames, and **`docker compose up`** starts the entire stack with a single command. It's how you run all of Cortex on your laptop, reproducibly — and it's the conceptual stepping stone to Kubernetes, which does the same orchestration at production scale.

## 1. Motivation

You can `docker run` one container by hand. But a real application is a *system* of cooperating services, and running seven `docker run` commands — each with the right ports, environment variables, volumes, and network wiring, in the right order, remembering how they connect — is miserable and error-prone. Every developer would do it slightly differently; onboarding would be a day of "now run this, then this, wait, set this env var..." The whole reproducibility win of containers (Chapter 1) collapses if *assembling the stack* is a manual ritual.

docker-compose restores it. You *declare* the entire multi-container stack — every service, its image, its config, its connections — in one version-controlled YAML file. Then `docker compose up` reads that declaration and brings the whole thing to life: pulls the images, creates a shared network so services can reach each other by name, sets the environment, mounts the volumes, and starts everything. A new contributor clones Cortex, runs one command, and has Postgres, Redis, Mongo, Keycloak, the sandbox, the diagram server, *and* the app all running and wired together. The stack becomes as reproducible as a single container — and the YAML is also a precise, readable *map* of what the system is made of.

## 2. Intuition (Analogy)

A **theatrical production's setup**. A play isn't just an actor — it's actors, lighting, sound, set pieces, and a stage manager coordinating them. You *could* set each up individually every night ("now position the lights, now cue the sound board, now..."), but instead you have a **production script** that lists every element and how they connect, and a stage manager who reads it and brings the whole show up together, in concert.

`docker-compose.yml` is that production script, and `docker compose up` is the stage manager. The file lists each "cast member" (service/container) — Postgres, Redis, the app — with its role and how it connects to the others (the shared network, the dependencies). One command brings the entire production up, coordinated. Tear it down with `docker compose down`, and the whole stage clears. No more setting up each light by hand every night.

## 3. Formal Definition

- **docker-compose.** A tool that defines and runs **multi-container** applications from a declarative `docker-compose.yml` file. (Now built into Docker as `docker compose`.)
- **Service.** One entry under `services:` — a container (or set of replicas) defined by an `image` (or a `build:` context), plus its configuration: `ports`, `environment`, `volumes`, `depends_on`, etc. Each service is reachable from the others by its **service name** as a hostname (compose creates a shared network with DNS).
- **Key fields:**
  - `image:` — which image to run (or `build:` to build one from a Dockerfile).
  - `ports: ["8081:8080"]` — map a host port to a container port (`host:container`).
  - `environment:` — env vars for the container (the config inputs from Part 1, Chapter 14).
  - `volumes:` — persist data outside the container (so a `docker compose down` doesn't lose the database) or mount host files in.
  - `depends_on:` — start ordering (start B after A — though it doesn't wait for A to be *ready*; that's what `healthcheck` is for).
  - `healthcheck:` — a command to test whether a service is actually *ready* (e.g. `pg_isready` for Postgres).
- **Lifecycle:** `docker compose up` (start all), `down` (stop + remove), `logs`, `ps`. `up <service>` starts one service (and its dependencies).
- **Networking.** Compose creates a network where each service resolves the others by name — so the app connects to `db:5432`, not an IP. (This is exactly the service-discovery idea Kubernetes formalizes — Chapter 7.)

> docker-compose declares a multi-container stack in one YAML and brings it all up, networked by service name, with one command. It makes a whole *system* as reproducible as a single container — and previews the orchestration Kubernetes does at scale.

## 4. Worked Example — Cortex's seven-service stack

Cortex's [`docker-compose.yml`](https://github.com/ani2fun/cortex/blob/main/docker-compose.yml) declares the whole local system. An abridged view:

```yaml
services:
  db:                                    # PostgreSQL — the main datastore
    image: postgres:17-alpine
    environment:
      POSTGRES_USER: cortex
      POSTGRES_PASSWORD: cortex
      POSTGRES_DB: cortex
    volumes:
      - pgdata:/var/lib/postgresql/data   # persist data across `down`/`up`
    ports: ["5432:5432"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U cortex -d cortex"]   # "is Postgres actually ready?"
      interval: 5s

  redis:                                 # rate-limit buckets (Part 3, Ch 20)
    image: redis:7-alpine

  mongo:                                 # event log
    image: mongo:7
    volumes: [mongodata:/data/db]

  keycloak:                              # the identity plane (Part 3)
    image: quay.io/keycloak/keycloak:26.0
    command: ["start-dev", "--import-realm"]
    ports: ["8081:8080"]

  go-judge:                              # the code-execution sandbox (/api/run)
    build: { dockerfile: docker/Dockerfile.go-judge }

  likec4:                                # the diagram server
    build: { dockerfile: docker/Dockerfile.likec4 }

  app:                                   # Cortex itself
    build: .                              # built from the Dockerfile in Chapter 3
    environment:
      DB_URL: jdbc:postgresql://db:5432/cortex     # reaches `db` by SERVICE NAME
      REDIS_URL: redis://redis:6379
      MONGO_URI: mongodb://mongo:27017
      EXECUTOR_URL: http://go-judge:5050
    depends_on: [db, redis, mongo, go-judge, likec4, keycloak]
    ports: ["8080:8080"]
```

Read it as the system's map. Seven services, each a container: the three datastores (`db`, `redis`, `mongo`), the auth provider (`keycloak`), the two helpers (`go-judge`, `likec4`), and `app` (Cortex, built from the Chapter 3 Dockerfile). The magic is in `app`'s environment: `DB_URL: jdbc:postgresql://db:5432/...` reaches Postgres by the *service name* `db` — compose's shared network resolves it, no IPs. (And recall Part 1, Chapter 14: these env vars override `application.conf`'s localhost defaults — here pointing at the *compose service names*; in production, Kubernetes points them at cluster services.) `volumes` keep the database alive across restarts; `healthcheck` lets compose know when Postgres is truly ready; `depends_on` orders startup. Run `docker compose up`, and a contributor has *all of Cortex* — the exact stack from Part 1, 2, and 3 — running locally, wired together, from one command. That's the `./bin/dev` workflow this book keeps referencing.

## 5. Build It

Run this. It models a compose stack: services that reference each other by name, brought up with dependencies resolved — and shows why name-based wiring beats hard-coded addresses.

```python run
# A compose file as data: each service, and which others it needs.
services = {
    "db":       {"image": "postgres:17", "needs": []},
    "redis":    {"image": "redis:7",     "needs": []},
    "keycloak": {"image": "keycloak:26", "needs": []},
    "go-judge": {"image": "go-judge",    "needs": []},
    "app":      {"image": "cortex",      "needs": ["db", "redis", "keycloak", "go-judge"],
                 "env": {"DB_URL": "jdbc:postgresql://db:5432/cortex",   # reaches `db` by NAME
                         "REDIS_URL": "redis://redis:6379"}},
}

def compose_up(services):
    started = []
    def start(name):
        if name in started: return
        for dep in services[name]["needs"]:     # start dependencies first
            start(dep)
        print(f"   starting {name} ({services[name]['image']})")
        started.append(name)
    for name in services:
        start(name)
    return started

print("docker compose up:")
order = compose_up(services)
print("\nstarted in dependency order:", order)
# Name-based wiring: `app` finds `db` via the shared network, no IP needed.
print("\napp connects to:", services["app"]["env"]["DB_URL"], "(service name, not an IP)")
```

**Now break it.** Change `app`'s `DB_URL` to a hard-coded IP like `172.17.0.3:5432` instead of the service name `db`. It might work *once*, but the next `docker compose up` could assign Postgres a *different* IP and the connection breaks — whereas the *name* `db` always resolves to wherever Postgres is. Service-name addressing is what makes the stack robust to restarts and rescheduling, and it's *exactly* the abstraction Kubernetes Services provide at scale (Chapter 7). Compose is teaching you the orchestration mindset on one machine before Kubernetes scales it across many.

## 6. Trade-offs & Complexity

| docker-compose | Manual `docker run` × N |
|---|---|
| Whole stack in one declarative file | Many commands, easy to get wrong |
| One command up/down, reproducible | Manual order, wiring, cleanup |
| Service-name networking | Juggle IPs/ports by hand |
| Great for *local dev* on one host | — |
| Not a production orchestrator | — |

The honest limit: docker-compose is a *single-host, local-development* tool. It brilliantly assembles a stack on *your machine*, but it doesn't do the things production needs — running across *many* machines, self-healing crashed containers, rolling updates with zero downtime, autoscaling. For that you need Kubernetes (the rest of this Part). The good news: compose is the perfect *on-ramp*. The concepts — declarative service definitions, name-based networking, health checks, dependency ordering — are exactly the ones Kubernetes generalizes to a cluster. Master compose locally and Kubernetes feels like "the same ideas, bigger."

## 7. Edge Cases & Failure Modes

- **`depends_on` ≠ "wait until ready."** It controls *start order*, not readiness — `app` may start before Postgres is *accepting connections*. Use `healthcheck` (+ `depends_on: condition: service_healthy`) or app-level retries (Cortex's startup migration tolerates a not-yet-ready DB by retrying/failing fast).
- **Lost data on `down -v`.** `docker compose down` keeps named volumes; `down -v` *deletes* them — wiping your database. Know the difference before you run it.
- **Hard-coded IPs.** Addressing services by IP instead of name breaks across restarts. Always use service names.
- **Compose in production.** Reaching for compose to run production on one big server forfeits self-healing, rolling updates, and multi-host scaling. It's a dev tool; production wants an orchestrator.

## 8. Practice

> **Exercise 1 — Read the map.** From Cortex's compose file, list the seven services and one-line what each provides to the app.

<details>
<summary><strong>Answer</strong></summary>

The compose file *is* the system's map (§4) — each `services:` entry is one container with one role:

- **`db` (Postgres)** — the main datastore; Cortex's primary relational data (reached at `db:5432`).
- **`redis`** — the rate-limit buckets (the token buckets behind request throttling).
- **`mongo`** — the event log / event store.
- **`keycloak`** — the identity plane: authentication and the OIDC issuer (the auth provider).
- **`go-judge`** — the code-execution sandbox that backs `/api/run` (runs learner-submitted code safely).
- **`likec4`** — the diagram server that renders the architecture diagrams.
- **`app`** — Cortex itself, built from the Chapter 3 Dockerfile; the server that ties all six dependencies together.

Read together, the file tells you *exactly* what the system is made of — three datastores, an auth provider, two helper services, and the app — which is half of why a declarative compose file is so valuable: it doubles as documentation.

</details>

> **Exercise 2 — Name, not IP.** Explain why `app` connects to `db:5432` rather than an IP address, and what breaks if you hard-code an IP.

<details>
<summary><strong>Answer</strong></summary>

Compose creates a **shared network with DNS**, where every service is reachable by its **service name** as a hostname (§3). So `db` resolves to *wherever Postgres's container currently is* — and crucially, it keeps resolving correctly even after a restart moves Postgres to a new address.

If you **hard-code an IP** (say `172.17.0.3:5432`), it may work *once*, but container IPs are assigned dynamically: the next `docker compose up` (or a restart of the `db` container) can give Postgres a *different* IP, and your hard-coded address now points at nothing — or worse, at some *other* container. The connection breaks, silently and unpredictably.

The name `db` is a stable handle over an unstable address. This is the same indirection lesson as Kubernetes Services (Chapter 7) — address the *name*, never the IP — just at single-host scale. It's why compose is the natural on-ramp to orchestration.

</details>

> **Exercise 3 — Ready vs started.** `depends_on: [db]` starts the app after Postgres's container starts, but the app still can't connect. Why, and what two mechanisms fix it?

<details>
<summary><strong>Answer</strong></summary>

**Why:** `depends_on` controls *start order*, not *readiness* (§3, §7). It guarantees Postgres's **container** has been started before `app`'s — but a started container is not the same as a database *accepting connections*. Postgres needs seconds to initialize (load data, open its socket) after its container starts, and during that gap `app` can be up and trying to connect to a database that isn't listening yet, so the connection fails.

**Two fixes:**

1. **`healthcheck` + `depends_on: condition: service_healthy`.** Give `db` a real readiness probe (`pg_isready -U cortex -d cortex`), and tell `app` to wait not just for the container to *start* but for that health check to *pass*. Now `app` starts only once Postgres is genuinely ready.
2. **App-level retries.** Have the app *tolerate* a not-yet-ready dependency by retrying the connection (or failing fast and being restarted). Cortex's startup migration does exactly this — it retries against a DB that isn't accepting connections yet, rather than assuming it's instantly available.

The principle: never assume "container started" means "service ready." Either make the dependency *prove* readiness (healthcheck) or make the dependent *survive* the gap (retries) — ideally both.

</details>

```quiz
{
  "prompt": "What does docker-compose do that running individual `docker run` commands doesn't?",
  "input": "Choose one:",
  "options": [
    "Declares an entire multi-container stack in one file and brings it all up together — networked so services reach each other by name — with a single command, reproducibly",
    "Makes containers run faster",
    "Compiles the application from source",
    "Deploys the app to production Kubernetes"
  ],
  "answer": "Declares an entire multi-container stack in one file and brings it all up together — networked so services reach each other by name — with a single command, reproducibly"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[Docker — Compose overview & file reference](https://docs.docker.com/compose/)** — services, networks, volumes, healthchecks, and the full YAML schema.
- **[Docker — Compose vs Kubernetes](https://docs.docker.com/compose/)** — where compose ends and an orchestrator begins.
- **[Cortex `docker-compose.yml`](https://github.com/ani2fun/cortex/blob/main/docker-compose.yml)** — the real seven-service stack you run locally with `./bin/dev`.

---

**Next:** compose is great on one laptop — but production needs many machines, self-healing, and zero-downtime updates. That's orchestration. Meet Kubernetes and its core idea: desired state. → [5. Why orchestration? Desired state](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/why-orchestration)
