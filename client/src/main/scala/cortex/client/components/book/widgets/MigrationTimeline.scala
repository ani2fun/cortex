package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import cortex.client.components.icons.LucideIcons
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.collection.mutable
import scala.scalajs.js

/**
 * Migration-timeline widget — visualises a database schema evolving as Liquibase applies changesets one at a
 * time. A [[Stepper]] walks the changelog; as each changeset applies, the **schema** panel gains (or loses)
 * tables and a new row appears in the **DATABASECHANGELOG** tracking table — the exact mechanism Liquibase
 * uses to know what has already run. The current changeset's SQL is shown below.
 *
 * Pure HTML, React-owned. State = the stepper index.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "Cortex's schema, evolving",
 *   "changesets": [
 *     { "id": "1-create-visits", "author": "cortex",
 *       "sql": "CREATE TABLE visits (...)", "creates": ["visits"], "note": "…" },
 *     { "id": "2-create-submissions", "author": "cortex",
 *       "sql": "CREATE TABLE submissions (...)", "creates": ["submissions"], "note": "…" }
 *   ]
 * }
 * }}}
 *
 *   - `creates` / `drops` (both optional arrays) update the running schema as the changeset applies.
 *   - The MD5SUM column is a deterministic stand-in derived from the id — illustrative, not a real Liquibase
 *     checksum.
 */
