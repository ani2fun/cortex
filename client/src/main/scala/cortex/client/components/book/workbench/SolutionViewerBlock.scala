package cortex.client.components.book.workbench

import cortex.client.components.book.MonacoEditor
import cortex.client.components.icons.LucideIcons
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

import scala.scalajs.js

/**
 * Read-only solution viewer inside problem editorials (from adjacent ```<lang> solution fences): language
 * tabs + a lock "Read-only" badge over a permanently read-only Monaco surface, with optional Time / Space
 * complexity chips underneath. Monaco (not a static highlighted <pre>) so the solution looks exactly like the
 * live editor on the right — same theme, same font, same tokenisation — and so the panel CSS that quiets
 * plaintext example boxes can never wash out solution code.
 */
object SolutionViewerBlock:

  /** Maps our runnable-language aliases onto Monaco's language ids (same map as the editors). */
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

  /** File extension for per-tab Monaco model paths (cosmetic, but keeps model ids honest). */
  private def extFor(alias: String): String = alias.toLowerCase match
    case "python" | "py" | "python3"  => "py"
    case "java"                       => "java"
    case "scala"                      => "scala"
    case "c"                          => "c"
    case "cpp" | "c++" | "cxx"        => "cpp"
    case "go" | "golang"              => "go"
    case "rust" | "rs"                => "rs"
    case "kotlin" | "kt"              => "kt"
    case "typescript" | "ts"          => "ts"
    case "javascript" | "js" | "node" => "js"
    case _                            => "txt"

  // Model paths are global to the Monaco runtime; several viewers can share a page.
  private var svSeq = 0

  private def nextSvId(): String =
    svSeq += 1
    s"wb-solution-$svSeq"

  final case class Props(
      tabs: List[Block.Tab],
      time: Option[String],
      space: Option[String]
  )

  /** "🐍 Python 3.8" → "Python 3.8" — the tab strip is text-only here. */
  private def displayLabel(tab: Block.Tab): String =
    val stripped = tab.languageLabel.replaceAll("^[^\\p{ASCII}]+\\s*", "").trim
    if stripped.nonEmpty then stripped else tab.language

  /** Content-driven height: the solution is fully visible, no inner scrolling (clamped for sanity). */
  private def heightFor(source: String): Int =
    val lines = math.max(source.split("\n").length, 3)
    math.min(lines * 22 + 28, 620)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(0)
      .useRefBy((_, _) => nextSvId())
      .render { (props, activeS, svIdRef) =>
        val active = props.tabs.lift(activeS.value).getOrElse(props.tabs.head)
        val height = heightFor(active.source)

        val monaco: VdomNode =
          val opts: js.Object = js.Dynamic
            .literal(
              readOnly = true,
              domReadOnly = true,
              padding = js.Dynamic.literal(top = 12, bottom = 12),
              fontFamily =
                "\"JetBrains Mono\", ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace",
              fontSize = 12.5,
              lineHeight = 22,
              minimap = js.Dynamic.literal(enabled = false),
              scrollBeyondLastLine = false,
              wordWrap = "off",
              automaticLayout = true,
              renderLineHighlight = "none",
              scrollbar = js.Dynamic.literal(alwaysConsumeMouseWheel = false),
              fixedOverflowWidgets = true,
              renderValidationDecorations = "off",
              contextmenu = false
            )
            .asInstanceOf[js.Object]

          val mProps = (new js.Object).asInstanceOf[MonacoEditor.EditorProps]
          // Per-tab models (`path` mode) — same fix as WorkbenchEditor: swapping one model's value
          // across tabs is racy in the wrapper; per-path models switch cleanly.
          mProps.path = s"${svIdRef.value}/sol-${activeS.value}.${extFor(active.language)}"
          mProps.defaultValue = active.source
          mProps.defaultLanguage = monacoLangFor(active.language)
          mProps.theme = "cortex-dark"
          mProps.height = height
          mProps.options = opts
          mProps.loading = "Loading solution…"
          mProps.onMount = (ed, _) =>
            // The viewer lives in lazily-mounted accordion panels that hide/unhide; Monaco's own
            // auto-layout doesn't reliably recover from display:none flips, so re-measure externally
            // (same trick as the editors).
            val container = ed.getContainerDomNode().asInstanceOf[dom.html.Element]
            val relayout: () => Unit = () =>
              val rect = container.getBoundingClientRect()
              if rect.width > 0 && rect.height > 0 then
                val _ = ed.layout(js.Dynamic.literal(width = rect.width, height = rect.height))
            val resizeCb: js.Function2[js.Array[dom.ResizeObserverEntry], dom.ResizeObserver, Unit] =
              (_, _) => relayout()
            val resizeObs = new dom.ResizeObserver(resizeCb)
            resizeObs.observe(container)
            relayout()

          <.div(
            ^.className := "wb-solution__monaco not-prose",
            ^.style     := js.Dynamic.literal(height = s"${height}px").asInstanceOf[js.Object],
            MonacoEditor.Component(mProps)
          )

        <.div(
          ^.className := "wb-solution not-prose",
          <.div(
            ^.className := "wb-solution__head",
            <.div(
              ^.className := "wb-solution__tabs",
              props.tabs.zipWithIndex.toVdomArray { case (tab, i) =>
                val on = i == activeS.value
                <.button(
                  ^.key := s"sol-$i",
                  ^.tpe := "button",
                  ^.className := (if on then "wb-solution__tab wb-solution__tab--active"
                                  else "wb-solution__tab"),
                  ^.onClick --> activeS.setState(i),
                  displayLabel(tab)
                )
              }
            ),
            <.span(
              ^.className := "wb-solution__lock",
              LucideIcons.Lock(LucideIcons.withClass("wb-solution__lock-icon")),
              "Read-only"
            )
          ),
          monaco,
          if props.time.isDefined || props.space.isDefined then
            <.div(
              ^.className := "wb-solution__meta",
              props.time.map { t =>
                <.span(
                  ^.className := "wb-solution__meta-chip",
                  LucideIcons.Clock(LucideIcons.withClass("wb-solution__meta-icon")),
                  s"Time $t"
                ): VdomNode
              }.getOrElse(EmptyVdom),
              props.space.map { sp =>
                <.span(
                  ^.className := "wb-solution__meta-chip",
                  LucideIcons.Cpu(LucideIcons.withClass("wb-solution__meta-icon")),
                  s"Space $sp"
                ): VdomNode
              }.getOrElse(EmptyVdom)
            )
          else EmptyVdom
        )
      }
