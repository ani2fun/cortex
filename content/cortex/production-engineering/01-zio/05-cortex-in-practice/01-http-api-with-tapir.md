---
title: '13. An HTTP API with Tapir + zio-http'
summary: Cortex describes its endpoints as values with Tapir, then serves them on zio-http. Describing an endpoint once gives you the server, a typed client, and OpenAPI docs — all from the same definition.
---

# 13. An HTTP API with Tapir + zio-http

## TL;DR

> Cortex's HTTP layer is two libraries working together. **Tapir** lets you *describe* an endpoint as a **value** — its method, path, inputs, outputs, and error type — independently of any server. **zio-http** is the high-performance server that *runs* those endpoints as ZIO effects. The payoff of "endpoint as a value": from one description you derive the **server route**, a **typed client** (the Scala.js frontend uses exactly this), and **OpenAPI docs** — no drift between them. Cortex's `HttpApp` composes all its route groups and serves them on one port.

## 1. Motivation

The "programs as values" idea (Chapter 1) extends beautifully to HTTP. Most web frameworks define an endpoint by *attaching behavior to a route* — the description and the handler are fused, and the client and the docs are written *separately*, by hand, and immediately start drifting from the server. You've lived the consequences: a frontend that calls `/api/usrs` because someone typo'd, docs that say a field is required when it's optional, a client that breaks silently when the server changes.

Tapir breaks the fusion. An **endpoint** is a *value* describing the contract: "POST `/api/run`, body is a `RunRequest`, returns a `RunResponse`, can fail with `ApiError`." That value knows nothing about servers. From it you *derive* the server (attach a ZIO handler), the client (the same value, interpreted as an HTTP call), and the OpenAPI spec — all from one source of truth. Cortex's Scala.js frontend literally shares the endpoint definitions with the backend, so a change to the contract is a *compile error on both sides* rather than a runtime surprise. That's the whole pitch: define once, derive everything, never drift.

## 2. Intuition (Analogy)

A **contract** versus the **parties who fulfill it**. A good legal contract describes the agreement precisely — what each side provides, what they get, what happens if things go wrong — *without* being either party. The same contract binds the buyer and the seller; both read the identical document, so they can't disagree about the terms.

A Tapir endpoint is that contract. It says exactly what goes in (the request), what comes out (the response), and how it can fail (the error type). The **server** is one party fulfilling it ("given this request, here's the response"); the **client** is the other ("I'll send a request shaped like this and expect that response"). Because both are derived from the *same* contract value, they literally cannot disagree about the API's shape — and a third reader, the **OpenAPI doc**, is just the contract printed for humans.

## 3. Formal Definition

