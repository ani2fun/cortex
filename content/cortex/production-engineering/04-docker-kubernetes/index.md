# Part 4 — Docker & Kubernetes: Shipping the System

> **"It works on my machine" is not a deployment strategy.** A container is a way to ship the *whole machine* — the OS libraries, the runtime, the app, the exact versions of everything — as one immutable artifact. Kubernetes is what runs thousands of those artifacts reliably, heals them when they die, and routes traffic to the survivors. This Part takes Cortex from a `Dockerfile` on your laptop to a pod serving `cortex.kakde.eu`.

## What you'll be able to do

Read Cortex's real multi-stage `Dockerfile` and explain why it has two stages. Bring the whole stack up on your laptop with `docker-compose`. Read a Kubernetes `Deployment`, `Service`, and `Ingress` and trace a request from the public internet to the container. Explain what a readiness probe is *for*, and what happens to traffic when it fails.

## Companion to *Homelab from Scratch*

[Homelab from Scratch](/cortex/homelab-from-scratch) builds the **cluster** — four machines, a private mesh, K3s, an edge. This Part is the other half: how an **application** gets *onto* a cluster and stays healthy there. We point at the real manifests that deploy Cortex (they live in the sibling `infra` repo) and the real `Dockerfile` and `docker-compose.yml` in the app repo.

## Chapters

**Containers from First Principles**
1. [What problem does Docker actually solve?](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/what-problem-does-docker-solve)
2. [Images, layers & the union filesystem](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/images-layers-union-fs)
3. [Writing a Dockerfile: Cortex's multi-stage build](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/writing-a-dockerfile)
4. [docker-compose: the whole stack on your laptop](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/docker-compose)

**Kubernetes Concepts**
5. [Why orchestration? Desired state](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/why-orchestration)
6. [Pods, ReplicaSets, Deployments](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/pods-deployments)
7. [Services & cluster networking](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/services-networking)
8. [Ingress, TLS & DNS](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/ingress-tls-dns)
9. [Config and Secrets](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/config-and-secrets)
10. [Probes: liveness, readiness, startup](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/probes)
11. [Resources: requests & limits](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/resources)
12. [Security context](/cortex/production-engineering/docker-kubernetes/kubernetes-concepts/security-context)

**Cortex in Production**
13. [The full path: git push → pod](/cortex/production-engineering/docker-kubernetes/cortex-in-production/the-full-path)
14. [Rollouts and rollbacks](/cortex/production-engineering/docker-kubernetes/cortex-in-production/rollouts-and-rollbacks)
15. [Observing a live pod](/cortex/production-engineering/docker-kubernetes/cortex-in-production/observing-a-live-pod)

*Versions in play: multi-stage Docker build (Temurin 21 + Node 20), Kubernetes via K3s, Traefik, cert-manager, images on `ghcr.io`.*

**Begin:** [1. What problem does Docker actually solve? →](/cortex/production-engineering/docker-kubernetes/containers-from-first-principles/what-problem-does-docker-solve)
