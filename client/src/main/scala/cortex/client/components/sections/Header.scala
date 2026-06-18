package cortex.client.components.sections

import cortex.client.Page
import cortex.client.auth.AccountMenu
import cortex.client.components.ToggleMode
import cortex.client.components.search.LibrarySearch
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Sticky editorial top nav for Cortex — deliberately quiet. The indigo logo chip + lowercase `cortex`
 * wordmark on the left (the brand returns to the library home), a centered **⌘K library search** affordance,
 * and on the right a theme toggle + the account control. The old Library / Blog links and the GitHub pill are
 * gone: the brand already routes home, the search is a faster path to everything, and GitHub now lives inside
 * the account menu. See the "Nav redesign" plan.
 */
object Header:

  final case class Props(ctl: RouterCtl[Page])

  val Component =
    ScalaFnComponent[Props] { props =>
      def routeClick(page: Page): ReactEventFromInput => Callback =
        (e: ReactEventFromInput) => e.preventDefaultCB >> props.ctl.set(page)

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
            ^.className := "header__mid",
            LibrarySearch.Trigger()
          ),
          <.div(
            ^.className := "header__actions",
            ToggleMode.Component(),
            AccountMenu.Component(AccountMenu.Props(props.ctl))
          )
        )
      )
    }
