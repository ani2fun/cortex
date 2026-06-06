package cortex.client

import cortex.client.pages.*
import cortex.shared.AppRoutes
import japgolly.scalajs.react.extra.router.*

/**
 * scalajs-react Router using HTML5 history (clean URLs).
 *
 * Routes are declared bottom-up: each `staticRoute` / `dynamicRouteCT` line maps a **URL pattern** to a
 * `Page` ADT case to a **component to render**. The `Page` cases come from `client/Page.scala` (a sealed
 * trait — adding a new route means adding a case there too).
 *
 * URL topology (this is cortex.kakde.eu — there is no `/cortex` prefix):
 *   - `root` → "/" → the Cortex book index
 *   - `/{book}/{chapter}` → the chapter reader
 *   - `/{book}` → redirect to the book's first chapter
 *   - `/blogs`, `/blogs/{slug}`, `/demo` → reserved routes (declared before the generic book routes)
 *   - legacy `/cortex/<rest>` → redirected to `/<rest>` (see the `rewritePath` rule)
 *
 * Rule ORDER matters: scalajs-react matches top-to-bottom, and the generic `/{book}` and `/{book}/{chapter}`
 * routes would swallow `/blogs` etc., so all reserved/static routes are declared first.
 *
 * The production server's index.html fallback covers `/`, the reserved segments in `shared.AppRoutes.SpaRoutes`,
 * and each book slug (enumerated from the content tree) — see ADR-0009 and `server.http.StaticRoutes`.
 */
object Router:

  private def stripFragment(path: Path): Path =
    Path(path.value.takeWhile(_ != '#'))

  private val config: RouterConfig[Page] = RouterConfigDsl[Page].buildConfig { dsl =>
    import dsl.*

    // Path matchers — `[^/?#]+` consumes a single path segment, stopping at
    // the first `/`, `?`, or `#`. The `#` exclusion matters: without it, an
    // in-page anchor click like `<a href="#section">` makes the router
    // capture `chapter#section` as the chapter slug, which then 404s on the
    // backend (slug regex rejects `#`). Same for query-string segments.
    val seg = string("[^/?#]+")

    // The | operator composes route rules. Each rule is "pattern ~> render".
    // `trimSlashes` normalises trailing/duplicate slashes before matching.
    val rules =
      trimSlashes
        // `/` is the Cortex book index (this is cortex.kakde.eu; there is no /cortex prefix).
        | staticRoute(root, Page.CortexIndex) ~> render(CortexIndexPage.Component())
        // Reserved top-level routes are declared BEFORE the generic one/two-segment book routes
        // below: scalajs-react matches in declaration order, and `/{book}` / `/{book}/{chapter}`
        // would otherwise swallow `/blogs`, `/blogs/{slug}`, `/demo`.
        | staticRoute(AppRoutes.Blogs, Page.Blogs) ~> render(BlogIndexPage.Component())
        | dynamicRouteCT((AppRoutes.Blogs / seg).caseClass[Page.BlogPost]) ~>
        dynRender((p: Page.BlogPost) => BlogPostPage.Component(BlogPostPage.Props(p.slug)))
        | staticRoute(AppRoutes.Demo, Page.Demo) ~> render(DemoPage.Component())
        // Legacy compatibility: `/cortex/<rest>` (the monorepo's old prefix) → `/<rest>`. The server's
        // ChapterAssetRewrite already strips this from in-content links; this catches external/bookmarked
        // URLs. Must precede the generic book routes so `/cortex/x` isn't read as book "cortex"; `/cortex`
        // alone redirects to the index `/`.
        | rewritePathR(
          "^/?cortex(?:/(.*))?$".r,
          m => Some(redirectToPath(Path(Option(m.group(1)).getOrElse("")))(SetRouteVia.HistoryReplace))
        )
        // /{book}/{chapter} — the chapter reader.
        | dynamicRouteCT((seg / seg).caseClass[Page.Chapter]) ~>
        dynRender((p: Page.Chapter) => ChapterPage.Component(ChapterPage.Props(p.book, p.chapter)))
        // /{book} — redirect to the book's first chapter (BookRedirectPage navigates on mount).
        | dynamicRouteCT(seg.caseClass[Page.BookRedirect]) ~>
        dynRender((p: Page.BookRedirect) => BookRedirectPage.Component(p.book))

    rules
      // The router library parses from `window.location.href`, so the path it
      // sees includes any in-page fragment. Route matching should ignore the
      // fragment while leaving the address bar untouched for section scrolling.
      .modPath(identity, path => Some(stripFragment(path)))
      // Unknown path → the Cortex index (replace, not push, so the bad URL doesn't
      // pollute the back-button history).
      .notFound(redirectToPage(Page.CortexIndex)(SetRouteVia.HistoryReplace))
      // Layout wraps every rendered page with Header + outlet + Footer.
      // The `ctl` (router controller) is passed down so links can use
      // `ctl.setRouteEH(Page.X)` instead of raw <a href>.
      .renderWith((ctl, res) => Layout.Component(Layout.Props(ctl, res)))
  }

  /**
   * A `Router` component already mounted at the page's origin, ready to drop into
   * `ReactDOMClient.createRoot(...).render(...)`. Rendered exactly once by [[Main]].
   */
  val Component = japgolly.scalajs.react.extra.router.Router(BaseUrl.fromWindowOrigin_/, config)
