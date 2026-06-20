---
title: '14. Configuration as a value: ZIO Config'
summary: A port, a database URL, Keycloak settings — configuration is just typed values read from a source. ZIO Config loads them with environment overrides, and Cortex slices one AppConfig into focused pieces.
---

# 14. Configuration as a value: ZIO Config

## TL;DR

> Configuration — the port, the DB URL, Keycloak's issuer — is data your app reads at startup, and ZIO models it as **typed values** loaded by **ZIO Config**. Cortex installs a config provider in `bootstrap` (Chapter 3) that reads `application.conf`, then `AppConfig.live` parses it into a typed `AppConfig` and *slices* it into focused configs (`DbConfig`, `RedisConfig`, `AuthConfig`) that individual layers depend on. The killer pattern is **environment overrides**: a default in the file, overridable by an env var — so the same build runs on your laptop and in Kubernetes by changing the environment, not the code.

## 1. Motivation

The same application binary must run in many environments — your laptop, CI, staging, production — each needing different values: a local Postgres vs a cluster one, `AUTH_ENABLED=false` in dev vs `true` in prod, a dev Keycloak vs `keycloak.kakde.eu`. Hard-coding these is obviously wrong; but so is the common mess of reading raw `System.getenv` strings scattered through the code, parsing them ad hoc, and discovering a typo'd or missing value only when that code path runs at 2 a.m.

ZIO Config treats configuration the way ZIO treats everything: as *typed values* with explicit *requirements*, loaded from a *provider*. You describe the shape you need (a `DbConfig` with a `url`, `user`, `password`), point ZIO Config at a source (a file, env vars, or both), and it produces the typed value or **fails at startup** with a clear message if something's missing or malformed. No scattered `getenv`, no late surprises — config is validated once, up front, and flows through your layers as a real value.

## 2. Intuition (Analogy)

A **stage play touring different theaters**. The *script* (your code) is identical everywhere. What changes per venue is the **set list and house settings** — this theater's lighting board addresses, that one's stage dimensions, the local fire-exit rules. The touring company doesn't rewrite the play for each city; they carry a *settings sheet* that the same production reads on arrival.

Configuration is that settings sheet. Your code is the script — written once. `application.conf` and the environment are the per-venue settings. ZIO Config is the stage manager who reads the sheet on arrival and refuses to start the show if a critical setting (the main power feed) is missing — better to know before the curtain than mid-performance.

## 3. Formal Definition

- **`Config[A]`** — a *description* of how to read a value of type `A` from configuration (e.g. `Config.string("url")`, composed into a case class via `deriveConfig` or manual mapping). Like an endpoint or a layer, it's a *value*.
- **`ConfigProvider`** — the *source*: where values come from. ZIO ships providers for environment variables, system properties, and (via `zio-config-typesafe`) HOCON files. `TypesafeConfigProvider.fromResourcePath()` reads `application.conf` from the classpath.
- **Reading config in an effect** — `ZIO.config(myConfig)` produces the typed value, failing with a `Config.Error` if absent/invalid. Layers like `AppConfig.live` wrap this.
- **`bootstrap`** — Cortex installs the provider *before* `run` via `Runtime.setConfigProvider(...)` (Chapter 3), so every `ZIO.config` call uses it.
- **Config slicing** — derive small, focused configs from one big one: `DbConfig` is just the `db { ... }` slice of `AppConfig`. A layer that only needs the DB settings depends on `DbConfig`, not the whole `AppConfig` — least privilege for config.
- **HOCON env-override pattern** — in `application.conf`, write a default and an env override on the same key; the env var wins when present.

> Config is typed values from a provider, validated once at startup, sliced so each layer sees only what it needs. The same artifact runs everywhere; the *environment* selects the values.

## 4. Worked Example — Cortex's config

Cortex's [`application.conf`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/application.conf) uses the env-override pattern throughout:

```hocon
db {
  url      = "jdbc:postgresql://localhost:5432/cortex"   # default (local dev)
  url      = ${?DB_URL}                                  # overridden by $DB_URL if set
  user     = "cortex"
  user     = ${?DB_USER}
  password = "cortex"
  password = ${?DB_PASSWORD}
}

auth {
  enabled   = true
  enabled   = ${?AUTH_ENABLED}
  issuerUrl = "https://keycloak.kakde.eu/realms/apps-prod"
  issuerUrl = ${?KEYCLOAK_ISSUER_URL}
  ...
}
```

The `${?DB_URL}` line is HOCON for "if the environment variable `DB_URL` exists, use it; otherwise keep the previous value." So on your laptop you get `localhost:5432`; in Kubernetes, the deployment sets `DB_URL=jdbc:postgresql://postgresql.databases-prod...` and the *same binary* connects to the cluster database (you'll see those env vars in Part 4's `deployment.yaml`). No build flags, no profiles compiled in — just the environment.

