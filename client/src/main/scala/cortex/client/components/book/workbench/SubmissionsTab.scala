package cortex.client.components.book.workbench

import cortex.client.api.ApiClient
import cortex.client.auth.AuthStore
import cortex.client.components.icons.LucideIcons
import cortex.shared.api.Endpoints.SubmissionSummary
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

/**
 * The Submissions tab of the problem workbench — the signed-in user's stored attempts for THIS problem.
 *
 * Deliberately decoupled from the editor on the right: it fetches `GET /api/submissions?book=&chapter=` on
 * mount (and after every per-row delete or sign-in), so a fresh Submit shows up the next time the tab is
 * opened rather than via cross-component state. "Current Submission" is just the newest row; "All
 * Submissions" is the full list — both as a No. / Status / Language / Code / Action table (the AI "Analyse"
 * column from the design is intentionally omitted until that feature lands). The Code eye reveals a read-only
 * Prism view of the saved source; the Action trash deletes that one row (`DELETE /api/submissions/{id}`).
 *
 * Only mounted on problem PAGES (ProblemWorkbench is handed a `book`/`chapter`); Your-Turn modals omit it.
 */
object SubmissionsTab:

  @js.native @JSImport("@markdown/runtime", "highlightWithPrism")
  private def highlightWithPrism(code: String, lang: String): String = js.native

  private given ExecutionContext = JSExecutionContext.queue

  final case class Props(book: String, chapter: String)

  /** Async load lifecycle for the list. */
  private enum Load:
    case Loading
    case Failed(message: String)
    case Loaded(rows: List[SubmissionSummary])

  private def tokenOf(snap: AuthStore.Snapshot): Option[String] = snap.status match
    case AuthStore.Status.Authed(_, token) => Some(token)
    case _                                 => None

  private def relativeTime(epochMs: Long): String =
    val seconds                  = math.max(0L, ((js.Date.now() - epochMs.toDouble) / 1000.0).toLong)
    def unit(n: Long, w: String) = s"$n $w${if n == 1 then "" else "s"} ago"
    if seconds < 60 then unit(seconds, "sec")
    else if seconds < 3600 then unit(seconds / 60, "min")
    else if seconds < 86400 then unit(seconds / 3600, "hour")
    else unit(seconds / 86400, "day")

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useState(AuthStore.current)  // (1) auth snapshot
      .useState[Load](Load.Loading) // (2) list load state
      .useState(Option.empty[Long]) // (3) submission id shown in the code view
      .useState(0)                  // (4) reload nonce — bump to refetch
      // Mirror the global auth snapshot so the tab re-renders (and refetches) on sign-in/out.
      .useEffectOnMountBy { (_, authS, _, _, _) =>
        CallbackTo {
          val unsubscribe = AuthStore.subscribe(s => authS.setState(s).runNow())
          Callback(unsubscribe())
        }
      }
      // Fetch whenever the token or the reload nonce changes. No token → nothing to fetch; the render
      // path shows the sign-in / auth-disabled message instead.
      .useEffectWithDepsBy((_, authS, _, _, reloadS) => (tokenOf(authS.value), reloadS.value)) {
        (props, _, loadS, _, _) =>
          { case (tokenOpt, _) =>
            tokenOpt match
              case None => Callback.empty
              case Some(token) =>
                loadS.setState(Load.Loading) >> Callback {
                  ApiClient.listMySubmissions(token, props.book, props.chapter).onComplete {
                    case Success(resp) =>
                      loadS.setState(Load.Loaded(resp.submissions.toList)).runNow()
                    case Failure(err) =>
                      loadS.setState(Load.Failed(Option(err.getMessage).getOrElse("Failed to load"))).runNow()
                  }
                }
          }
      }
      .render { (props, authS, loadS, selectedS, reloadS) =>
        def verdict(r: SubmissionSummary): (String, String) =
          if r.accepted then ("Accepted", "subs__status--ok")
          else (s"${r.passedCases}/${r.totalCases} passed", "subs__status--bad")

        def onDelete(token: String, id: Long): Callback = Callback {
          ApiClient.deleteOneSubmission(token, id).onComplete { _ =>
            (selectedS.setState(None) >> reloadS.modState(_ + 1)).runNow()
          }
        }

        def row(token: String, r: SubmissionSummary, no: Int): VdomNode =
          val (label, mod) = verdict(r)
          val viewing      = selectedS.value.contains(r.id)
          <.tr(
            ^.key := s"sub-${r.id}",
            <.td(^.className := "subs__cell subs__cell--no", no.toString),
            <.td(
              ^.className := "subs__cell",
              <.span(^.className := s"subs__status $mod", label),
              <.span(^.className := "subs__time", relativeTime(r.createdAtEpochMs))
            ),
            <.td(^.className := "subs__cell", <.span(^.className := "subs__lang", r.language)),
            <.td(
              ^.className := "subs__cell subs__cell--action",
              <.button(
                ^.tpe       := "button",
                ^.className := (if viewing then "subs__icon-btn subs__icon-btn--on" else "subs__icon-btn"),
                ^.title     := (if viewing then "Hide code" else "View code"),
                ^.onClick --> selectedS.setState(if viewing then None else Some(r.id)),
                LucideIcons.Eye(LucideIcons.withClass("subs__icon"))
              )
            ),
            <.td(
              ^.className := "subs__cell subs__cell--action",
              <.button(
                ^.tpe       := "button",
                ^.className := "subs__icon-btn subs__icon-btn--danger",
                ^.title     := "Delete this submission",
                ^.onClick --> onDelete(token, r.id),
                LucideIcons.Trash2(LucideIcons.withClass("subs__icon"))
              )
            )
          )

        def table(token: String, rows: List[SubmissionSummary]): VdomNode =
          <.table(
            ^.className := "subs__table",
            <.thead(
              <.tr(
                <.th(^.className := "subs__th subs__cell--no", "No."),
                <.th(^.className := "subs__th", "Status"),
                <.th(^.className := "subs__th", "Language"),
                <.th(^.className := "subs__th subs__cell--action", "Code"),
                <.th(^.className := "subs__th subs__cell--action", "Action")
              )
            ),
            <.tbody(rows.zipWithIndex.toVdomArray { case (r, i) => row(token, r, i + 1) })
          )

        def codeView(rows: List[SubmissionSummary]): VdomNode =
          selectedS.value.flatMap(id => rows.find(_.id == id)) match
            case None => EmptyVdom
            case Some(r) =>
              <.div(
                ^.className := "subs__code",
                <.div(
                  ^.className := "subs__code-head",
                  <.span(^.className := "subs__code-title", s"Submission #${r.id} · ${r.language}"),
                  <.button(
                    ^.tpe       := "button",
                    ^.className := "subs__code-close",
                    ^.title     := "Close",
                    ^.onClick --> selectedS.setState(None),
                    LucideIcons.X(LucideIcons.withClass("subs__icon"))
                  )
                ),
                <.pre(
                  ^.className               := "subs__code-pre not-prose",
                  ^.dangerouslySetInnerHtml := s"<code>${highlightWithPrism(r.source, r.language)}</code>"
                )
              )

        def loaded(token: String, rows: List[SubmissionSummary]): VdomNode =
          if rows.isEmpty then
            <.div(
              ^.className := "subs__empty",
              LucideIcons.Inbox(LucideIcons.withClass("subs__empty-icon")),
              <.p(^.className := "subs__empty-title", "No submissions yet"),
              <.p(
                ^.className := "subs__empty-body",
                "Solve it on the right, then press Submit — your attempts are saved here."
              )
            )
          else
            React.Fragment(
              <.section(
                ^.className := "subs__section",
                <.h3(^.className := "subs__heading", "Current Submission"),
                table(token, rows.take(1))
              ),
              <.section(
                ^.className := "subs__section",
                <.h3(^.className := "subs__heading", "All Submissions"),
                table(token, rows),
                <.p(
                  ^.className := "subs__count",
                  s"Showing ${rows.length} ${if rows.length == 1 then "submission" else "submissions"}"
                )
              ),
              codeView(rows)
            )

        val body: VdomNode = authS.value.status match
          case AuthStore.Status.Loading =>
            <.p(^.className := "subs__note", "Checking sign-in…")
          case AuthStore.Status.Disabled =>
            <.div(
              ^.className := "subs__note subs__note--info",
              LucideIcons.Info(LucideIcons.withClass("subs__note-icon")),
              <.span(
                "Submissions are tied to a signed-in identity, and auth is off on this server " +
                  "(AUTH_ENABLED=false). Restart dev with auth on to save and view submissions."
              )
            )
          case AuthStore.Status.Anonymous =>
            <.div(
              ^.className := "subs__note",
              <.p("Sign in to view the submissions you've saved for this problem."),
              <.button(
                ^.tpe       := "button",
                ^.className := "subs__signin",
                ^.onClick --> Callback(AuthStore.openSignIn()),
                LucideIcons.LogIn(LucideIcons.withClass("subs__icon")),
                "Sign in"
              )
            )
          case AuthStore.Status.Authed(_, token) =>
            loadS.value match
              case Load.Loading => <.p(^.className := "subs__note", "Loading submissions…")
              case Load.Failed(msg) =>
                <.div(
                  ^.className := "subs__note subs__note--err",
                  LucideIcons.AlertTriangle(LucideIcons.withClass("subs__note-icon")),
                  <.span(msg)
                )
              case Load.Loaded(rows) => loaded(token, rows)

        <.div(^.className := "subs", body)
      }
