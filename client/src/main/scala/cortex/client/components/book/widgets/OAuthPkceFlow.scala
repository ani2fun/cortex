package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * OAuth-PKCE-flow widget — a step-through sequence diagram of an OAuth 2.0 / OIDC message exchange.
 *
 * Four (or more) actors become vertical lanes; each payload step is one message between two lanes (or a
 * self-step computed on one lane). A [[Stepper]] scrubs the exchange one message at a time: messages before
 * the cursor are drawn solid-but-muted, the cursor message is highlighted in its kind colour, and messages
 * after the cursor are faint — so the reader watches the handshake build up. A caption echoes the current
 * step's plain-English `detail`.
 *
 * Multiple `variants` let one widget instance carry both the secure (PKCE) flow and a vulnerable (no-PKCE)
 * flow with an interception step, toggled by a button row — the centerpiece of the "Why PKCE exists" chapter.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Authorization Code Flow + PKCE",
 *   "actors": [
 *     { "id": "ua",  "label": "Browser" },
 *     { "id": "spa", "label": "Cortex SPA" },
 *     { "id": "as",  "label": "Keycloak" },
 *     { "id": "rs",  "label": "Cortex API" }
 *   ],
 *   "variants": [
 *     {
 *       "id": "pkce", "label": "With PKCE (secure)",
 *       "summary": "The public client proves it started the flow.",
 *       "steps": [
 *         { "from": "spa", "to": "spa", "kind": "compute",  "label": "make code_verifier", "detail": "..." },
 *         { "from": "spa", "to": "as",  "kind": "redirect", "label": "/auth?code_challenge=…", "detail": "…" },
 *         { "from": "as",  "to": "spa", "kind": "redirect", "label": "redirect ?code=…", "detail": "…" },
 *         { "from": "spa", "to": "as",  "kind": "token",    "label": "POST /token (+verifier)", "detail": "…" },
 *         { "from": "spa", "to": "rs",  "kind": "data",     "label": "GET /api (Bearer)", "detail": "…" }
 *       ]
 *     }
 *   ]
 * }
 * }}}
 *
 *   - `kind` is one of `request | redirect | token | data | compute | attack`; it picks the message colour.
 *     `compute` is the natural kind for a self-step (`from == to`). `attack` paints an interceptor step red.
 *     Unknown values fall back to `request`.
 *   - A step with `from == to` renders as a rounded box straddling that lane (a computation/decision on that
 *     actor) rather than an arrow.
 *   - `detail` is the caption shown under the diagram for the current step; `label` is the short text on the
 *     arrow itself. Keep `label` short — SVG text does not wrap.
 *
 * SVG is built as a string and injected via `dangerouslySetInnerHtml` — the same pattern HandshakeTimeline
 * and the rest of the string-SVG catalog use. Only the variant index and the stepper index live in React
 * state.
 */
