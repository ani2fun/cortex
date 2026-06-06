# 32. Example — modelling a system with the C4 model

> A worked example, not a tutorial. We sketch a small architecture using the C4 model and render it with [LikeC4](https://likec4.dev) — embedded live in this page from a source file checked into this repository.

## TL;DR

> **C4** is a way to describe a software system at four progressively-deeper zoom levels: **Context**, **Container**, **Component**, **Code**. **LikeC4** is an open-source tool that compiles a small DSL into a navigable canvas. The diagram below is the live overview of this site's homelab platform, regenerated on every push from [`workspace.c4`](https://github.com/ani2fun/codefolio/blob/main/content/cortex/system-design/05-application-architecture/c4/workspace.c4).

## 1. Why a model, not a picture

A whiteboard drawing made on Tuesday is on a different whiteboard than the one wiped on Wednesday. The drawing was the artefact. There was no model behind it — nothing the team could come back to, version, or argue with on Friday.

A **model** is the structure underneath the drawing: the elements (people, systems, containers, components), how they relate, and a few rules about how the picture should look. The picture is then *derived* from the model. Wipe the picture, the model is still in source control. Change the model, the picture re-renders.

The C4 model is one such structure. Simon Brown's original framing:

- **Context** — the system as a black box plus the actors that talk to it.
- **Container** — the system opened up: deployable units (a backend, a database, a worker) and their wire-level conversations.
- **Component** — one container opened up: the internal modules and their wiring.
- **Code** — the actual code. Rarely drawn at this level; UML or a tree view in your IDE serves better.

> **Rule of thumb:** if you can't decide which level a box belongs to, the model isn't wrong — the level boundary is. Pick the one closer to "container" and move on.

## 2. The example

The diagram below describes the platform that serves this very page. It's deliberately small: four nodes, one actor, four relationships. Nothing about it is invented — every box maps to a machine that exists.

<iframe
  src="/c4/view/index"
  width="100%"
  height="640"
  style="border: 1px solid var(--border, #2b2b2b); border-radius: 8px;"
  loading="lazy"
  title="Homelab C4 overview"
></iframe>

> Pan and zoom inside the frame. The canvas is a real LikeC4 SPA, not an image — drag to scroll, scroll to zoom.

## 3. The source behind the picture

Here is the entire DSL that produces the canvas above:

```
specification {
  element actor
  element system
  element container
}

model {
  user = actor 'Operator' {
    description 'Aniket — administers the homelab'
  }

  homelab = system 'Homelab Platform' {
    description 'K3s cluster across four Ubuntu nodes'

    ms1  = container 'ms-1'        { description 'K3s server and admin host' }
    wk1  = container 'wk-1'        { description 'Database worker' }
    wk2  = container 'wk-2'        { description 'GitOps worker' }
    edge = container 'vm-1 (edge)' { description 'Public ingress edge' }

    user -> ms1 'kubectl over private mesh'
    edge -> ms1 'overlay traffic'
    edge -> wk1 'overlay traffic'
    edge -> wk2 'overlay traffic'
  }
}

views {
  view index of homelab {
    title 'Homelab overview'
    include *
  }
}
```

Three things to notice:

1. **Elements are typed.** `actor`, `system`, and `container` are not styling labels; they carry semantics. LikeC4 lays out an `actor` differently from a `container` so the picture stays consistent across views.
2. **Relationships are nouns, not arrows.** `user -> ms1 'kubectl over private mesh'` says *what* the relationship is, not just that one exists. Good models are arguable; arrows alone are not.
3. **Views select, they don't redraw.** The single `view index of homelab { include * }` says "show everything from the homelab system". Adding a new container automatically extends this view — you don't go back and edit the picture.

## 4. What the C4 model is *not*

- **Not a deployment diagram.** C4's "container" is a runtime process (a Spring Boot app, a Postgres instance, a Lambda), not a Docker container in the orchestration sense. Resist conflating the two.
- **Not a substitute for sequence diagrams.** The C4 model describes structure, not behaviour. If you need to show "request flows through A, then B, then C", reach for a sequence diagram and keep C4 as the index.
- **Not architecture-as-code.** It is architecture-*as-diagrams*-as-code: the model captures what's drawn, not what's deployed. The gap between model and reality must be closed by review — no tool will do that for you.

## 5. Where to go next

- [LikeC4 documentation](https://likec4.dev/docs) — the full DSL, view-filtering, theming, embedding.
- [The C4 model](https://c4model.com) — Simon Brown's original site, including the Containers / Components zoom rules.
- [Structurizr DSL](https://docs.structurizr.com/dsl) — LikeC4's nearest sibling. Different syntax, same idea; valuable to read both before picking one.

> **Operator's note for this site:** the diagram above is rendered by the `likec4` Service in the same Kubernetes namespace as Codefolio. It has no public ingress; this page reverse-proxies `/c4/*` to it in-cluster. The threat model is the same as any public blog post — the diagram is visible to anyone visiting this page — so the source DSL is restricted to "topology + technology + public hostnames" and excludes internal IPs, software versions, and secret-management details.