Then `bootstrap` installs the provider and `AppConfig.live` reads + slices it:

```scala
// In Main (Chapter 3): make application.conf the config source for the whole app.
override val bootstrap = Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())

// AppConfig.live parses the typed config; DbConfig is the `db { }` slice.
val dbCfg: ZLayer[AppConfig, Nothing, DbConfig] =
  ZLayer.fromFunction((c: AppConfig) => c.db)   // slice: a layer that only exposes DB settings
```

Now `DataSource.live` (Chapter 9) depends on `DbConfig` — not the whole `AppConfig` — so it sees exactly the three values it needs. If `application.conf` were missing `db.url` *and* `$DB_URL` were unset, the app would **fail at boot** with a precise `Config.Error`, not connect to the wrong place silently. Validation up front, least-privilege slices, environment-driven values: configuration done the ZIO way.

## 5. Build It

Run this. It models the env-override pattern and config slicing — a default, an environment override, and focused slices each layer reads.

```scala run
@main def run(): Unit =
  // Simulate environment variables (in Kubernetes these are set by the deployment).
  val env = Map("DB_URL" -> "jdbc:postgresql://cluster-db:5432/cortex") // try emptying this map!

  // A default, overridden by env if the override key is present (the ${?VAR} pattern).
  def config(keyPath: String, default: String): String =
    val overrideKey = keyPath.replace(".", "_").toUpperCase // db.url -> DB_URL
    env.getOrElse(overrideKey, default)

  // The full AppConfig, read once at startup:
  val appConfig = Map(
    "db" -> Map(
      "url"  -> config("db.url", "jdbc:postgresql://localhost:5432/cortex"),
      "user" -> config("db.user", "cortex")
    ),
    "auth" -> Map(
      "enabled"   -> config("auth.enabled", "true"),
      "issuerUrl" -> config("auth.issuerUrl", "https://keycloak.kakde.eu/realms/apps-prod")
    )
  )

  // Config slicing: DataSource only gets the db slice, not the whole thing.
  val dbConfig = appConfig("db")
  println(s"DataSource sees only: $dbConfig")
  println("  -> url came from " +
    (if env.contains("DB_URL") then "ENV (cluster)" else "the default (localhost)"))

  // Validation at startup: a required value missing => fail BEFORE serving.
  def require(cfg: Map[String, String], key: String): String =
    cfg.get(key).filter(_.nonEmpty) match
      case Some(v) => v
      case None    => sys.error(s"FATAL config error: missing $key — refusing to start")
  println(s"validated db.url: ${require(dbConfig, "url")}")
```

**Now break it.** Empty the `env` map (`val env = Map.empty[String, String]`) and re-run: every value falls back to the localhost defaults — the same code, different environment, different config. Then give `db.url` an empty default and watch `require` abort startup with a clear message instead of letting the app boot and fail mysteriously on the first query. That fail-fast-on-bad-config is the same instinct as fail-fast-on-bad-migration (Chapter 3, and Part 2): surface configuration problems at boot, loudly, not deep in a request.

## 6. Trade-offs & Complexity

| ZIO Config (typed, validated) | Scattered `getenv` parsing |
|---|---|
| Validated once at startup, clear errors | Fails late, in some code path |
| Typed values flow through layers | Raw strings parsed ad hoc |
| Env overrides without code changes | Often hard-coded or branchy |
| Slices give least-privilege config | Whole config (or globals) everywhere |
| A config DSL to learn | "Just read the env var" |

The cost is modeling config as types and learning the provider/override mechanics — more than a quick `sys.env("PORT")`. The payoff is that misconfiguration becomes a *loud, early* failure with a precise message, the same binary runs in every environment, and each layer depends only on the config it actually uses. For anything deployed to more than one place (every real service), that's essential.

## 7. Edge Cases & Failure Modes

- **Secrets in `application.conf`.** Defaults like `password = "cortex"` are fine for *local dev only*. In production, the value comes from an env var fed by a Kubernetes secret (Part 4) — never commit real secrets to the config file.
- **Silent wrong defaults.** A default that's *valid but wrong* for prod (e.g. a localhost URL) won't fail validation — it'll just connect to the wrong place. Make prod-critical values *required* (no usable default) so a missing env var fails loudly.
- **Type mismatches.** `AUTH_ENABLED=ye` won't parse as a boolean — ZIO Config fails at startup, which is correct. Don't catch-and-default config errors; let them stop the boot.
- **Over-broad config deps.** A layer depending on the whole `AppConfig` when it needs one field couples it to unrelated settings. Slice configs (`DbConfig`, `AuthConfig`) like Cortex does.

## 8. Practice

