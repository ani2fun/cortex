package cortex.client.components.book.workbench

import cortex.client.components.book.BlockMounter
import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * The full problem workbench — left pane with Description / Editorial / Coach tabs, a draggable vertical
 * splitter, and the stacked [[WorkbenchEditor]] on the right. Hosts both the problem PAGE (ChapterPage's
 * `kind: problem` branch, which passes title/difficulty/topics from the frontmatter) and the Your-Turn MODAL
 * (which passes the packaged statement as the description and the editorial as a single section).
 *
 * **Panel mounting.** The Description and Editorial bodies are packaged HTML (from `<template data-wb=…>`)
 * that may contain nested placeholders — d3 widgets, solution viewers, quizzes, mermaid. Each panel is
 * injected + discovered via [[BlockMounter.mountInto]] **lazily on first reveal** (active tab / opened
 * accordion section), so widgets never measure a `display:none` box; once mounted, panels toggle `hidden` and
 * keep their component state (the RunnableCodeGroup precedent).
 */
object ProblemWorkbench:

  final case class Props(
      title: String,
      difficulty: Option[String],
      topics: List[String],
      descriptionHtml: String,
      sections: List[Block.EdSection],
      tabs: List[Block.Tab],
      spec: Block.TestSpec,
      /** CSS height of the whole panel — viewport-bound on the page, `100%` inside the modal. */
      heightCss: String,
      /** Present on problem PAGES only — enables the editor's Submit button (Your-Turn modals omit it). */
      submitCtx: Option[WorkbenchEditor.SubmitContext] = None
  )

  object Props:

    /** Generic-dispatch fallback (a placeholder met outside ChapterPage's problem branch): no page header. */
    def fromBlock(pw: Block.ProblemWorkbench): Props =
      Props(
        title = "",
        difficulty = None,
        topics = Nil,
        descriptionHtml = pw.descriptionHtml,
        sections = pw.sections,
        tabs = pw.tabs,
        spec = pw.spec,
        heightCss = "calc(100vh - 12rem)"
      )

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(44.0)   // left pane width (%)
      .useState(0)      // active left tab: 0 desc · 1 editorial · 2 coach
      .useState(Set(0)) // open editorial sections (Intuition open by default)
      .useRefToVdom[dom.html.Element]
      .useRefBy(_ => js.Array[BlockMounter.RootHandle]())
      .useRefBy(_ => Set.empty[String]) // panel keys already injected+mounted
      // Lazily inject + mount panels when they first become visible. String dep = visible-key set.
      .useEffectWithDepsBy { (_, _, tabS, openS, _, _, _) =>
        s"${tabS.value}|${openS.value.toList.sorted.mkString(",")}"
      } { (props, _, tabS, openS, rootRef, rootsRef, mountedRef) => _ =>
        rootRef.foreach { root =>
          val keys =
            if tabS.value == 0 then List("desc")
            else if tabS.value == 1 then openS.value.toList.sorted.map(i => s"sec-$i")
            else Nil
          keys.foreach { key =>
            if !mountedRef.value.contains(key) then
              val el = root.querySelector(s"[data-wb-panel='$key']")
              if el != null then
                val html = key match
                  case "desc" => props.descriptionHtml
                  case k =>
                    props.sections.lift(k.stripPrefix("sec-").toInt).map(_.html).getOrElse("")
                BlockMounter
                  .mountInto(el.asInstanceOf[dom.html.Element], html)
                  .foreach { r =>
                    val _ = rootsRef.value.push(r)
                  }
                mountedRef.value = mountedRef.value + key
          }
        }
      }
      // Tear the nested roots down when the workbench itself unmounts (modal close, page change).
      .useEffectOnMountBy { (_, _, _, _, _, rootsRef, _) =>
        CallbackTo {
          Callback {
            BlockMounter.teardown(rootsRef.value)
            rootsRef.value = js.Array()
          }
        }
      }
      .render { (props, leftPctS, tabS, openS, rootRef, _, _) =>
        def startSplitDrag(e: ReactMouseEvent): Callback = Callback {
          e.preventDefault()
          dom.document.body.style.cursor = "col-resize"
          dom.document.body.style.setProperty("user-select", "none")
          val move: js.Function1[dom.MouseEvent, Unit] = ev =>
            rootRef.get.runNow().foreach { root =>
              val rect = root.getBoundingClientRect()
              if rect.width > 0 then
                val pct = (ev.clientX - rect.left) / rect.width * 100.0
                leftPctS.setState(math.min(64.0, math.max(28.0, pct))).runNow()
            }
          lazy val up: js.Function1[dom.MouseEvent, Unit] = _ => {
            dom.document.removeEventListener("mousemove", move)
            dom.document.removeEventListener("mouseup", up)
            dom.document.body.style.cursor = ""
            val _ = dom.document.body.style.removeProperty("user-select")
          }
          dom.document.addEventListener("mousemove", move)
          dom.document.addEventListener("mouseup", up)
        }

        // Submissions is a problem-PAGE tab only (it needs the book/chapter to fetch); the Your-Turn
        // modal (submitCtx = None) shows just Description / Editorial / Coach.
        val tabDefs: Vector[(String, LucideIcons.IconProps => VdomNode)] =
          val base = Vector[(String, LucideIcons.IconProps => VdomNode)](
            ("Description", p => LucideIcons.BookOpen(p)),
            ("Editorial", p => LucideIcons.Lightbulb(p)),
            ("Coach", p => LucideIcons.Target(p))
          )
          if props.submitCtx.isDefined then base :+ ("Submissions", p => LucideIcons.Clock(p))
          else base

        val tabStrip =
          <.div(
            ^.className := "pwb__tabs",
            tabDefs.zipWithIndex.toVdomArray { case ((name, icon), i) =>
              val on = tabS.value == i
              <.button(
                ^.key       := s"tab-$i",
                ^.tpe       := "button",
                ^.className := (if on then "pwb__tab pwb__tab--active" else "pwb__tab"),
                ^.onClick --> tabS.setState(i),
                icon(LucideIcons.withClass("pwb__tab-icon")),
                name
              )
            }
          )

        val titleRow: VdomNode =
          if props.title.isEmpty then EmptyVdom
          else
            <.div(
              ^.className := "pwb__titlerow",
              <.div(
                ^.className := "pwb__titleline",
                <.h1(^.className := "pwb__title", props.title),
                props.difficulty.map { d =>
                  <.span(
                    ^.className := s"pwb__badge pwb__badge--${d.toLowerCase}",
                    d.take(1).toUpperCase + d.drop(1).toLowerCase
                  ): VdomNode
                }.getOrElse(EmptyVdom)
              ),
              if props.topics.nonEmpty then
                <.div(
                  ^.className := "pwb__topics",
                  props.topics.toVdomArray(t =>
                    <.span(^.key := s"topic-$t", ^.className := "pwb__topic-pill", t)
                  )
                )
              else EmptyVdom
            )

        val descPanel =
          <.div(
            ^.className := "pwb__panel",
            ^.hidden    := tabS.value != 0,
            titleRow,
            <.div(
              ^.className               := "pwb__html chapter-content",
              VdomAttr("data-wb-panel") := "desc"
            )
          )

        val editorialPanel =
          <.div(
            ^.className := "pwb__panel",
            ^.hidden    := tabS.value != 1,
            if props.sections.isEmpty then
              <.p(
                ^.className := "pwb__empty",
                "No editorial yet — solve it first, then check back."
              ): VdomNode
            else
              <.div(
                ^.className := "pwb__accordion",
                props.sections.zipWithIndex.toVdomArray { case (sec, i) =>
                  val open = openS.value.contains(i)
                  <.div(
                    ^.key       := s"sec-wrap-$i",
                    ^.className := (if open then "ed-acc ed-acc--open" else "ed-acc"),
                    <.button(
                      ^.tpe       := "button",
                      ^.className := "ed-acc__head",
                      ^.onClick --> openS.modState(s => if s.contains(i) then s - i else s + i),
                      LucideIcons.ChevronRight(
                        LucideIcons.withClass(
                          if open then "ed-acc__chevron ed-acc__chevron--open" else "ed-acc__chevron"
                        )
                      ),
                      <.span(^.className := "ed-acc__title", sec.title)
                    ),
                    <.div(
                      ^.className := "ed-acc__body",
                      ^.hidden    := !open,
                      <.div(
                        ^.className               := "pwb__html chapter-content",
                        VdomAttr("data-wb-panel") := s"sec-$i"
                      )
                    )
                  )
                }
              )
          )

        val coachPanel =
          <.div(
            ^.className := "pwb__panel pwb__panel--coach",
            ^.hidden    := tabS.value != 2,
            CoachTab.Component()
          )

        val submissionsPanel: VdomNode =
          props.submitCtx match
            case None => EmptyVdom
            case Some(ctx) =>
              <.div(
                ^.className := "pwb__panel pwb__panel--subs",
                ^.hidden    := tabS.value != 3,
                SubmissionsTab.Component(SubmissionsTab.Props(ctx.book, ctx.chapter))
              )

        <.div.withRef(rootRef)(
          ^.className := "pwb",
          ^.style     := js.Dynamic.literal(height = props.heightCss).asInstanceOf[js.Object],
          <.div(
            ^.className := "pwb__left",
            ^.style     := js.Dynamic.literal(width = s"${leftPctS.value}%").asInstanceOf[js.Object],
            tabStrip,
            <.div(^.className := "pwb__panels", descPanel, editorialPanel, coachPanel, submissionsPanel)
          ),
          <.div(
            ^.className := "wb-split wb-split--v",
            ^.onMouseDown ==> startSplitDrag,
            <.div(^.className := "wb-split__line"),
            <.div(
              ^.className := "wb-split__grip",
              <.span(^.className := "wb-split__dot"),
              <.span(^.className := "wb-split__dot"),
              <.span(^.className := "wb-split__dot")
            )
          ),
          <.div(
            ^.className := "pwb__right",
            WorkbenchEditor.Component(
              WorkbenchEditor.Props(
                tabs = props.tabs,
                spec = props.spec,
                fixedHeightPx = None,
                pctClamp = (30.0, 78.0),
                defaultPct = 58.0,
                submitCtx = props.submitCtx
              )
            )
          )
        )
      }
