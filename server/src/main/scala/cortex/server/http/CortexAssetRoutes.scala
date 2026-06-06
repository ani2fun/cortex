package cortex.server.http

import zio.*
import zio.http.*

/**
 * Serves binary assets from the Cortex content tree.
 *
 * URL: `GET /api/cortex/asset/{book}/{rest...}` → file at `${cortexRoot}/{book}/{rest}`.
 *
 * Why under `/api`? The Vite dev server proxies `/api → :8080` already, so the same URL works in both
 * `bin/dev` (Vite at :5173 proxying to the JVM at :8080) and in production (single JVM at :8080 fronting both
 * API and static frontend). Putting the asset route under `/cortex/...` would require an extra Vite proxy
 * rule and would collide with the SPA chapter routes that are intentionally swallowed by index.html.
 *
 * Path-safety, content-type resolution, and the read-or-404 are all delegated to [[FileServer]] — the same
 * module [[StaticRoutes]] uses, so the security-critical path-traversal guard has one home and one test
 * surface. A resolved real path that escapes `cortexRoot` (via `..`, an absolute input, or a symlink), and
 * non-files, all 404.
 */
object CortexAssetRoutes:

  def from(cortexRoot: String): Routes[Any, Response] =
    val fileServer = FileServer(cortexRoot)
    Routes(
      Method.GET / "api" / "cortex" / "asset" / trailing ->
        Handler.fromFunctionZIO[(zio.http.Path, Request)] { case (rest, _) =>
          fileServer.serve(rest.encode)
        }
    )
