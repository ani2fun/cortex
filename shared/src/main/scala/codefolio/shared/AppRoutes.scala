package codefolio.shared

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
   * One top-level route the SPA owns. `segment` is the first path segment (`cortex`, `blogs`, `demo`);
   * `hasNestedRoutes` is true when the SPA also handles deeper paths under it (`/cortex/{book}/{chapter}`,
   * `/blogs/{slug}`).
   */
  final case class SpaRoute(segment: String, hasNestedRoutes: Boolean)

  /**
   * The SPA's top-level routes, excluding `/` (which is `index.html` itself). Single source of truth: the
   * client `Router` uses these segments, and the production server derives its `index.html` fallback list
   * from this list — a hard reload of any SPA path must return `index.html` so the client router can
   * re-resolve. Add an SPA route here and the server picks it up automatically; there is no separate
   * server-side mirror to keep in sync. See ADR-0009.
   */
  val SpaRoutes: List[SpaRoute] = List(
    SpaRoute(Demo, hasNestedRoutes = false),
    SpaRoute(Cortex, hasNestedRoutes = true),
    SpaRoute(Blogs, hasNestedRoutes = true)
  )
