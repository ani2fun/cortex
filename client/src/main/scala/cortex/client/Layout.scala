package cortex.client

import cortex.client.components.sections.{Footer, Header}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.extra.router.{Resolution, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * App-shell layout: Header at top, the resolved page in the middle, Footer at the bottom. The Header receives
 * the RouterCtl so its nav links navigate via pushState rather than full reloads.
 */
object Layout:

  final case class Props(ctl: RouterCtl[Page], resolution: Resolution[Page])

  val Component = ScalaFnComponent[Props] { props =>
    <.div(
      ^.className := "min-h-screen flex flex-col bg-background text-foreground",
      Header.Component(Header.Props(props.ctl)),
      <.div(^.className := "flex-1", props.resolution.render()),
      Footer.Component()
    )
  }
