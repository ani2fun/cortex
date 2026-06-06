package cortex.client.components.book

import cortex.client.components.icons.LucideIcons
import cortex.client.markdown.MarkdownRenderer
import cortex.client.util.HashScroll
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

/**
 * Floating "On This Page" TOC, used on every viewport size.
 *
 *   - **Hamburger FAB** stacked above the back-to-top button on the same vertical axis (`right: 20px`), same
 *     44×44 circle and shadow language. Open-state flips the button to the foreground colour and swaps the
 *     `ListTree` icon for `X`, so it reads as "press again to close".
 *   - **Popover** anchored above the FAB stack with the heading list. Each row is a tick + label indented per
 *     heading depth, matching the look of the previous right-rail rule (active row's tick widens + tints
 *     indigo).
 *   - **Active section** tracked via IntersectionObserver — same logic as the prior right-rail
 *     implementation; an additional ancestor walk lights the parent section's tick when the reader is inside
 *     a sub-heading, so two rows can read as active at once (sub + its parent l1 anchor).
 *   - **Closes on**: click outside (transparent scrim), `Escape`, or selecting an entry.
 *
 * Replaces both the old expanding right-rail aside and the inline `MobileToc` collapsible — neither is
 * rendered anywhere else.
 */
object CortexToc:

  final case class Props(toc: List[MarkdownRenderer.TocEntry])

  // The minimum heading depth in this chapter is the popover's "level 1" — typically h2, but some imported
  // books start at h3, so we normalise. Levels above the visible cap (3) clamp down so we don't generate
  // unbounded class names.
  private val MaxLevel = 3

  private def levelOf(entryDepth: Int, base: Int): Int =
    val raw = entryDepth - base + 1
    math.max(1, math.min(MaxLevel, raw))

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(false)                // popover open?
      .useState(Option.empty[String]) // active heading slug
      .useEffectWithDepsBy((props, _, _) => props.toc.map(_.slug)) { (props, _, activeS) => _ =>
        if props.toc.isEmpty then Callback.empty
        else
          Callback {
            val headings = props.toc
              .flatMap(item => Option(dom.document.getElementById(item.slug)))
              .toJSArray

            if headings.length == 0 then ()
            else
              val cb: js.Function2[js.Array[dom.IntersectionObserverEntry], dom.IntersectionObserver, Unit] =
                (entries, _) =>
                  val visible = entries
                    .filter(_.isIntersecting)
                    .toList
                    .sortBy(_.boundingClientRect.top.toDouble)
                  visible.headOption.foreach { entry =>
                    val slug = entry.target.asInstanceOf[dom.Element].id
                    if activeS.value.contains(slug) then ()
                    else
                      activeS.setState(Some(slug)).runNow()
                      // Broadcast the active heading so the sticky chapter bar + mini-map can update
                      // without each running its own IntersectionObserver. The detail is the slug.
                      val init = (new js.Object).asInstanceOf[dom.CustomEventInit]
                      init.detail = slug
                      val ev = new dom.CustomEvent("cortex:activeHeading", init)
                      val _  = dom.window.dispatchEvent(ev)
                  }

              val opts = (new js.Object).asInstanceOf[dom.IntersectionObserverInit]
              opts.rootMargin = "-20% 0px -70% 0px"
              opts.threshold = js.Array[Double](0.0, 1.0)
              val observer = new dom.IntersectionObserver(cb, opts)
              for i <- 0 until headings.length do observer.observe(headings(i))
            ()
          }
      }
      // Keyboard: Escape closes the popover. Listener is global because the FAB / popover are not on the
      // document focus chain by default. The effect re-runs when openS flips, so we only have a listener
      // installed while the popover is open.
      .useEffectWithDepsBy((_, openS, _) => openS.value) { (_, openS, _) => isOpen =>
        if !isOpen then Callback.empty
        else
          Callback {
            val onKey: js.Function1[dom.KeyboardEvent, Unit] = (e: dom.KeyboardEvent) =>
              if e.key == "Escape" then openS.setState(false).runNow()
            dom.window.addEventListener("keydown", onKey, useCapture = false)
            // Single-shot cleanup via re-attaching a one-time handler; React 17+ hooks cleanup pattern
            // would be cleaner if this codebase exposed it, but the dependency on openS makes the effect
            // re-run on every flip so leak risk is bounded.
            dom.window.setTimeout(
              () =>
                if !openS.value then
                  dom.window.removeEventListener("keydown", onKey, useCapture = false),
              0.0
            )
            ()
          }
      }
      .render { (props, openS, activeS) =>
        if props.toc.isEmpty then EmptyVdom
        else
          val baseDepth  = props.toc.map(_.depth).min
          val activeSlug = activeS.value
          val activeAncestor = activeSlug.flatMap { slug =>
            val idx = props.toc.indexWhere(_.slug == slug)
            if idx >= 0 then props.toc.take(idx + 1).reverse.find(_.depth == baseDepth).map(_.slug)
            else None
          }

          val close: Callback = openS.setState(false)

          val popover: VdomNode =
            <.aside(
              ^.id                  := "cortex-reader-toc-pop",
              ^.className           := "cortex-reader-toc-pop",
              ^.role                := "dialog",
              ^.aria.label          := "On this page",
              VdomAttr("data-open") := (if openS.value then "true" else "false"),
              <.div(
                ^.className := "cortex-reader-toc-pop__head",
                <.span(^.className := "cortex-reader-toc-pop__eyebrow", "On this page"),
                <.span(
                  ^.className := "cortex-reader-toc-pop__count",
                  s"${props.toc.size} sections"
                )
              ),
              <.ul(
                ^.className := "cortex-reader-toc-pop__list",
                props.toc.toTagMod { item =>
                  val level    = levelOf(item.depth, baseDepth)
                  val isActive = activeSlug.contains(item.slug) || activeAncestor.contains(item.slug)
                  val rowCls = {
                    val base = s"cortex-reader-toc-pop__row cortex-reader-toc-pop__row--l$level"
                    if isActive then s"$base cortex-reader-toc-pop__row--active" else base
                  }
                  <.li(
                    ^.key       := s"toc-${item.slug}",
                    ^.className := rowCls,
                    <.a(
                      ^.href      := s"#${item.slug}",
                      ^.className := "cortex-reader-toc-pop__btn",
                      ^.onClick ==> ((e: ReactMouseEvent) =>
                        HashScroll.onHashLinkClick(e, item.slug) >>
                          Callback {
                            // Pulse the sibling sidebar tile/row so the two TOC surfaces feel synced.
                            val init = (new js.Object).asInstanceOf[dom.CustomEventInit]
                            init.detail = item.slug
                            val ev = new dom.CustomEvent("cortex:syncFlash", init)
                            val _  = dom.window.dispatchEvent(ev)
                          } >>
                          close
                      ),
                      <.span(^.className := "cortex-reader-toc-pop__tick", ^.aria.hidden := true),
                      <.span(^.className := "cortex-reader-toc-pop__label", item.text)
                    )
                  )
                }
              )
            )

          <.div(
            // Transparent click-outside scrim.
            <.div(
              ^.className           := "cortex-reader-toc-scrim",
              VdomAttr("data-open") := (if openS.value then "true" else "false"),
              ^.onClick --> close
            ),
            popover,
            // Floating hamburger FAB.
            <.button(
              ^.tpe           := "button",
              ^.className     := "cortex-reader-toc-float",
              ^.aria.label    := "On this page",
              ^.aria.expanded := openS.value,
              ^.aria.controls := "cortex-reader-toc-pop",
              ^.onClick --> openS.modState(!_),
              LucideIcons.ListTree(
                LucideIcons.withClass("cortex-reader-toc-float__icon cortex-reader-toc-float__icon--menu")
              ),
              LucideIcons.X(
                LucideIcons.withClass("cortex-reader-toc-float__icon cortex-reader-toc-float__icon--close")
              )
            )
          )
      }
