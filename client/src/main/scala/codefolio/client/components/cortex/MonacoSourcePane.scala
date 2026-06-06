package codefolio.client.components.cortex

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Read-only Monaco editor for the Visualise modal's SOURCE pane.
 *
 * The modal traces a fixed snapshot of the code, so this pane never edits — it only *displays* the source
 * through the same Monaco surface + `codefolio-dark` theme as the chapter code blocks (visual consistency,
 * real syntax highlighting), and marks the line that just executed (`currentLine`) and the line the next step
 * will run (`nextLine`) with whole-line decorations. Decorations + a gentle
 * `revealLineInCenterIfOutsideViewport` are applied imperatively through the editor instance captured in
 * `onMount`, driven by a `(currentLine, nextLine)`-keyed effect — so stepping never remounts Monaco.
 *
 * Self-contained: it owns its editor / monaco / decoration-id refs, so the host modal's (large) hook chain is
 * untouched. The hand-rolled tokenizer's hover-to-highlight-cursor affordance is intentionally dropped here —
 * hovering a local in the Frames pane still highlights its arrow.
 */
object MonacoSourcePane:

  final case class Props(
      source: String,
      language: String,
      currentLine: Option[Int],
      nextLine: Option[Int]
  )

  /** Our runnable-language aliases → Monaco language ids (mirror of RunnableCodeBlock.monacoLangFor). */
  private def monacoLangFor(alias: String): String = alias.toLowerCase match
    case "python" | "py" | "python3"  => "python"
    case "java"                       => "java"
    case "scala"                      => "scala"
    case "c" | "cpp" | "c++" | "cxx"  => "cpp"
    case "go" | "golang"              => "go"
    case "rust" | "rs"                => "rust"
    case "kotlin" | "kt"              => "kotlin"
    case "typescript" | "ts"          => "typescript"
    case "javascript" | "js" | "node" => "javascript"
    case other                        => other

  /**
   * Replace the line decorations with whole-line highlights for `current` / `next` and keep the active line
   * in view. Returns the new decoration-id set (feed it back as `oldIds` on the next call).
   */
  private def applyDecorations(
      ed: js.Dynamic,
      monaco: js.Dynamic,
      current: Option[Int],
      next: Option[Int],
      oldIds: js.Array[String]
  ): js.Array[String] =
    val decos = js.Array[js.Any]()
    def add(line: Int, cls: String): Unit =
      val range = js.Dynamic.newInstance(monaco.selectDynamic("Range"))(line, 1, line, 1)
      val _ = decos.push(
        js.Dynamic.literal(range = range, options = js.Dynamic.literal(isWholeLine = true, className = cls))
      )
    current.foreach(add(_, "algolens-mono__line--current"))
    next.foreach(add(_, "algolens-mono__line--next"))
    val newIds = ed.deltaDecorations(oldIds, decos).asInstanceOf[js.Array[String]]
    current.foreach(line => ed.revealLineInCenterIfOutsideViewport(line))
    newIds

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useRefBy(_ => Option.empty[js.Dynamic])               // edRef     — editor instance
      .useRefBy(_ => Option.empty[js.Dynamic])               // monacoRef — monaco namespace (for Range)
      .useRefBy(_ => js.Array[String]())                     // decoRef   — current decoration ids
      .useRefBy(_ => (Option.empty[Int], Option.empty[Int])) // lineRef   — latest (current, next) for onMount
      // Re-decorate + reveal whenever the executing / next line changes. Sentinel -1 for None keeps the
      // dep an (Int, Int) tuple (cheap Reusability) while the body reads the real Options.
      .useEffectWithDepsBy((p, _, _, _, _) => (p.currentLine.getOrElse(-1), p.nextLine.getOrElse(-1))) {
        (p, edRef, monacoRef, decoRef, lineRef) => _ =>
          Callback {
            lineRef.value = (p.currentLine, p.nextLine)
            (edRef.value, monacoRef.value) match
              case (Some(ed), Some(monaco)) =>
                decoRef.value = applyDecorations(ed, monaco, p.currentLine, p.nextLine, decoRef.value)
              case _ => ()
          }
      }
      .render { (p, edRef, monacoRef, decoRef, lineRef) =>
        val opts: js.Object = js.Dynamic
          .literal(
            readOnly = true,
            domReadOnly = true,
            padding = js.Dynamic.literal(top = 8, bottom = 8),
            fontFamily =
              "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace",
            fontSize = 13,
            lineHeight = 20,
            tabSize = 4,
            minimap = js.Dynamic.literal(enabled = false),
            scrollBeyondLastLine = false,
            wordWrap = "off",
            automaticLayout = true,
            renderLineHighlight = "none",
            scrollbar = js.Dynamic.literal(alwaysConsumeMouseWheel = false),
            lineNumbersMinChars = 3,
            glyphMargin = false,
            folding = false,
            cursorStyle = "line",
            renderValidationDecorations = "off"
          )
          .asInstanceOf[js.Object]

        val mProps = (new js.Object).asInstanceOf[MonacoEditor.EditorProps]
        mProps.value = p.source
        mProps.language = monacoLangFor(p.language)
        mProps.theme = "codefolio-dark"
        // Definite-height flex container (see .algolens-code-editor__monaco) → "100%" resolves and
        // automaticLayout keeps Monaco filling it; no content-driven height dance needed (unlike the
        // editable chapter blocks, this pane is a fixed-height grid cell that scrolls internally).
        mProps.height = "100%"
        mProps.options = opts
        mProps.loading = "Loading source…"
        mProps.onMount = (ed, monaco) =>
          edRef.value = Some(ed)
          monacoRef.value = Some(monaco)
          // @monaco-editor/react mounts its container behind display:none, so Monaco's first measure is
          // 0×0 and it falls back to a 5px layout that automaticLayout doesn't reliably recover from.
          // Force it to fill the (definite-height) pane via an external ResizeObserver that calls
          // ed.layout() from the host's rect (mirrors RunnableCodeBlock.monacoView).
          val node = ed.getContainerDomNode().asInstanceOf[dom.html.Element]
          val host = Option(node.closest(".algolens-code-editor__monaco"))
            .map(_.asInstanceOf[dom.html.Element])
            .getOrElse(node)
          val relayout: () => Unit = () =>
            val rect = host.getBoundingClientRect()
            if rect.width > 0 && rect.height > 0 then
              val _ = ed.layout(js.Dynamic.literal(width = rect.width, height = rect.height))
          val resizeCb: js.Function2[js.Array[dom.ResizeObserverEntry], dom.ResizeObserver, Unit] =
            (_, _) => relayout()
          val resizeObs = new dom.ResizeObserver(resizeCb)
          resizeObs.observe(host)
          val onDispose: js.Function0[Unit] = () => resizeObs.disconnect()
          ed.onDidDispose(onDispose)
          relayout()
          val (cur, nxt) = lineRef.value
          decoRef.value = applyDecorations(ed, monaco, cur, nxt, decoRef.value)

        <.div(
          ^.className := "algolens__pane algolens__pane--code",
          <.div(
            ^.className := "algolens-code-editor",
            <.header(
              ^.className := "algolens-code-editor__head",
              <.span(^.className := "algolens-code-editor__eyebrow", "SOURCE · READ-ONLY")
            ),
            <.div(
              ^.className := "algolens-code-editor__monaco",
              MonacoEditor.Component(mProps)
            )
          )
        )
      }
