package cortex.client.components.sections

import cortex.client.Page
import cortex.client.components.ToggleMode
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Sticky editorial top nav for Cortex — the design system's chrome: the indigo logo chip + lowercase `cortex`
 * wordmark, the Library / Blog routes, a theme toggle, and a GitHub link. SPA-internal links navigate via
 * `RouterCtl` (no full reload); the brand returns to the library home.
 */
object Header:

  final case class Props(ctl: RouterCtl[Page])

  final private case class NavLink(page: Page, href: String, label: String)

  private val navLinks: List[NavLink] = List(
    NavLink(Page.CortexIndex, "/", "Library"),
    NavLink(Page.Blogs, "/blogs", "Blog")
  )

  private val GithubUrl = "https://github.com/ani2fun/cortex"

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)
      .render { (props, menuOpenS) =>
        val toggleMenu = menuOpenS.modState(!_)

        def routeClick(page: Page): ReactEventFromInput => Callback =
          (e: ReactEventFromInput) => e.preventDefaultCB >> props.ctl.set(page)

        def desktopLink(link: NavLink): VdomNode =
          <.a(
            ^.key       := link.label,
            ^.href      := link.href,
            ^.className := "header__link",
            ^.onClick ==> routeClick(link.page),
            link.label
          )

        def mobileLink(link: NavLink): VdomNode =
          <.li(
            ^.key := link.label,
            <.a(
              ^.href      := link.href,
              ^.className := "header__drawer-link",
              ^.onClick ==> ((e: ReactEventFromInput) =>
                e.preventDefaultCB >> menuOpenS.setState(false) >> props.ctl.set(link.page)
              ),
              link.label
            )
          )

        <.header(
          ^.className := "header",
          <.nav(
            ^.className := "header__nav container",
            <.a(
              ^.href      := "/",
              ^.className := "header__brand",
              ^.onClick ==> routeClick(Page.CortexIndex),
              <.img(
                ^.src       := "/img/cortex/cortex-chip.svg",
                ^.width     := "28",
                ^.height    := "28",
                ^.alt       := "Cortex",
                ^.className := "header__chip"
              ),
              <.span(^.className := "header__wordmark", "cortex")
            ),
            <.div(
              ^.className := "header__menu",
              navLinks.toTagMod(desktopLink)
            ),
            <.div(
              ^.className := "header__actions",
              <.div(^.className := "header__toggle--mobile", ToggleMode.Component()),
              <.div(^.className := "header__toggle--desktop", ToggleMode.Component()),
              <.a(
                ^.href      := GithubUrl,
                ^.target    := "_blank",
                ^.rel       := "noopener noreferrer",
                ^.className := "header__cta",
                "GitHub"
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
                navLinks.toTagMod(mobileLink)
              )
            )
          else EmptyVdom
        )
      }