object MigrationTimeline:

  final case class Changeset(
      id: String,
      author: String,
      sql: String,
      creates: List[String],
      drops: List[String],
      note: String
  )

  final case class Spec(title: Option[String], changesets: List[Changeset])
  final case class Props(payload: String)

  private val StepDelayMs = 1700

  private def parseChangeset(d: js.Dynamic): Changeset =
    Changeset(
      id = d.string("id").trim,
      author = d.optString("author").getOrElse("cortex"),
      sql = d.string("sql").trim,
      creates = d.stringList("creates").getOrElse(Nil),
      drops = d.stringList("drops").getOrElse(Nil),
      note = d.string("note").trim
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val cs = d.dynList("changesets").map(parseChangeset).filter(_.id.nonEmpty)
      if cs.isEmpty then throw PayloadDecoder.invalid("changesets must be non-empty")
      Spec(d.optString("title"), cs)
    }

  /** Illustrative, deterministic checksum from the id — NOT a real Liquibase MD5. */
  private def fakeChecksum(id: String): String =
    val h = Integer.toHexString(math.abs(id.hashCode))
    s"8:${(h + "00000000").take(8)}"

  // Running schema (ordered) + applied changelog rows, up to the cursor.
  private def replay(spec: Spec, cursor: Int): (List[String], List[Changeset]) =
    val tables  = mutable.LinkedHashSet.empty[String]
    val applied = mutable.ArrayBuffer.empty[Changeset]
    var i       = 0
    while i <= cursor && i < spec.changesets.size do
      val cs = spec.changesets(i)
      cs.creates.foreach(tables += _)
      cs.drops.foreach(tables -= _)
      applied += cs
      i += 1
    (tables.toList, applied.toList)

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .customBy { (_, specM) =>
        val stepCount = specM.value.toOption.fold(0)(_.changesets.size)
        Stepper.hook(Stepper.Input(stepCount, StepDelayMs.toDouble))
      }
      .render { (_, specM, stepper) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Migration-timeline payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val cursor            = stepper.index
            val count             = spec.changesets.size
            val (tables, applied) = replay(spec, cursor)
            val current           = spec.changesets.lift(cursor)

            val program: VdomNode =
              <.ol(
                ^.className := "migration-timeline__list",
                spec.changesets.zipWithIndex.toVdomArray { case (cs, i) =>
                  val cls =
                    if i < cursor then "applied"
                    else if i == cursor then "current"
                    else "pending"
                  <.li(
                    ^.key       := cs.id,
                    ^.className := s"migration-timeline__cs migration-timeline__cs--$cls",
                    <.span(^.className := "migration-timeline__cs-mark", if i <= cursor then "●" else "○"),
                    <.code(^.className := "migration-timeline__cs-id", cs.id)
                  )
                }
              )

            val schemaPanel: VdomNode =
              <.div(
                ^.className := "migration-timeline__panel",
                <.p(^.className := "migration-timeline__panel-label", "Schema — tables that exist"),
                if tables.isEmpty then <.p(^.className := "migration-timeline__empty", "(empty database)")
                else
                  <.div(
                    ^.className := "migration-timeline__tables",
                    tables.toVdomArray(t => <.span(^.key := t, ^.className := "migration-timeline__table", t))
                  )
              )

            val changelogPanel: VdomNode =
              <.div(
                ^.className := "migration-timeline__panel",
                <.p(^.className := "migration-timeline__panel-label", "DATABASECHANGELOG"),
                if applied.isEmpty then
                  <.p(^.className := "migration-timeline__empty", "(no changesets applied)")
                else
                  <.table(
                    ^.className := "migration-timeline__changelog",
                    <.thead(
                      <.tr(
                        <.th("ORDER"),
                        <.th("ID"),
                        <.th("AUTHOR"),
                        <.th("MD5SUM")
                      )
                    ),
                    <.tbody(
                      applied.zipWithIndex.toVdomArray { case (cs, i) =>
                        <.tr(
                          ^.key := cs.id,
                          <.td((i + 1).toString),
                          <.td(<.code(cs.id)),
                          <.td(cs.author),
                          <.td(^.className := "migration-timeline__md5", fakeChecksum(cs.id))
                        )
                      }
                    )
                  )
              )

            val sqlPanel: VdomNode =
              current match
                case Some(cs) if cs.sql.nonEmpty =>
                  <.div(
                    ^.className := "migration-timeline__sql",
                    <.p(^.className   := "migration-timeline__panel-label", s"Applying: ${cs.id}"),
                    <.pre(^.className := "migration-timeline__sql-code", cs.sql)
                  )
                case _ => EmptyVdom

            val controls: VdomNode =
              if count <= 1 then EmptyVdom
              else
                <.div(
                  ^.className := "migration-timeline__controls",
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.previous,
                    ^.disabled   := stepper.atStart,
                    ^.aria.label := "Rollback one",
                    ^.className  := "migration-timeline__button",
                    LucideIcons.ArrowLeft(LucideIcons.withClass("migration-timeline__button-icon")),
                    "Rollback"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.togglePlay,
                    ^.aria.label := (if stepper.isPlaying then "Pause" else "Play"),
                    ^.className  := "migration-timeline__button migration-timeline__button--primary",
                    if stepper.isPlaying then
                      LucideIcons.Pause(LucideIcons.withClass("migration-timeline__button-icon"))
                    else LucideIcons.Play(LucideIcons.withClass("migration-timeline__button-icon")),
                    if stepper.isPlaying then "Pause" else "Play"
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.next,
                    ^.disabled   := stepper.atEnd,
                    ^.aria.label := "Apply next",
                    ^.className  := "migration-timeline__button",
                    "Apply",
                    LucideIcons.ArrowRight(LucideIcons.withClass("migration-timeline__button-icon"))
                  ),
                  <.button(
                    ^.tpe := "button",
                    ^.onClick --> stepper.reset,
                    ^.disabled   := stepper.atStart && !stepper.isPlaying,
                    ^.aria.label := "Reset",
                    ^.className  := "migration-timeline__button migration-timeline__button--icon",
                    LucideIcons.RotateCcw(LucideIcons.withClass("migration-timeline__button-icon"))
                  ),
                  <.span(
                    ^.className := "migration-timeline__progress",
                    s"v${cursor + 1} / ${math.max(1, count)}"
                  )
                )

            <.div(
              ^.className := "migration-timeline not-prose",
              spec.title
                .map(t => <.p(^.className := "migration-timeline__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "migration-timeline__body",
                program,
                <.div(^.className := "migration-timeline__panels", schemaPanel, changelogPanel)
              ),
              sqlPanel,
              <.p(
                ^.className := "migration-timeline__caption",
                ^.aria.live := "polite",
                current.map(_.note).filter(_.nonEmpty).getOrElse(" ")
              ),
              controls
            )
      }
