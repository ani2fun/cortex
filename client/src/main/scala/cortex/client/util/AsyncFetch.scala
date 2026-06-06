package cortex.client.util

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.html_<^.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Three-state ADT for an in-flight `Future[T]`. Pages render by branching on the result; the page never sees
 * the underlying `Option[Either[String, T]]` state shape.
 *
 * The companion carries the rendering helper [[AsyncResult.render]] plus the default Loading / Errored VDOM —
 * concentrated here so adding a skeleton loader, an `aria-busy` attribute, or a retry button is a one-place
 * change instead of a six-page sweep.
 */
enum AsyncResult[+T]:
  case Loading
  case Errored(message: String)
  case Loaded(value: T)

object AsyncResult:

  /**
   * Inline "Loading…" line styled to match the Cortex reader. Pages that need a full-page treatment override.
   */
  val defaultLoading: VdomNode =
    <.p(^.className := "cortex__status", "Loading…")

  /** Centered destructive-text line; neutral default for any page that doesn't have its own error chrome. */
  def defaultError(message: String): VdomNode =
    <.p(^.className := "text-center text-destructive", message)

  extension [T](r: AsyncResult[T])

    /**
     * Branch over the three states with an optional override for Loading / Errored. The success renderer is
     * always required; the defaults handle the common case so pages can collapse to "render the loaded value
     * and let the helper take care of the rest."
     */
    def render(
        loaded: T => VdomNode,
        loading: VdomNode = defaultLoading,
        errored: String => VdomNode = defaultError
    ): VdomNode = r match
      case AsyncResult.Loading      => loading
      case AsyncResult.Errored(msg) => errored(msg)
      case AsyncResult.Loaded(v)    => loaded(v)

/**
 * State-machine helper for "fetch a `Future[T]` and render the loading/error/success branches" — the pattern
 * that previously lived inline in six client call sites (every page that hits the API and one home-page
 * section). Pages now write a `useState(AsyncFetch.initial[T])` + `useEffect(... AsyncFetch.run ...)` shape;
 * the state-machine wiring (reset-to-Loading on deps change, `Throwable.getMessage` fallback, error-prefix
 * formatting) lives only here.
 */
object AsyncFetch:

  /** Initial value for `.useState(...)`. The state-machine starts in Loading. */
  def initial[T]: AsyncResult[T] = AsyncResult.Loading

  /**
   * Build the `Callback` for the effect block: reset to Loading, fire the fetch, fold success/failure into
   * `AsyncResult` via the state setter. An optional `onLoaded` runs after a successful state update — used
   * for post-fetch side effects (e.g., setting the page title from a freshly-loaded payload, redirecting
   * after a lookup) so they ride the same fetch instead of needing a second effect chained on state-value.
   *
   * `errorPrefix` is prepended to the throwable's message; the empty/null `getMessage` case is replaced with
   * "unknown error" so the user always sees something readable.
   */
  def run[T](
      setState: AsyncResult[T] => Callback,
      fetch: => Future[T],
      errorPrefix: String = "Failed to load",
      onLoaded: T => Callback = (_: T) => Callback.empty
  )(using ec: ExecutionContext): Callback =
    setState(AsyncResult.Loading) >> Callback {
      fetch.onComplete {
        case Success(v) =>
          setState(AsyncResult.Loaded(v)).runNow()
          onLoaded(v).runNow()
        case Failure(t) =>
          val msg  = Option(t.getMessage).filter(_.nonEmpty).getOrElse("unknown error")
          val full = if errorPrefix.isEmpty then msg else s"$errorPrefix: $msg"
          setState(AsyncResult.Errored(full)).runNow()
      }
    }
