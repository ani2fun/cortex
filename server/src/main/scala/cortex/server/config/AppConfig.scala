package cortex.server.config

import zio.*
import zio.config.magnolia.deriveConfig

final case class DbConfig(url: String, user: String, password: String)
final case class RedisConfig(url: String, ttlSecs: Int)
final case class MongoConfig(uri: String, database: String)

/**
 * One fixed-window rate-limit bucket: at most `limit` requests per `windowSeconds`.
 */
final case class RateLimitBucket(windowSeconds: Int, limit: Int)

/**
 * Rate-limit policy for `/api/run`. `anonymous` is keyed per client IP (short window); `authenticated` is
 * keyed per JWT `sub` (hourly window). Enforced by `server/http/RateLimiter.scala`, and bypassed entirely
 * when `auth.enabled` is false.
 */
final case class RateLimitConfig(anonymous: RateLimitBucket, authenticated: RateLimitBucket)

/**
 * Configuration for the code-execution proxy.
 *
 * `executorUrl` points at the self-hosted go-judge sandbox (e.g. `http://go-judge:5050`). If unset, the
 * `/api/run` endpoint returns 503 — the misconfiguration is made visible rather than silently swallowed.
 * There is a single backend by design (no runtime failover).
 *
 * `executorAuthToken` is an optional bearer token (go-judge's `ES_AUTH_TOKEN`); when unset the in-cluster
 * NetworkPolicy is the access control and no `Authorization` header is sent.
 *
 * `rateLimit` bounds how often `/api/run` can be called — see [[RateLimitConfig]].
 */
final case class RunnerConfig(
    executorUrl: Option[String],
    executorAuthToken: Option[String],
    rateLimit: RateLimitConfig
)

/**
 * Upstream URL for the LikeC4 reverse proxy that fronts `/c4/`. Defaults to the in-cluster Kubernetes Service
 * name; override with `LIKEC4_URL` for local dev (`http://localhost:8090`) or docker compose
 * (`http://likec4:8080`).
 */
final case class LikeC4Config(upstreamUrl: String)

/**
 * Path to the Cortex content tree. Default is `./content/cortex` relative to the working directory (matches
 * the layout shipped with the Docker image, where the Dockerfile copies content/ to /app/content). The
 * structure is fully convention-driven: each immediate subdirectory is a book; nested directories become
 * sections; `.md` files are chapters. Optional `book.json` / `_section.json` provide titles and metadata.
 *
 * `autoReload` checks the maximum mtime of files under the root on every index request and rebuilds the
 * cached index if anything changed. Cheap (~10ms at 200 chapters), and means you can drop a new directory and
 * refresh the page without restarting the server. Disable in prod where content ships baked-in.
 */
final case class CortexConfig(root: String, autoReload: Boolean)

/**
 * Path to the blog content tree. Default `./content/blogs` relative to the working directory. Each immediate
 * `*.md` file under the root is a single post; the slug is the filename without `.md`. `autoReload` checks
 * the maximum mtime of files under the root on every index request and rebuilds the cached index if anything
 * changed — same pattern as Cortex. Disable in prod by setting `BLOG_AUTO_RELOAD=false`.
 */
final case class BlogConfig(root: String, autoReload: Boolean)

/**
 * Keycloak / OIDC integration for the Cortex-edit auth gate (ADR-0013).
 *
 *   - `enabled` is the master switch. When `false` (typical for `bin/dev`), the server still runs but the
 *     auth verifier short-circuits to `AuthFailure.AuthDisabled` for required-auth endpoints and to "no
 *     claims" for optional-auth endpoints. `/api/auth/config` reports `enabled=false` to the SPA, which skips
 *     initialising `keycloak-js` entirely.
 *   - `issuerUrl` is the Keycloak realm URL (e.g. `https://keycloak.kakde.eu/realms/apps-prod`). Used as both
 *     the `iss` claim we validate and the base for the JWKS URL
 *     (`{issuerUrl}/protocol/openid-connect/certs`).
 *   - `realm` + `clientId` are passed through to the SPA via `/api/auth/config` so `keycloak-js` can boot
 *     against the same Keycloak install the server is validating against.
 *   - `audience` is the expected `aud` / `azp` claim value. Keycloak's quirk: tokens issued to a public SPA
 *     client carry `aud: ["account"]` but `azp: "<clientId>"`; we accept either, so this typically defaults
 *     to `clientId` and rarely needs overriding.
 *   - `jwksCacheTtlSec` is informational (Nimbus' `JWKSourceBuilder.cache(...)` carries its own TTL); we keep
 *     the knob to surface the choice in config rather than burying it in code.
 */
final case class AuthConfig(
    enabled: Boolean,
    issuerUrl: String,
    realm: String,
    clientId: String,
    audience: String,
    jwksCacheTtlSec: Int
)

final case class AppConfig(
    port: Int,
    staticDir: String,
    db: DbConfig,
    redis: RedisConfig,
    mongo: MongoConfig,
    runner: RunnerConfig,
    likec4: LikeC4Config,
    cortex: CortexConfig,
    blog: BlogConfig,
    auth: AuthConfig
)

object AppConfig:

  val config: Config[AppConfig] = deriveConfig[AppConfig].nested("cortex")

  val live: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer.fromZIO(ZIO.config(config))