object OAuthPkceFlow:

  // ===========================================================================
  // Schema
  // ===========================================================================

  enum MsgKind:
    case Request, Redirect, Token, Data, Compute, Attack

  final case class Actor(id: String, label: String)
  final case class Step(from: String, to: String, kind: MsgKind, label: String, detail: String)
  final case class Variant(id: String, label: String, summary: Option[String], steps: List[Step])
  final case class Spec(title: Option[String], actors: List[Actor], variants: List[Variant])

  final case class Props(payload: String)

  // ===========================================================================
  // Layout constants
  // ===========================================================================

  private val ViewBoxWidth = 760.0
  private val SidePad      = 74.0 // lane inset from the SVG edge
  private val TopPad       = 8.0
  private val ActorBoxH    = 34.0
  private val ActorBoxW    = 132.0
  private val HeaderGap    = 38.0 // space between actor boxes and first message row
  private val RowGap       = 50.0
  private val BottomPad    = 22.0
  private val SelfWidth    = 168.0
  private val SelfHeight   = 30.0
  private val ArrowSize    = 8.0
  private val StepDelayMs  = 1600

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parseKind(s: String): MsgKind = s.toLowerCase match
    case "request"  => MsgKind.Request
    case "redirect" => MsgKind.Redirect
    case "token"    => MsgKind.Token
    case "data"     => MsgKind.Data
    case "compute"  => MsgKind.Compute
    case "attack"   => MsgKind.Attack
    case _          => MsgKind.Request

  private def parseActor(d: js.Dynamic): Actor =
    Actor(id = d.string("id").trim, label = d.string("label").trim)

  private def parseStep(d: js.Dynamic): Step =
    Step(
      from = d.string("from").trim,
      to = d.string("to").trim,
      kind = parseKind(d.string("kind")),
      label = d.string("label").trim,
      detail = d.string("detail").trim
    )

  private def parseVariant(d: js.Dynamic): Variant =
    Variant(
      id = d.string("id").trim,
      label = d.string("label").trim,
      summary = d.optString("summary"),
      steps = d.dynList("steps").map(parseStep)
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val actors   = d.dynList("actors").map(parseActor).filter(_.id.nonEmpty)
      val variants = d.dynList("variants").map(parseVariant).filter(_.id.nonEmpty)
      if actors.size < 2 then throw PayloadDecoder.invalid("actors must have at least 2 entries")
      if variants.isEmpty then throw PayloadDecoder.invalid("variants must be non-empty")
      if variants.exists(_.steps.isEmpty) then
        throw PayloadDecoder.invalid("every variant.steps must be non-empty")
      val actorIds = actors.map(_.id).toSet
      val bad      = variants.flatMap(_.steps).flatMap(s => List(s.from, s.to)).find(!actorIds.contains(_))
      bad.foreach(id => throw PayloadDecoder.invalid(s"step references unknown actor id '$id'"))
      Spec(d.optString("title"), actors, variants)
    }

  // ===========================================================================
  // SVG building
  // ===========================================================================

  private def esc(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  private def kindClass(k: MsgKind): String = k match
    case MsgKind.Request  => "oauth-pkce-flow__msg--request"
    case MsgKind.Redirect => "oauth-pkce-flow__msg--redirect"
    case MsgKind.Token    => "oauth-pkce-flow__msg--token"
    case MsgKind.Data     => "oauth-pkce-flow__msg--data"
    case MsgKind.Compute  => "oauth-pkce-flow__msg--compute"
    case MsgKind.Attack   => "oauth-pkce-flow__msg--attack"

  private def stateClass(stepIdx: Int, cursor: Int): String =
    if stepIdx < cursor then "oauth-pkce-flow__msg--past"
    else if stepIdx == cursor then "oauth-pkce-flow__msg--current"
    else "oauth-pkce-flow__msg--future"

  private def laneX(i: Int, n: Int): Double =
    if n <= 1 then ViewBoxWidth / 2.0
    else SidePad + i.toDouble * ((ViewBoxWidth - 2 * SidePad) / (n - 1).toDouble)

  private def rowY(i: Int): Double =
    TopPad + ActorBoxH + HeaderGap + i.toDouble * RowGap

  private def diagramHeight(stepCount: Int): Double =
    rowY(math.max(0, stepCount - 1)) + RowGap / 2 + BottomPad

  private def arrowHead(x2: Double, y: Double, goingRight: Boolean): String =
    val tipBackX = if goingRight then x2 - ArrowSize else x2 + ArrowSize
    s"""<polygon class="oauth-pkce-flow__msg-arrow"
       |  points="$x2,$y $tipBackX,${y - ArrowSize / 2} $tipBackX,${y + ArrowSize / 2}"/>""".stripMargin

  private def messageSvg(step: Step, idx: Int, cursor: Int, indexOf: String => Int, n: Int): String =
    val cls = s"oauth-pkce-flow__msg ${stateClass(idx, cursor)} ${kindClass(step.kind)}"
    val y   = rowY(idx)
    if step.from == step.to then
      val cx   = laneX(indexOf(step.from), n)
      val x    = cx - SelfWidth / 2
      val boxY = y - SelfHeight / 2
      s"""<g class="$cls">
         |  <rect class="oauth-pkce-flow__self-rect" x="$x" y="$boxY" width="$SelfWidth" height="$SelfHeight" rx="6"/>
         |  <text class="oauth-pkce-flow__self-label" x="$cx" y="${y + 4}" text-anchor="middle">${esc(
          step.label
        )}</text>
         |</g>""".stripMargin
    else
      val x1         = laneX(indexOf(step.from), n)
      val x2         = laneX(indexOf(step.to), n)
      val goingRight = x2 > x1
      val midX       = (x1 + x2) / 2
      val line =
        s"""<line class="oauth-pkce-flow__msg-line" x1="$x1" y1="$y" x2="$x2" y2="$y"/>"""
      val head = arrowHead(x2, y, goingRight)
      val label =
        s"""<text class="oauth-pkce-flow__msg-label" x="$midX" y="${y - 8}" text-anchor="middle">${esc(
            step.label
          )}</text>"""
      s"""<g class="$cls">$line\n$head\n$label</g>"""

  private def buildSvg(spec: Spec, variant: Variant, cursor: Int): String =
    val n       = spec.actors.size
    val indexOf = spec.actors.zipWithIndex.map { case (a, i) => a.id -> i }.toMap
    val stepCnt = variant.steps.size
    val height  = diagramHeight(stepCnt)
    val lifeTop = TopPad + ActorBoxH
    val lifeBot = height - BottomPad / 2

    val header = spec.actors.zipWithIndex.map { case (a, i) =>
      val cx = laneX(i, n)
      val x  = cx - ActorBoxW / 2
      s"""<g class="oauth-pkce-flow__actor">
         |  <rect class="oauth-pkce-flow__actor-box" x="$x" y="$TopPad" width="$ActorBoxW" height="$ActorBoxH" rx="6"/>
         |  <text class="oauth-pkce-flow__actor-label" x="$cx" y="${TopPad + ActorBoxH / 2 + 4}" text-anchor="middle">${esc(
          a.label
        )}</text>
         |</g>""".stripMargin
    }.mkString("\n")

    val lifelines = spec.actors.indices.map { i =>
      val cx = laneX(i, n)
      s"""<line class="oauth-pkce-flow__lifeline" x1="$cx" y1="$lifeTop" x2="$cx" y2="$lifeBot"/>"""
    }.mkString("\n")

    val messages = variant.steps.zipWithIndex.map { case (s, i) =>
      messageSvg(s, i, cursor, indexOf, n)
    }.mkString("\n")

    s"""<svg viewBox="0 0 $ViewBoxWidth $height" class="oauth-pkce-flow__svg" role="img"
       |     aria-label="${esc(spec.title.getOrElse("OAuth flow"))}" xmlns="http://www.w3.org/2000/svg">
       |  $lifelines
       |  $header
       |  $messages
       |</svg>""".stripMargin

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(0) // selected variant index
      .customBy { (_, specM, variantS) =>
        val stepCount =
          specM.value.toOption.flatMap(_.variants.lift(variantS.value)).fold(0)(_.steps.size)
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs.toDouble))
      }
      .render { (_, specM, variantS, stepper) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "OAuth-PKCE-flow payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val vIdx    = math.min(variantS.value, spec.variants.size - 1)
            val variant = spec.variants(vIdx)
            val count   = variant.steps.size
            val cursor  = stepper.index
            val current = variant.steps.lift(cursor)

            val variantBar: VdomNode =
              if spec.variants.size <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "oauth-pkce-flow__variants",
                  spec.variants.zipWithIndex.toVdomArray { case (v, i) =>
                    <.button(
                      ^.key := v.id,
                      ^.tpe := "button",
                      ^.className := s"oauth-pkce-flow__variant-btn${
                          if i == vIdx then " oauth-pkce-flow__variant-btn--active" else ""
                        }",
                      ^.onClick --> (variantS.setState(i) >> stepper.reset),
                      v.label
                    )
                  }
                )

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "oauth-pkce-flow__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Previous message",
                    ^.className  := "oauth-pkce-flow__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("oauth-pkce-flow__button-icon")),
                    "Prev"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "oauth-pkce-flow__button oauth-pkce-flow__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("oauth-pkce-flow__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("oauth-pkce-flow__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Next message",
                    ^.className  := "oauth-pkce-flow__button",
                    "Next",
                    LucideIcons.ArrowRight(LucideIcons.withClass("oauth-pkce-flow__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "oauth-pkce-flow__button oauth-pkce-flow__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("oauth-pkce-flow__button-icon"))
                  ),
                  <.span(
                    ^.className := "oauth-pkce-flow__progress",
                    s"Step ${cursor + 1} / ${math.max(1, count)}"
                  )
                )

            <.div(
              ^.className := "oauth-pkce-flow not-prose",
              spec.title
                .map(t => <.p(^.className := "oauth-pkce-flow__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              variantBar,
              variant.summary
                .map(s => <.p(^.className := "oauth-pkce-flow__summary", s): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className               := "oauth-pkce-flow__frame",
                ^.dangerouslySetInnerHtml := buildSvg(spec, variant, cursor)
              ),
              <.p(
                ^.className := "oauth-pkce-flow__detail",
                ^.aria.live := "polite",
                current.map(_.detail).filter(_.nonEmpty).getOrElse(" ")
              ),
              controls
            )
      }
