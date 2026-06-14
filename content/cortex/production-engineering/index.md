# Production Engineering

> **The textbook you are reading is also the system you will learn to build.**
> Every page on this site is served by a real backend — Scala running ZIO, a PostgreSQL database whose schema is managed by Liquibase, an identity layer built on Keycloak and OAuth 2.0, all packaged into Docker images and run on a Kubernetes cluster. This book takes that one real system apart, from first principles, and shows you how every piece works — by pointing at the actual code that is, right now, delivering these words to your screen.

## Why this book exists

Most tutorials teach a technology in a vacuum. You learn ZIO with a toy "hello world", OAuth with a fictional "Acme Corp", Kubernetes with an `nginx` that does nothing. Then you try to use them together on something real and discover that nobody told you how the pieces *fit*.

This book is the opposite. There is **one system** — Cortex, the application you are using right now — and we study it the way an engineer actually has to: as a living thing that must be **written**, given a **memory**, **secured**, and **shipped**. Those four verbs are the four Parts of this book.

## The arc: the life of a backend service

```d2
direction: right

write: "① Write it" {
  tooltip: "Part 1 — ZIO"
  shape: rectangle
}
evolve: "② Evolve its data" {
  tooltip: "Part 2 — Liquibase"
  shape: rectangle
}
secure: "③ Secure it" {
  tooltip: "Part 3 — Keycloak & OAuth 2.0"
  shape: rectangle
}
ship: "④ Ship it" {
  tooltip: "Part 4 — Docker & Kubernetes"
  shape: rectangle
}

write -> evolve -> secure -> ship

cortex: "The result: cortex.kakde.eu\n(this website)" {
  shape: cloud
}
ship -> cortex: "runs as"
```

A backend is born as **code** — and we write it with ZIO, a framework that turns side effects into values you can reason about. It needs a **memory** that can change shape safely over months of releases — that is Liquibase, version control for your database schema. Once it holds anything that matters, it must know **who is talking to it and what they're allowed to do** — that is identity, taught through OAuth 2.0, OpenID Connect, and Keycloak. And finally it has to **leave your laptop and run somewhere reliable, around the clock** — that is Docker and Kubernetes.

Each Part stands on its own, but read in order they tell a single story: how a pile of source files becomes a service that ten thousand people can trust.

## The four Parts

### [Part 1 — ZIO: Effects, Layers & Concurrency](/cortex/production-engineering/zio)
What is a "side effect", really, and why does it make programs hard to reason about? We rebuild the idea of a program-as-a-value from scratch, then meet `ZIO[R, E, A]` — the type at the heart of Cortex's backend — and learn how dependencies, errors, and concurrency all become ordinary values you can pass around. *Grounded in Cortex's real `Main.scala`, service layers, and database pool.*

### [Part 2 — Liquibase: Evolving a Database Without Fear](/cortex/production-engineering/liquibase)
You cannot just `ALTER TABLE` on a live database with real users. We start from the problem — schema drift, the "works on my machine" database — and build up to migrations as version control for your schema, then to the advanced refactors (expand/contract, backfills) that let you change a running system's shape without downtime. *Grounded in Cortex's real changelog and startup migration runner.*

### [Part 3 — Identity & Access Management: OAuth 2.0, OIDC & Keycloak](/cortex/production-engineering/iam-keycloak-oauth)
Why did the whole industry stop asking you for your password? We answer that question properly, derive OAuth 2.0 and PKCE from the attacks they were invented to stop, decode a real JWT by hand, and stand up Keycloak — then trace the exact sign-in that happens when you click **Edit** on a code block in this very site. *Self-contained enough to read first if security is what brought you here.*

### [Part 4 — Docker & Kubernetes: Shipping the System](/cortex/production-engineering/docker-kubernetes)
A container is a way to ship the whole machine, not just the code. We build up from images and layers to the multi-stage `Dockerfile` that packages Cortex, then to Kubernetes — pods, services, ingress, probes, secrets — and follow a `git push` all the way to a running pod serving traffic. *Companion to [Homelab from Scratch](/cortex/homelab-from-scratch), which builds the cluster this app runs on.*

## Who this is for

A motivated high-school student can read this. We assume you can read a little code and have written *something* that ran — but we assume **nothing** about distributed systems, cryptography, the JVM, or the cloud. Every term is defined the first time it appears. Every diagram is interactive: drag it, step through it, change the inputs. Every "Build It" section is real code you can run in the page.

> **How to read a chapter.** Each one follows the same rhythm: a one-line **TL;DR**, a **motivation** (usually a real outage or a real decision), an **intuition** by analogy, a **formal definition**, a **worked example** with an interactive diagram, a hands-on **Build It**, the **trade-offs**, the **failure modes**, **practice** exercises, and pointers to the real thing **in the wild**. You do not have to read every section — but the analogy and the worked example are where the understanding lives.

**Begin at the beginning of the arc:** [Part 1 — What is a side effect, really? →](/cortex/production-engineering/zio/why-effects/what-is-a-side-effect)
&nbsp;&nbsp;·&nbsp;&nbsp;or jump to [Part 2 — Liquibase](/cortex/production-engineering/liquibase), [Part 3 — Identity & Access](/cortex/production-engineering/iam-keycloak-oauth), [Part 4 — Docker & Kubernetes](/cortex/production-engineering/docker-kubernetes).
