package codefolio.client.components.sections

import codefolio.client.Page
import codefolio.client.components.ToggleMode
import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

/**
 * Sticky top nav with desktop pill menu and mobile drawer.
 *
 * Hash-link anchors (`/#about`, `/#experience`, …) scroll to the matching `id` if the section is already in
 * the DOM. If we're on a non-home route (e.g. `/cortex/...`), we fall back to a real URL navigation —
 * [[codefolio.client.pages.HomePage]]'s `useEffectOnMountBy` reads the URL fragment after mount and scrolls.
 */
object Header:

  final case class Props(ctl: RouterCtl[Page])

  /**
   * One nav-bar entry. Two flavours:
   *
   *   - [[HashLink]] — `/#about`, `/#cortex`, etc. Scrolls to the matching `id` on the home page; falls back
   *     to `location.assign` when triggered from a non-home route.
   *   - [[RouteLink]] — `/blogs`. Real top-level route navigated via `RouterCtl.setRouteEH(page)` so we stay
   *     SPA-internal (no full reload).
   */
  sealed private trait MenuLink:
    def label: String

  final private case class HashLink(hash: String, label: String)              extends MenuLink
  final private case class RouteLink(page: Page, href: String, label: String) extends MenuLink

  private val menuLinks: List[MenuLink] = List(
    HashLink("/#work", "Work"),
    HashLink("/#about", "About"),
    HashLink("/#experience", "Experience"),
    HashLink("/#projects", "Projects"),
    RouteLink(Page.CortexIndex, "/cortex", "Cortex"),
    RouteLink(Page.Blogs, "/blogs", "Blog")
  )

  /**
   * Scroll to the section if its id is in the DOM, otherwise navigate to the home URL with the fragment so
   * HomePage's mount effect handles the scroll.
   *
   * We DON'T use `RouterCtl.set(Page.Home)` here: it computes the canonical URL for `Page.Home` (`/`) and
   * pushes that, wiping the fragment we want. `replaceState` keeps the fragment in the URL for shareability,
   * and `location.assign` is the bulletproof fallback for cross-route nav.
   */
  private def goToAnchor(hash: String): Callback =
    Callback {
      val id     = hash.stripPrefix("/#").stripPrefix("#")
      val target = dom.document.getElementById(id)
      if target != null then
        target.scrollIntoView(true)
        dom.window.history.replaceState(null, "", "#" + id)
      else dom.window.location.assign(hash)
    }

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .render { (props, menuOpenS) =>
        val toggleMenu = menuOpenS.modState(!_)

        def linkClick(hash: String): ReactEventFromInput => Callback =
          (e: ReactEventFromInput) => e.preventDefaultCB >> goToAnchor(hash)

        def routeClick(page: Page): ReactEventFromInput => Callback =
          (e: ReactEventFromInput) => e.preventDefaultCB >> props.ctl.set(page)

        def desktopLink(link: MenuLink): VdomNode = link match
          case HashLink(hash, label) =>
            <.a(
              ^.key       := hash,
              ^.href      := hash,
              ^.className := "header__link",
              ^.onClick ==> linkClick(hash),
              label
            )
          case RouteLink(page, href, label) =>
            <.a(
              ^.key       := href,
              ^.href      := href,
              ^.className := "header__link",
              ^.onClick ==> routeClick(page),
              label
            )

        def mobileLink(link: MenuLink): VdomNode = link match
          case HashLink(hash, label) =>
            <.li(
              ^.key := hash,
              <.a(
                ^.href      := hash,
                ^.className := "header__drawer-link",
                ^.onClick ==> ((e: ReactEventFromInput) =>
                  e.preventDefaultCB >> menuOpenS.setState(false) >> goToAnchor(hash)
                ),
                label
              )
            )
          case RouteLink(page, href, label) =>
            <.li(
              ^.key := href,
              <.a(
                ^.href      := href,
                ^.className := "header__drawer-link",
                ^.onClick ==> ((e: ReactEventFromInput) =>
                  e.preventDefaultCB >> menuOpenS.setState(false) >> props.ctl.set(page)
                ),
                label
              )
            )

        <.header(
          ^.className := "header",
          <.nav(
            ^.className := "header__nav container",
            <.a(
              ^.href      := "/#hero",
              ^.className := "header__brand",
              ^.onClick ==> linkClick("/#hero"),
              <.span(^.className := "header__logomark", "a"),
              <.span(^.className := "header__wordmark", "aniket.kakde")
            ),
            <.div(
              ^.className := "header__menu",
              menuLinks.toTagMod(desktopLink)
            ),
            <.div(
              ^.className := "header__actions",
              <.div(^.className := "header__toggle--mobile", ToggleMode.Component()),
              <.div(^.className := "header__toggle--desktop", ToggleMode.Component()),
              <.a(
                ^.href      := "mailto:a.r.kakde@gmail.com",
                ^.className := "header__cta",
                "Get in touch"
              ),
              <.button(
                ^.className  := "header__burger",
                ^.aria.label := (if menuOpenS.value then "Close menu" else "Open menu"),
                ^.onClick --> toggleMenu,
                if menuOpenS.value then
                  LucideIcons.X(LucideIcons.withClass("header__burger-icon"))
                else LucideIcons.Menu(LucideIcons.withClass("header__burger-icon"))
              )
            )
          ),
          if menuOpenS.value then
            <.nav(
              ^.className := "header__drawer",
              <.ul(
                ^.className := "header__drawer-list",
                menuLinks.toTagMod(mobileLink)
              )
            )
          else EmptyVdom
        )
      }
