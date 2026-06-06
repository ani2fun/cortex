package cortex.client.pages

import cortex.client.api.ApiClient
import cortex.client.components.book.{
  ChapterContent,
  CortexBreadcrumb,
  CortexErrorView,
  CortexPrevNext,
  CortexReadMeta,
  CortexReaderLayout
}
import cortex.client.markdown.MarkdownRenderer
import cortex.client.util.{AsyncFetch, PageTitle, ReaderState}
import cortex.shared.api.Endpoints.{ChapterPayload, ChapterRef}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

/**
 * Reads `/api/cortex/{book}/{chapter}`, runs the markdown through the client-side pipeline, and lays out the
 * result with sidebar + breadcrumb + TOC + pager.
 *
 * Side-effects beyond rendering: caches the chapter's word-count-derived "minutes to read" in localStorage on
 * every successful load, and listens to the window's `scroll` event while mounted to persist the user's
 * reading-progress percent (both keyed by chapter slug). The sidebar reads those values back to render the
 * progress rail and the `Xm` time-to-read stamp.
 */
object ChapterPage:

  final case class Props(book: String, chapter: String)

  /**
   * Combined fetch + render bundle. We keep both stages so the UI shows loading until both the API call and
   * the markdown renderer have finished.
   */
  final private case class Loaded(payload: ChapterPayload, render: MarkdownRenderer.Result)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(AsyncFetch.initial[Loaded])
      // Cleanup ref for the per-chapter scroll listener. Same pattern as D2Diagram: on the next
      // effect run we drain this array (removing the listener and saving a final progress sample),
      // then push a fresh cleanup as the new listener is installed.
      .useRefBy((_, _) => js.Array[js.Function0[Unit]]())
      .useEffectWithDepsBy((props, _, _) => (props.book, props.chapter)) { (_, state, _) => deps =>
        val (_, chapter) = deps
        AsyncFetch.run(
          setState = state.setState,
          fetch =
            for
              payload  <- ApiClient.getCortexChapter(deps._1, chapter)
              rendered <- MarkdownRenderer.render(payload.raw)
            yield Loaded(payload, rendered),
          errorPrefix = s"Failed to load ${deps._1}/${chapter}",
          onLoaded = loaded =>
            PageTitle.set(s"${loaded.payload.frontmatter.title} — ${loaded.payload.book.title}") >>
              Callback(ReaderState.saveMinutesFromRaw(loaded.payload.chapter.slug, loaded.payload.raw))
        )
      }
      // Persist scroll-progress for the active chapter while the user reads. We throttle via a 200ms
      // setTimeout debounce so we're not hammering localStorage on every wheel tick. The previous
      // chapter's listener is torn down (and a final progress sample written) on chapter change.
      .useEffectWithDepsBy((p, _, _) => (p.book, p.chapter)) { (_, _, cleanupRef) => deps =>
        val (_, chapter) = deps

        val tearDown: Callback = Callback {
          val arr = cleanupRef.value
          for i <- 0 until arr.length do arr(i)()
          cleanupRef.value = js.Array()
        }

        val install: Callback = Callback {
          var pending: Option[Int] = None
          val onScroll: js.Function1[dom.Event, Unit] = (_: dom.Event) => {
            pending.foreach(dom.window.clearTimeout)
            pending = Some(dom.window.setTimeout(
              () => ReaderState.saveScrollProgress(chapter),
              200.0
            ))
          }
          dom.window.addEventListener("scroll", onScroll, useCapture = false)
          cleanupRef.value.push { () =>
            dom.window.removeEventListener("scroll", onScroll, useCapture = false)
            // One last snapshot so the sidebar shows where the user actually left, not the last
            // debounced sample (which may be ~200ms stale).
            ReaderState.saveScrollProgress(chapter)
            pending.foreach(dom.window.clearTimeout)
            ()
          }
          ()
        }

        tearDown >> install
      }
      .render { (props, state, _) =>
        state.value.render(
          loaded = renderLoaded,
          loading = <.main(
            ^.className := "container mx-auto py-24 text-center text-muted-foreground",
            s"Loading ${props.book}/${props.chapter}…"
          ),
          errored = msg => <.main(^.className := "container mx-auto py-16 text-center", CortexErrorView(msg))
        )
      }

  private def renderLoaded(loaded: Loaded): VdomNode =
    val payload = loaded.payload
    val toc     = loaded.render.toc

    val prev: Option[ChapterRef] =
      payload.prevSlug.flatMap(s => payload.book.chapters.find(_.slug == s))
    val next: Option[ChapterRef] =
      payload.nextSlug.flatMap(s => payload.book.chapters.find(_.slug == s))

    val content: VdomNode =
      <.div(
        ^.className := "cortex-reader-layout__prose",
        CortexBreadcrumb.Component(
          CortexBreadcrumb.Props(
            bookSlug = payload.book.slug,
            bookTitle = payload.book.title,
            chapterTitle = payload.frontmatter.title
          )
        ),
        <.h1(
          ^.id        := "chapter-top",
          ^.className := "cortex-reader-prose__title",
          payload.frontmatter.title
        ),
        CortexReadMeta.Component(CortexReadMeta.Props(rawMarkdown = payload.raw)),
        payload.frontmatter.summary
          .map(s =>
            <.p(
              ^.className := "cortex-reader-prose__lede",
              s
            ): VdomNode
          )
          .getOrElse(EmptyVdom),
        ChapterContent.Component(ChapterContent.Props(loaded.render)),
        CortexPrevNext.Component(
          CortexPrevNext.Props(bookSlug = payload.book.slug, prev = prev, next = next)
        )
      )

    <.div(
      ^.className := "pt-20",
      CortexReaderLayout.Component(
        CortexReaderLayout.Props(
          book = payload.book,
          activeChapterSlug = payload.chapter.slug,
          toc = toc,
          content = content,
          chapterTitle = payload.frontmatter.title
        )
      )
    )
