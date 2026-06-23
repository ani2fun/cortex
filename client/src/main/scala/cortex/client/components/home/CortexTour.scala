package cortex.client.components.home

import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.util.Try

/**
 * "Everything Cortex can do" — the guided-tour slideshow on the landing page (`/`).
 *
 * A five-panel cross-fade carousel (library → runnable code → reader → Coach → habits). Faithfully ported
 * from the `Cortex Homepage Slideshow` design: the panels are stacked absolutely and the active one fades in
 * via the `.is-active` class (opacity/z-index live in `sections/cortex-tour.css`, not inline). Auto-advances
 * every 6s, pauses on hover, restores the last-viewed panel from `localStorage`, and honours
 * `prefers-reduced-motion`. The timer wiring mirrors [[cortex.client.components.book.D2Slideshow]].
 */
object CortexTour:

  private val SlideCount = 5
  private val IntervalMs = 6000.0
  private val StorageKey = "cortex.tour.index"

  private val Labels =
    List("The library", "Runnable code", "Find your way", "The Coach", "Make it a habit")

  // ── Persistence + motion preference (all guarded — storage/matchMedia can throw) ──────────────
  private def reducedMotion: Boolean =
    Try(dom.window.matchMedia("(prefers-reduced-motion: reduce)").matches).getOrElse(false)

  private def loadIndex(): Int =
    Try(Option(dom.window.localStorage.getItem(StorageKey))).toOption.flatten
      .flatMap(s => Try(s.toInt).toOption)
      .filter(i => i >= 0 && i < SlideCount)
      .getOrElse(0)

  private def saveIndex(i: Int): Unit =
    val _ = Try(dom.window.localStorage.setItem(StorageKey, i.toString))

  // ── Small building blocks ─────────────────────────────────────────────────────────────────────
  private def slideClass(active: Int, i: Int, extra: String): String =
    s"cx-tour__slide $extra" + (if i == active then " is-active" else "")

  private def bullet(text: String): VdomNode =
    <.div(
      ^.className := "cx-tour__bullet",
      <.span(^.className := "cx-tour__bullet-dot", ^.aria.hidden := true),
      text
    )

  // Each card links to `/{slug}` so a click opens that book — same route the real library cards use.
  private def bookCard(variant: String, slug: String, title: String, meta: String): VdomNode =
    <.a(
      ^.href      := s"/$slug",
      ^.className := "cx-tour__book",
      <.div(
        ^.className               := s"cx-tour__book-cover cx-tour__book-cover--$variant",
        ^.aria.hidden             := true,
        ^.dangerouslySetInnerHtml := coverArt(variant)
      ),
      <.div(^.className := "cx-tour__book-title", title),
      <.div(^.className := "cx-tour__book-meta", meta)
    )

  /**
   * Cover art for the book cards — an original line-art motif per topic in translucent cream, so each
   * gradient reads as a designed cover rather than a flat colour: a node-ring (System Design), a binary tree
   * (DSA), a spark (The Claude Stack), and code brackets (Programming Languages). Injected as raw SVG the
   * same way [[cortex.client.components.book.D2Diagram]] mounts its pre-rendered diagrams.
   */
  private def coverArt(variant: String): String = variant match
    case "sd" => // System Design — a connected node-ring (distributed system)
      """<svg viewBox="0 0 100 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet"><g stroke="rgba(246,241,232,0.32)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M50 11 L71 26 L63 51 L37 51 L29 26 Z"/><line x1="50" y1="11" x2="63" y2="51"/><line x1="71" y1="26" x2="37" y2="51"/></g><g fill="rgba(246,241,232,0.45)"><circle cx="71" cy="26" r="5"/><circle cx="63" cy="51" r="5"/><circle cx="37" cy="51" r="5"/><circle cx="29" cy="26" r="5"/></g><circle cx="50" cy="11" r="6.5" fill="rgba(246,241,232,0.78)"/></svg>"""
    case "dsa" => // Data Structures & Algorithms — a binary tree
      """<svg viewBox="0 0 100 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet"><g stroke="rgba(246,241,232,0.38)" stroke-width="2"><line x1="50" y1="13" x2="32" y2="34"/><line x1="50" y1="13" x2="68" y2="34"/><line x1="32" y1="34" x2="22" y2="53"/><line x1="32" y1="34" x2="42" y2="53"/><line x1="68" y1="34" x2="58" y2="53"/><line x1="68" y1="34" x2="78" y2="53"/></g><g fill="rgba(246,241,232,0.62)"><circle cx="50" cy="13" r="6"/><circle cx="32" cy="34" r="5"/><circle cx="68" cy="34" r="5"/><circle cx="22" cy="53" r="4"/><circle cx="42" cy="53" r="4"/><circle cx="58" cy="53" r="4"/><circle cx="78" cy="53" r="4"/></g></svg>"""
    case "claude" => // The Claude Stack — a spark
      """<svg viewBox="0 0 100 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet"><path d="M50 8 C 53 26 56 29 74 32 C 56 35 53 38 50 56 C 47 38 44 35 26 32 C 44 29 47 26 50 8 Z" fill="rgba(246,241,232,0.6)"/><path d="M79 13 C 80 21 81 22 87 23 C 81 24 80 25 79 33 C 78 25 77 24 71 23 C 77 22 78 21 79 13 Z" fill="rgba(246,241,232,0.4)"/></svg>"""
    case "lang" => // Programming Languages — code brackets </>
      """<svg viewBox="0 0 100 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet"><g stroke="rgba(246,241,232,0.58)" stroke-width="3.6" stroke-linecap="round" stroke-linejoin="round"><path d="M38 20 L24 32 L38 44"/><path d="M62 20 L76 32 L62 44"/><path d="M55 16 L45 48"/></g></svg>"""
    case _ => ""

  // Syntax-coloured spans for the runnable-code mock (mapped to --syn-* tokens in CSS).
  private def kw(s: String): VdomNode  = <.span(^.className := "cx-tour__kw", s)
  private def fn(s: String): VdomNode  = <.span(^.className := "cx-tour__fn", s)
  private def num(s: String): VdomNode = <.span(^.className := "cx-tour__num", s)
  private def ln(s: String): VdomNode  = <.span(^.className := "cx-tour__ln", s)

  private def chapter(text: String, active: Boolean): VdomNode =
    <.div(^.className := "cx-tour__chap" + (if active then " is-active" else ""), text)

  private def progressSeg(done: Boolean): VdomNode =
    <.span(^.className := "cx-tour__coach-seg" + (if done then " is-done" else ""), ^.aria.hidden := true)

  private def habit(n: String, name: String, desc: String): VdomNode =
    <.div(
      ^.className := "cx-tour__habit",
      <.div(^.className := "cx-tour__habit-num", n),
      <.div(^.className := "cx-tour__habit-name", name),
      <.div(^.className := "cx-tour__habit-desc", desc)
    )

  // ── The five slides ───────────────────────────────────────────────────────────────────────────
  private def booksSlide(active: Int): VdomNode =
    <.div(
      ^.className   := slideClass(active, 0, "cx-tour__slide--split cx-tour__slide--books"),
      ^.aria.hidden := (active != 0),
      <.div(
        ^.className := "cx-tour__col",
        <.div(^.className := "cx-tour__kicker", "01 — The library"),
        <.h3(^.className  := "cx-tour__title", "Open a book,", <.br, "start reading."),
        <.p(
          ^.className := "cx-tour__lede",
          "The home library lists every book Cortex has — each set like a publication you'd actually want " +
            "to read, with interactive widgets and runnable code."
        ),
        <.div(
          ^.className := "cx-tour__list",
          bullet("Re-read any time — no paywall, no ads"),
          bullet("Pick up exactly where you left off"),
          bullet("Yours to keep — 100% of the writing")
        )
      ),
      <.div(
        ^.className := "cx-tour__books",
        bookCard("sd", "system-design", "System Design", "Distributed · Architecture"),
        bookCard("dsa", "data-structures-and-algorithms", "Data Structures & Algorithms", "Python · Java"),
        bookCard("claude", "the-claude-stack", "The Claude Stack", "Claude Code · MCP"),
        bookCard("lang", "languages", "Programming Languages", "Python · Java · SQL")
      )
    )

  private def codeSlide(active: Int): VdomNode =
    <.div(
      ^.className   := slideClass(active, 1, "cx-tour__slide--split cx-tour__slide--code"),
      ^.aria.hidden := (active != 1),
      <.div(
        ^.className := "cx-tour__col",
        <.div(^.className := "cx-tour__kicker", "02 — Runnable code"),
        <.h3(^.className  := "cx-tour__title", "Run it right", <.br, "in the page."),
        <.p(
          ^.className := "cx-tour__lede",
          "Every code block is live. Press ",
          <.strong("Run"),
          " and it executes in the browser — Python, Java, SQL, Go and more. Sign in to make the example your own."
        ),
        <.div(
          ^.className := "cx-tour__list",
          bullet("No setup, no install — it just runs"),
          bullet("Edit the source once you sign in"),
          bullet("Full Monaco editor, same as your IDE")
        )
      ),
      <.div(
        ^.className := "cx-tour__editor",
        <.div(
          ^.className := "cx-tour__editor-head",
          <.div(
            ^.className := "cx-tour__tabs",
            <.span(^.className := "cx-tour__tab is-active", "Python"),
            <.span(^.className := "cx-tour__tab", "Java"),
            <.span(^.className := "cx-tour__tab", "SQL")
          ),
          <.span(^.className := "cx-tour__run", "▶ Run")
        ),
        <.div(
          ^.className := "cx-tour__code",
          <.div(
            ^.className := "cx-tour__code-line",
            ln("1"),
            "  nums = [",
            num("1"),
            ", ",
            num("2"),
            ", ",
            num("3"),
            ", ",
            num("4"),
            ", ",
            num("5"),
            "]"
          ),
          <.div(^.className := "cx-tour__code-line", ln("2"), "  doubled = []"),
          <.div(^.className := "cx-tour__code-line", ln("3"), "  ", kw("for"), " n ", kw("in"), " nums:"),
          <.div(
            ^.className := "cx-tour__code-line",
            ln("4"),
            "      doubled.",
            fn("append"),
            "(n * ",
            num("2"),
            ")"
          ),
          <.div(
            ^.className := "cx-tour__code-line",
            ln("5"),
            "  ",
            fn("print"),
            "(doubled)",
            <.span(^.className := "cx-tour__caret", ^.aria.hidden := true)
          )
        ),
        <.div(
          ^.className := "cx-tour__output",
          <.span(^.className := "cx-tour__output-label", "Output"),
          <.br,
          "[2, 4, 6, 8, 10]"
        )
      )
    )

  private def navSlide(active: Int): VdomNode =
    <.div(
      ^.className   := slideClass(active, 2, "cx-tour__slide--split cx-tour__slide--nav"),
      ^.aria.hidden := (active != 2),
      <.div(
        ^.className := "cx-tour__col",
        <.div(^.className := "cx-tour__kicker", "03 — Find your way"),
        <.h3(^.className  := "cx-tour__title", "Never lose", <.br, "the thread."),
        <.p(
          ^.className := "cx-tour__lede",
          "A book is a lot of pages. The reader keeps you oriented — a chapter sidebar, a live table of " +
            "contents, breadcrumbs and a minimap, all one glance away."
        ),
        <.div(
          ^.className := "cx-tour__list",
          bullet("Sidebar, TOC, breadcrumbs & minimap"),
          bullet("Resume reading lands you back in place"),
          bullet("Focus mode hides all but the page")
        )
      ),
      <.div(
        ^.className := "cx-tour__reader",
        <.div(
          ^.className := "cx-tour__reader-side",
          <.div(^.className := "cx-tour__reader-side-label", "Chapters"),
          <.div(
            ^.className := "cx-tour__chaps",
            chapter("1 · Foundations", active = false),
            chapter("2 · Storage", active = false),
            chapter("3 · Replication", active = true),
            chapter("4 · Partitioning", active = false),
            chapter("5 · Consistency", active = false)
          )
        ),
        <.div(
          ^.className := "cx-tour__reader-main",
          <.div(^.className := "cx-tour__crumb", "Systems / Replication"),
          <.div(^.className := "cx-tour__reader-title", "Leaders & Followers"),
          <.div(
            ^.className := "cx-tour__reader-body",
            "A replica designated the leader accepts all writes. Followers receive the same changes as a " +
              "log and apply them in order…"
          ),
          <.div(^.className := "cx-tour__skeleton", ^.aria.hidden                          := true),
          <.div(^.className := "cx-tour__skeleton cx-tour__skeleton--short", ^.aria.hidden := true),
          <.div(
            ^.className := "cx-tour__reader-foot",
            <.span("← Storage"),
            <.span("Partitioning →")
          )
        ),
        <.div(
          ^.className   := "cx-tour__minimap",
          ^.aria.hidden := true,
          (0 until 5).map { i =>
            <.div(
              ^.key       := s"mm$i",
              ^.className := "cx-tour__minimap-bar" + (if i == 0 then " is-active" else "")
            )
          }.toTagMod
        )
      )
    )

  private def coachSlide(active: Int): VdomNode =
    <.div(
      ^.className   := slideClass(active, 3, "cx-tour__slide--split cx-tour__slide--coach"),
      ^.aria.hidden := (active != 3),
      <.div(
        ^.className := "cx-tour__col",
        <.div(^.className := "cx-tour__kicker", "04 — The Coach"),
        <.h3(^.className  := "cx-tour__title cx-tour__title--sm", "A tutor that", <.br, "asks first."),
        <.p(
          ^.className := "cx-tour__lede cx-tour__lede--sm",
          "The Coach doesn't hand over the answer — it walks you through your own thinking, one step at a time."
        ),
        <.div(
          ^.className := "cx-tour__keynote",
          <.div(
            ^.className := "cx-tour__keynote-head",
            LucideIcons.Lock(LucideIcons.withClass("cx-tour__keynote-icon")),
            "Bring your own key"
          ),
          <.p(
            ^.className := "cx-tour__keynote-body",
            "Runs on ",
            <.strong("your own OpenRouter key"),
            ". Paste it once, pick a model, go. It stays in ",
            <.em("this tab only"),
            " — never saved, never sent to Cortex, only to OpenRouter."
          )
        )
      ),
      <.div(
        ^.className := "cx-tour__coach",
        <.div(
          ^.className := "cx-tour__coach-head",
          <.div(
            ^.className := "cx-tour__coach-bar",
            <.div(
              ^.className := "cx-tour__coach-brand",
              <.img(
                ^.className := "cx-tour__coach-logo",
                ^.src       := "/img/cortex/cortex-chip.svg",
                ^.alt       := ""
              ),
              "Coach"
            ),
            <.span(^.className := "cx-tour__coach-step", "Step 2 of 6 · Approach")
          ),
          <.div(
            ^.className := "cx-tour__coach-progress",
            progressSeg(done = true),
            progressSeg(done = true),
            progressSeg(done = false),
            progressSeg(done = false),
            progressSeg(done = false),
            progressSeg(done = false)
          )
        ),
        <.div(
          ^.className := "cx-tour__coach-body",
          <.div(
            ^.className := "cx-tour__msg cx-tour__msg--bot",
            "What's the brute-force way to find the two numbers, and what does it cost?"
          ),
          <.div(^.className := "cx-tour__msg cx-tour__msg--user", "Check every pair — two loops, so O(n²)."),
          <.div(
            ^.className := "cx-tour__msg cx-tour__msg--bot",
            "Right. What could you remember as you go to skip the inner loop?",
            <.span(^.className := "cx-tour__msg-meta", "✓ approach · 8/10")
          )
        ),
        <.div(
          ^.className := "cx-tour__coach-input",
          <.div(^.className  := "cx-tour__coach-field", "Type your answer…"),
          <.span(^.className := "cx-tour__coach-send", "Send")
        )
      )
    )

  private def habitSlide(active: Int): VdomNode =
    <.div(
      ^.className   := slideClass(active, 4, "cx-tour__slide--habit"),
      ^.aria.hidden := (active != 4),
      <.div(^.className := "cx-tour__habit-kicker", "05 — Make it a habit"),
      <.h3(^.className  := "cx-tour__habit-title", "Get the most out of it."),
      <.div(
        ^.className := "cx-tour__habits",
        habit(
          "01",
          "Read, then run",
          "Don't just read the example — press Run, change a value, run it again."
        ),
        habit("02", "Let it test you", "On the shaky parts, open the Coach and have it grill you."),
        habit("03", "Key once per tab", "Paste your OpenRouter key at the start — it's gone when you close."),
        habit("04", "Come back", "Resume reading drops you back where you stopped.")
      )
    )

  // ── Component ─────────────────────────────────────────────────────────────────────────────────
  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useState(0)                      // indexS  : active slide
      .useState(false)                  // pausedS : hover-pause flag
      .useRefBy(_ => Option.empty[Int]) // timerRef: pending auto-advance timeout id
      .useEffectOnMountBy { (_, indexS, _, _) =>
        // Resume where the visitor left off (client-only SPA — localStorage is available).
        Callback {
          val saved = loadIndex()
          if saved != 0 then indexS.setState(saved).runNow()
        }
      }
      .useEffectWithDepsBy((_, indexS, pausedS, _) => (indexS.value, pausedS.value)) {
        (_, indexS, _, timerRef) => (index, paused) =>
          Callback {
            timerRef.value.foreach(dom.window.clearTimeout)
            timerRef.value = None
            if !paused && !reducedMotion then
              val next = (index + 1) % SlideCount
              val id = dom.window.setTimeout(
                () => { indexS.setState(next).runNow(); saveIndex(next) },
                IntervalMs
              )
              timerRef.value = Some(id)
          }
      }
      .render { (_, indexS, pausedS, _) =>
        val index = indexS.value

        def go(i: Int): Callback =
          val n = ((i % SlideCount) + SlideCount) % SlideCount
          indexS.setState(n) >> Callback(saveIndex(n))

        <.div(
          ^.className := "cx-tour",
          <.div(
            ^.className := "cx-tour__card",
            ^.onMouseEnter --> pausedS.setState(true),
            ^.onMouseLeave --> pausedS.setState(false),
            <.div(
              ^.className := "cx-tour__stage",
              booksSlide(index),
              codeSlide(index),
              navSlide(index),
              coachSlide(index),
              habitSlide(index)
            ),
            <.div(
              ^.className := "cx-tour__bar",
              <.div(
                ^.className := "cx-tour__counter",
                ^.aria.live := "polite",
                s"0${index + 1} / 05",
                <.span(^.className := "cx-tour__counter-dash", " — "),
                Labels(index)
              ),
              <.div(
                ^.className := "cx-tour__dots",
                (0 until SlideCount).map { i =>
                  <.button(
                    ^.key        := s"dot$i",
                    ^.tpe        := "button",
                    ^.className  := "cx-tour__dot" + (if i == index then " is-active" else ""),
                    ^.aria.label := s"Go to slide ${i + 1}: ${Labels(i)}",
                    ^.onClick --> go(i)
                  )
                }.toTagMod
              ),
              <.div(
                ^.className := "cx-tour__nav",
                <.button(
                  ^.tpe        := "button",
                  ^.className  := "cx-tour__navbtn",
                  ^.aria.label := "Previous slide",
                  ^.onClick --> go(index - 1),
                  LucideIcons.ChevronLeft(LucideIcons.withClass("cx-tour__navbtn-icon"))
                ),
                <.button(
                  ^.tpe        := "button",
                  ^.className  := "cx-tour__navbtn cx-tour__navbtn--next",
                  ^.aria.label := "Next slide",
                  ^.onClick --> go(index + 1),
                  LucideIcons.ChevronRight(LucideIcons.withClass("cx-tour__navbtn-icon"))
                )
              )
            )
          )
        )
      }