> **Exercise 1 — Read the override.** Given `port = 8080` then `port = ${?PORT}` in HOCON, what port runs (a) on a laptop with no `PORT` set, (b) in a container with `PORT=9000`?

<details>
<summary><strong>Answer</strong></summary>

- **(a) laptop, no `PORT` set → `8080`.** `${?PORT}` means "if the env var `PORT` exists, substitute it; otherwise *keep the previous value*." With `PORT` unset, the second line is a no-op and the default `8080` stands.
- **(b) container with `PORT=9000` → `9000`.** Now `${?PORT}` resolves, so the second assignment overrides the first.

That's the whole env-override pattern: a sensible default baked into `application.conf`, overridable per environment by setting an env var — so the *same binary* binds `8080` on your laptop and `9000` (or whatever Kubernetes injects) in production, with no code change and no compiled-in profiles.

</details>

> **Exercise 2 — Slice it.** Cortex's `AuthConfig` is the `auth { }` slice. Why does `Auth.live` depend on `AuthConfig` rather than `AppConfig`? What does that buy in testing and clarity?

<details>
<summary><strong>Answer</strong></summary>

It's **least privilege for config**: `Auth.live` needs only the auth settings (`enabled`, `issuerUrl`, …), so it should depend on `AuthConfig` — the `auth { }` slice — not the whole `AppConfig`. What that buys:

- **Testing:** to test `Auth.live` you only have to supply a small `AuthConfig`, not construct an entire `AppConfig` with a DB URL, Redis settings, Mongo, etc. that `Auth` doesn't use. The layer's *type* tells you exactly what it needs, and that's all you wire up.
- **Clarity / coupling:** depending on the whole `AppConfig` would couple `Auth` to *unrelated* settings — a change to, say, the DB config shape would needlessly touch auth's dependencies. The narrow slice makes the real dependency honest and keeps the layer insulated from the rest of the config.

Same instinct as narrowing `R` in Chapter 8: a component should declare only the dependencies it actually uses, and config is just another dependency.

</details>

> **Exercise 3 — Make it fail right.** A prod service has `db.url` default to `localhost`. It boots fine in prod but every query fails. Diagnose, and propose a config change so this fails *at startup* instead.

<details>
<summary><strong>Answer</strong></summary>

**Diagnosis:** the env var that should point at the cluster database (e.g. `DB_URL`) is **unset in prod**, so `db.url = ${?DB_URL}` falls back to its `localhost` default. `localhost` is a *valid but wrong* value — ZIO Config validates it fine, the app boots, and only the first real query fails because nothing is listening on `localhost:5432` in the prod container. A wrong-but-parseable default passes validation silently.

**Fix:** make the prod-critical value **required** — no usable default — so a missing env var fails *loudly at boot* with a precise `Config.Error` instead of booting and dying per-request. Options:

```hocon
# Option A: no default at all — config read fails fast if DB_URL is unset.
db { url = ${DB_URL} }            # note: ${...} (required), not ${?...} (optional override)

# Option B: keep localhost ONLY for local dev, but require the env var in prod
# (e.g. supply it via a separate prod config / Kubernetes secret), so prod has no fallback.
```

The principle (from §7 and Chapter 3): don't let a *valid-but-wrong* default mask a missing setting. Surface configuration problems at startup, the same fail-fast instinct as fail-fast-on-bad-migration — loud and early beats silent and deep in a request.

</details>

```quiz
{
  "prompt": "In Cortex's `application.conf`, what does the pattern `url = \"jdbc:...localhost...\"` followed by `url = ${?DB_URL}` achieve?",
  "input": "Choose one:",
  "options": [
    "A default value (localhost) that is overridden by the DB_URL environment variable when it is set — same binary, different environments",
    "It connects to two databases at once",
    "It encrypts the database URL",
    "It retries the connection if it fails"
  ],
  "answer": "A default value (localhost) that is overridden by the DB_URL environment variable when it is set — same binary, different environments"
}
```

## Your Turn

Before you move on, check your understanding with the coach — explain the idea, apply it, weigh the trade-offs, then defend your reasoning.

<div class="concept-coach"></div>

## In the Wild

- **[ZIO Config docs](https://zio.dev/zio-config/)** — `Config`, providers, and deriving config from case classes.
- **[HOCON spec — substitutions](https://github.com/lightbend/config/blob/main/HOCON.md#substitutions)** — the `${?VAR}` override syntax Cortex relies on.
- **[Cortex `application.conf`](https://github.com/ani2fun/cortex/blob/main/server/src/main/resources/application.conf)** — the real config with env overrides, sliced into typed pieces.

---

**Next:** the last piece — proving it works. Because effects are values and dependencies are provided, ZIO code is unusually testable: swap real services for fakes, control time, assert on results. → [15. Testing ZIO](/cortex/production-engineering/zio/cortex-in-practice/testing-zio)
