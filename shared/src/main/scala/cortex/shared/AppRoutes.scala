package cortex.shared

/** Shared route and public-asset path segments used by the SPA router and the production static fallback. */
object AppRoutes:
  val Root           = "/"
  val Demo           = "demo"
  val Cortex         = "cortex"
  val Blogs          = "blogs"
  val IndexHtml      = "index.html"
  val Assets         = "assets"
  val Images         = "img"
  val Certificates   = "certificates"
  val CvFile         = "Aniket-Kakde-CV-EN.pdf"
  val SilentCheckSso = "silent-check-sso.html"

  /**
   * One reserved top-level route the SPA owns. `segment` is the first path segment (`blogs`, `demo`, and the
   * legacy `cortex`); `hasNestedRoutes` is true when the SPA also handles deeper paths under it
   * (`/blogs/{slug}`, `/cortex/{book}/{chapter}`).
   */
  final case class SpaRoute(segment: String, hasNestedRoutes: Boolean)

  /**
   * The SPA's reserved top-level routes (everything that is NOT a Cortex book) — the Cortex book index now
   * lives at `/`, and chapters at `/{book}/{chapter}`, so book slugs are enumerated separately by the static
   * server (they're filesystem-driven, not compile-time constants). The production server serves `index.html`
   * for each of these segments so a hard reload re-enters the SPA. `Cortex` is the legacy `/cortex/...`
   * prefix: the server serves index.html and the client router redirects `/cortex/x` → `/x` (see ADR-0009).
   */
  val SpaRoutes: List[SpaRoute] = List(
    SpaRoute(Demo, hasNestedRoutes = false),
    SpaRoute(Blogs, hasNestedRoutes = true),
    SpaRoute(Cortex, hasNestedRoutes = true)
  )
