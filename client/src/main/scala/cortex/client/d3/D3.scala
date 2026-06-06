package cortex.client.d3

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Minimum-viable Scala.js facade over [[https://d3js.org/ d3]].
 *
 * Scoped to the surface area widgets actually use today: top-level `select`/`selectAll`, easing functions,
 * and the `Selection` / `Transition` chains. Growing this is encouraged — but only when a new widget needs a
 * binding, not pre-emptively. D3's API is large and most of it stays unused. See ADR-0013.
 *
 * Pattern:
 *   - Static functions on the `D3` object are pulled from `d3`'s named exports.
 *   - `Selection` and `Transition` are `@js.native` traits returning themselves for chaining.
 *   - Values that accept either a literal or a function-of-`(datum, index)` are typed `js.Any` — the runtime
 *     accepts both. Authors using the facade should pass typed JS functions (`js.Function2[D, Int, R]`) where
 *     they want to derive an attribute from data.
 */
object D3:

  // Note: `selection.prototype.transition` and `.interrupt` are wired up by
  // d3-transition's side-effect import, which lives in [`client/main.js`].
  // It has to be there, not here — a Scala.js `@JSImport(_, Namespace)`
  // reference to `d3-transition` from this object gets DCE'd by the JS
  // linker because the val is never read, and d3's barrel re-export goes
  // through d3-selection which is marked `"sideEffects": false`, so the
  // bundler tree-shakes the path that would touch d3-transition's index.js.

  @js.native @JSImport("d3", "select")
  def select(element: dom.Element): Selection = js.native

  @js.native @JSImport("d3", "selectAll")
  def selectAll(selector: String): Selection = js.native

  @js.native @JSImport("d3", "easeCubicInOut")
  val easeCubicInOut: js.Function1[Double, Double] = js.native

  @js.native @JSImport("d3", "easeQuadInOut")
  val easeQuadInOut: js.Function1[Double, Double] = js.native

  @js.native
  trait Selection extends js.Object:
    def select(selector: String): Selection                                                    = js.native
    def selectAll(selector: String): Selection                                                 = js.native
    def append(tag: String): Selection                                                         = js.native
    def remove(): Selection                                                                    = js.native
    def data(data: js.Array[? <: js.Any]): Selection                                           = js.native
    def data(data: js.Array[? <: js.Any], keyFn: js.Function2[js.Any, Int, js.Any]): Selection = js.native
    def enter(): Selection                                                                     = js.native
    def exit(): Selection                                                                      = js.native
    def join(tag: String): Selection                                                           = js.native
    def attr(name: String, value: js.Any): Selection                                           = js.native
    def text(value: js.Any): Selection                                                         = js.native
    def html(value: String): Selection                                                         = js.native
    def style(name: String, value: js.Any): Selection                                          = js.native
    def classed(name: String, value: Boolean): Selection                                       = js.native
    def classed(name: String, value: js.Function2[js.Any, Int, Boolean]): Selection            = js.native
    def each(fn: js.Function2[js.Any, Int, Unit]): Selection                                   = js.native
    def call(fn: js.Function1[Selection, Any]): Selection                                      = js.native
    def transition(): Transition                                                               = js.native
    // Named transitions don't interrupt each other on the same element — use
    // distinct names (e.g. "fade" and "move") when running concurrent
    // transitions on overlapping selections, otherwise the second one
    // cancels the first by D3 default-namespace rules.
    def transition(name: String): Transition = js.native
    def node(): dom.Element                  = js.native
    def empty(): Boolean                     = js.native
    def size(): Int                          = js.native

  @js.native
  trait Transition extends js.Object:
    def duration(ms: Double): Transition                   = js.native
    def delay(ms: Double): Transition                      = js.native
    def ease(fn: js.Function1[Double, Double]): Transition = js.native
    def attr(name: String, value: js.Any): Transition      = js.native
    def style(name: String, value: js.Any): Transition     = js.native
    def selection(): Selection                             = js.native
    def end(): js.Promise[Unit]                            = js.native
    // Removes each selected element at the end of the transition (after
    // duration + delay). Used for fade-out + cleanup of exiting selections —
    // pairs with an `attr("opacity", 0)` to fade nodes out before they vanish.
    def remove(): Transition = js.native