- **Tapir `Endpoint[I, E, O, R]`** — a *value* describing an HTTP endpoint: its inputs `I` (path, query, headers, body), error output `E`, success output `O`, and required capabilities `R` (e.g. streaming). Built declaratively: `endpoint.post.in("api" / "run").in(jsonBody[RunRequest]).out(jsonBody[RunResponse]).errorOut(...)`.
- **Server interpreter** — `endpoint.serverLogic(handler)` attaches a ZIO handler, producing a `ZServerEndpoint`. A list of these is interpreted into zio-http `Routes`.
- **Client interpreter** — the *same* endpoint value, interpreted by `SttpClientInterpreter`, becomes a function that performs the HTTP call (this is what Cortex's `ApiClient` uses — Part 3, Chapter 18/20).
- **Docs interpreter** — the same value generates an OpenAPI/Swagger spec.
- **zio-http** — the server runtime: `Server.serve(routes).provide(Server.live, configLayer)`. It runs each request as a fiber (Chapter 10), so concurrency is automatic.
- **Single source of truth.** Cortex keeps endpoint definitions in a `shared` module cross-compiled to JVM (server) and JS (client), so server and client share the *exact* same contract.

> One endpoint value → server route + typed client + OpenAPI docs. Describe the contract once; derive every party from it; eliminate drift by construction.

## 4. Worked Example — Cortex's HttpApp

Cortex assembles its API in [`HttpApp.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/HttpApp.scala). The shape:

```scala
final private class HttpAppLive(...) extends HttpApp:
  // Each route group is a list of Tapir ZServerEndpoints, interpreted into zio-http Routes.
  private val apiRoutes        = ApiRoutes.routes(cfg, helloPipeline, codeRun, cortex, blog, auth, rateLimiter, submissions)
  private val cortexAssetRoutes = CortexAssetRoutes.from(cfg.cortex.root)
  private val likec4Routes      = LikeC4ProxyRoutes.from(cfg.likec4.upstreamUrl)
  private val staticRoutes      = StaticRoutes.from(cfg.staticDir, ...)

  override def serve: Task[Unit] =
    Server
      .serve(apiRoutes ++ cortexAssetRoutes ++ likec4Routes ++ staticRoutes.routes)  // one combined route set
      .provide(
        ZLayer.succeed(Server.Config.default.port(cfg.port)),   // bind the configured port
        Server.live                                              // the zio-http server
      )
      .unit
```

Read it as composition all the way down. Each `*Routes` is a *value* — a bundle of endpoints with their handlers. `++` combines them into one route set (route groups compose like any value). `Server.serve(...)` turns that into the effect "serve these routes forever," and `.provide(...)` supplies the server config (port) and the zio-http `Server.live` runtime — the same `provide` from Chapter 8. The whole HTTP server is, once again, a single `ZIO` value handed to the runtime; `serve` is what `Main` (`ZIO.serviceWithZIO[HttpApp](_.serve)`, Chapter 3) ultimately runs. And because the underlying endpoints are Tapir values, the very same `ApiRoutes` definitions are reused by the Scala.js client to call them, with the request/response types checked on both sides.

## 5. Build It

Run this. It models "endpoint as a value" — define a contract once, then derive a server handler, a client call, and a doc from it, all consistent by construction.

```scala run
@main def run(): Unit =
  // Define the CONTRACT once — it knows nothing about server or client.
  final case class Endpoint(
      method: String,
      path: String,
      inputType: String,
      outputType: String,
      errorType: String
  )

  val runCode = Endpoint("POST", "/api/run", "RunRequest", "RunResponse", "ApiError")

  // Derive the SERVER from it: attach a handler.
  def server(ep: Endpoint, handler: String => String): String => String =
    request =>
      println(s"   [server] ${ep.method} ${ep.path}: ${ep.inputType} -> ${ep.outputType}")
      handler(request)

  // Derive the CLIENT from the SAME value: it knows the shape to send/expect.
  def client(ep: Endpoint): String => String =
    request =>
      println(s"   [client] sending ${ep.inputType} to ${ep.method} ${ep.path}, expecting ${ep.outputType}")
      s"<${ep.outputType}>"

  // Derive the DOCS from the SAME value.
  def openapi(ep: Endpoint): String =
    s"""   ${ep.method} ${ep.path}
       |     request: ${ep.inputType}
       |     response: ${ep.outputType}
       |     error: ${ep.errorType}""".stripMargin

  val handle = server(runCode, req => "RunResponse(ok)")
  val send   = client(runCode)

  println("server route:"); handle("RunRequest(scala)")
  println("client call:"); println("   got " + send("RunRequest(scala)"))
  println("\nOpenAPI (same source of truth):")
  println(openapi(runCode))
```

**Now break it.** Change `runCode`'s `outputType` to `"RunResultV2"` — and notice the server, the client, *and* the docs all update together, automatically, because they derive from one value. In hand-written code, you'd have to find and edit three places and you'd miss one. In Cortex (with real types in a shared module), that single change is a *compile error* on any caller that hasn't adapted — drift caught by the compiler, not by a user hitting a 500.

## 6. Trade-offs & Complexity

| Tapir endpoints-as-values | Annotation/route-DSL frameworks |
|---|---|
| One source → server + client + docs | Client and docs hand-written, drift |
| Contract checked by the compiler | Mismatches found at runtime |
| Reusable across JVM/JS (shared module) | Per-platform reimplementation |
| More upfront structure | Quicker to scribble one route |
| zio-http concurrency for free (fibers) | Varies |

The cost is a steeper start: you describe inputs/outputs/errors explicitly instead of slapping a handler on a path, and Tapir's combinators take a little learning. The payoff compounds with every endpoint and every client: no drift, a free typed client, free docs, and compile-time safety across the whole API. For a system with a real frontend (like Cortex's SPA), sharing the contract is transformative.

## 7. Edge Cases & Failure Modes

- **Fusing description and handler.** If you find yourself unable to generate a client or docs, you've probably coupled the endpoint to the server. Keep the endpoint a pure value; attach logic via `serverLogic`.
- **Error type omitted.** An endpoint with no modeled `errorOut` pushes all failures into generic 500s. Model expected failures (Chapter 4) as typed error outputs so clients can react.
- **Giant single route file.** Compose route *groups* (as Cortex does — api, assets, likec4, static) rather than one mega-list; they're values, so split freely.
- **Blocking handlers.** A route handler that does JDBC must use `attemptBlocking` (Chapter 11), or it blocks zio-http's fibers under load. The HTTP layer doesn't save you from the blocking-pool rule.

## 8. Practice

> **Exercise 1 — Three from one.** Name the three artifacts Cortex derives from a single Tapir endpoint definition, and the bug each derivation prevents compared to hand-writing it.

<details>
<summary><strong>Answer</strong></summary>

From one endpoint *value* Cortex derives:

1. **The server route** (`serverLogic` → a `ZServerEndpoint` interpreted into zio-http `Routes`). Prevents the server *handling a different shape than it advertises* — the handler is type-checked against the endpoint's declared inputs/outputs.
2. **A typed client** (the same value interpreted by the sttp client; Cortex's Scala.js frontend uses it). Prevents **client/server drift** — the typo'd path (`/api/usrs`) or wrong request type that a hand-written client would only reveal as a runtime 404/500. Here it's a *compile error* on both sides.
3. **The OpenAPI/Swagger docs** (the same value interpreted as a spec). Prevents **stale docs** — docs that say a field is required when it's optional, because the spec is generated from the contract, not maintained by hand.

The single point: all three come from *one source of truth*, so they "literally cannot disagree about the API's shape." Hand-writing them means three places to keep in sync, and you *will* miss one — drift caught by a user, not the compiler.

</details>

> **Exercise 2 — Model an endpoint.** Write (in pseudo-Tapir) the endpoint for `GET /api/recent?limit=10` returning a `RecentCalls`, failing with `HelloFailure`. What are its inputs, output, and error?

<details>
<summary><strong>Answer</strong></summary>

```scala
val recent: Endpoint[Unit, HelloFailure, RecentCalls, Any] =
  endpoint.get
    .in("api" / "recent")              // path
    .in(query[Int]("limit"))           // input: the ?limit=10 query parameter
    .out(jsonBody[RecentCalls])         // success output O
    .errorOut(jsonBody[HelloFailure])   // error output E
```

- **Inputs `I`:** the path `/api/recent` plus a query parameter `limit: Int` (`?limit=10`). The path segments aren't "inputs" you receive, but the query param is — so `I = Int`.
- **Success output `O`:** `RecentCalls` (serialized as JSON in the 200 body).
- **Error output `E`:** `HelloFailure` (a *typed* failure, so the client can react to it instead of seeing a generic 500).

The point of writing it as a value: this same description yields the server route, the typed client call, and the docs — and modeling `HelloFailure` as `errorOut` (rather than letting it fall through to a 500) is the typed-errors discipline from Chapter 4 applied at the HTTP boundary.

</details>

> **Exercise 3 — Compose routes.** Cortex does `apiRoutes ++ cortexAssetRoutes ++ ...`. Explain why route groups composing with `++` is the same "programs as values" idea from Chapter 1.

<details>
<summary><strong>Answer</strong></summary>

Because each `*Routes` group **is a value** — a bundle of endpoints with their handlers, describing behavior but not yet running anything — and `++` is just *combining values* into a bigger value, exactly like composing two effects into one (Chapter 1). `apiRoutes ++ cortexAssetRoutes ++ ...` produces one larger route set the same way small effects compose into a single top-level effect; nothing executes until that combined value is handed to `Server.serve(...)` and ultimately to the runtime (`serve`, run by `Main` — Chapter 3). Routes being values is what lets Cortex *split* its API into focused groups (api, assets, likec4, static) and recombine them freely, instead of one mega-list — the same "describe as composable values, run once at the edge" funnel, applied to HTTP routing.

</details>

```quiz
{
  "prompt": "What is the key benefit of describing an HTTP endpoint as a value with Tapir?",
  "input": "Choose one:",
  "options": [
    "From one definition you derive the server route, a typed client, AND the OpenAPI docs — so they can't drift apart",
    "It makes HTTP requests faster",
    "It replaces the need for a database",
    "It automatically writes the business logic"
  ],
  "answer": "From one definition you derive the server route, a typed client, AND the OpenAPI docs — so they can't drift apart"
}
```

## In the Wild

- **[Tapir docs](https://tapir.softwaremill.com/)** — endpoints as values, and the server/client/docs interpreters.
- **[zio-http docs](https://zio.dev/zio-http/)** — the server runtime Cortex serves on.
- **[Cortex `HttpApp.scala`](https://github.com/ani2fun/cortex/blob/main/server/src/main/scala/cortex/server/HttpApp.scala)** & **[`api/openapi.yaml`](https://github.com/ani2fun/cortex/blob/main/api/openapi.yaml)** — the real route composition and the generated API spec.

---

**Next:** the server reads a port, a database URL, Keycloak settings — all *configuration*. ZIO treats config as typed values too. Meet ZIO Config and Cortex's `AppConfig`. → [14. Configuration as a value: ZIO Config](/cortex/production-engineering/zio/cortex-in-practice/configuration-as-a-value)
