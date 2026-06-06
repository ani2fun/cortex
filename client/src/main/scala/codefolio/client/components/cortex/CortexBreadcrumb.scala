package codefolio.client.components.cortex

import codefolio.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

object CortexBreadcrumb:

  final case class Props(bookSlug: String, bookTitle: String, chapterTitle: String)

  val Component = ScalaFnComponent[Props] { props =>
    <.nav(
      ^.aria.label := "Breadcrumb",
      ^.className  := "cortex-reader-breadcrumb",
      <.ol(
        ^.className := "cortex-reader-breadcrumb__list",
        <.li(<.a(^.className := "cortex-reader-breadcrumb__link", ^.href := "/", "Home")),
        LucideIcons.ChevronRight(LucideIcons.withClass("cortex-reader-breadcrumb__separator")),
        <.li(<.a(^.className := "cortex-reader-breadcrumb__link", ^.href := "/cortex", "Cortex")),
        LucideIcons.ChevronRight(LucideIcons.withClass("cortex-reader-breadcrumb__separator")),
        <.li(
          <.a(
            ^.className := "cortex-reader-breadcrumb__link",
            ^.href      := s"/cortex/${props.bookSlug}",
            props.bookTitle
          )
        ),
        LucideIcons.ChevronRight(LucideIcons.withClass("cortex-reader-breadcrumb__separator")),
        <.li(^.className := "cortex-reader-breadcrumb__current", props.chapterTitle)
      )
    )
  }
