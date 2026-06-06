package cortex.client.components.book.widgets

import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/**
 * Shared JSON-payload decoder for D3 Widgets.
 *
 * Every widget in the catalog used to own a 30–60 line `parsePayload(json: String): Either[String, Spec]`
 * doing the same dance: `js.JSON.parse` wrapped in a `Try`, then a stack of
 * `raw.field.asInstanceOf[js.UndefOr[T]].toOption.getOrElse(default)` reads, then a `Success/Failure` match
 * that converts a thrown error into a `Left(message)` so the renderer shows an inline error placeholder
 * instead of crashing the chapter. The schema itself is widget-local — `Blocks.D3Widget` keeps payloads
 * structurally loose (ADR-0006) — but the *mechanics* of pulling typed fields out of a `js.Dynamic` are the
 * same shape every time.
 *
 * This module owns the parse envelope plus typed extension methods on `js.Dynamic`. A widget writes:
 * {{{
 *   private def parsePayload(json: String): Either[String, Spec] =
 *     PayloadDecoder.run(json) { d =>
 *       val items = d.stringList("items").getOrElse(throw missing("items"))
 *       if items.isEmpty then throw invalid("items must be non-empty")
 *       Spec(items, d.optString("title"), d.dynList("steps").map(parseStep), …)
 *     }
 * }}}
 *
 * Validation lives inside the decoder lambda — any `Throwable` it raises is caught by [[run]] and turned into
 * a `Left(message)`. The widget's `Right(spec)` branch can assume the spec passed validation, so the old
 * `Success(spec) if pred => Left(msg)` ladder collapses into one straight read.
 */
object PayloadDecoder:

  /**
   * Parse `json` then hand the rooted `js.Dynamic` to `decode`. Any thrown exception (parse error, missing
   * required field, type mismatch, validation failure) is caught and converted to `Left(message)`. The
   * fallback message `"invalid payload JSON"` is used when the throwable's `getMessage` is null or empty.
   */
  def run[T](json: String)(decode: js.Dynamic => T): Either[String, T] =
    Try(decode(js.JSON.parse(json).asInstanceOf[js.Dynamic])) match
      case Success(v) => Right(v)
      case Failure(t) => Left(Option(t.getMessage).filter(_.nonEmpty).getOrElse("invalid payload JSON"))

  /**
   * Build a missing-field exception. Widgets throw this from inside `run`'s lambda to mark a required field
   * absent; `run` converts it into the `Left` the renderer displays.
   */
  def missing(name: String): IllegalArgumentException =
    IllegalArgumentException(s"payload.$name is required")

  /**
   * Build a validation-failure exception. Widgets throw this from inside `run`'s lambda when a parsed value
   * fails a domain check (e.g., a probability outside `[0, 1)`).
   */
  def invalid(message: String): IllegalArgumentException =
    IllegalArgumentException(s"payload.$message")

  /**
   * Typed field readers on `js.Dynamic`. The `opt*` variants return `Option`; the variants with a default
   * return the underlying type. Empty strings collapse to `None` in `optString`. None of these throw — the
   * widget decides where to demand a value.
   */
  extension (d: js.Dynamic)

    def optString(name: String): Option[String] =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[String]].toOption.filter(_.nonEmpty)

    def string(name: String, default: String = ""): String =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[String]].toOption.getOrElse(default)

    def optInt(name: String): Option[Int] =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Int]].toOption

    def int(name: String, default: Int = 0): Int =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Int]].toOption.getOrElse(default)

    def optDouble(name: String): Option[Double] =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Double]].toOption

    def double(name: String, default: Double = 0.0): Double =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Double]].toOption.getOrElse(default)

    def optBool(name: String): Option[Boolean] =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Boolean]].toOption

    def bool(name: String, default: Boolean = false): Boolean =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[Boolean]].toOption.getOrElse(default)

    /** Nested object when present; `None` when the field is absent or `undefined`. */
    def optObj(name: String): Option[js.Dynamic] =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[js.Dynamic]].toOption

    /** Array of nested objects; empty list when the field is absent. Use `.map(parseFoo)` to decode. */
    def dynList(name: String): List[js.Dynamic] =
      d.selectDynamic(name)
        .asInstanceOf[js.UndefOr[js.Array[js.Dynamic]]]
        .toOption
        .fold(List.empty[js.Dynamic])(_.toList)

    /** Array of primitives coerced to strings via JS `String(v)`. `None` when the field is absent. */
    def stringList(name: String): Option[List[String]] =
      d.selectDynamic(name)
        .asInstanceOf[js.UndefOr[js.Array[js.Any]]]
        .toOption
        .map(_.toList.map(v => js.Dynamic.global.String(v).asInstanceOf[String]))

    /** Array of primitives coerced to ints. `None` when the field is absent. */
    def intList(name: String): Option[List[Int]] =
      d.selectDynamic(name)
        .asInstanceOf[js.UndefOr[js.Array[Int]]]
        .toOption
        .map(_.toList)

    /**
     * Two-dimensional array of primitives → `List[List[String]]`. Each row is coerced like `stringList`
     * (every element through JS `String(v)`). `None` when the field is absent. Used by widgets with row ×
     * column grid payloads (array-traversal `layout:"2d"`).
     */
    def stringMatrix(name: String): Option[List[List[String]]] =
      d.selectDynamic(name)
        .asInstanceOf[js.UndefOr[js.Array[js.Array[js.Any]]]]
        .toOption
        .map(_.toList.map(_.toList.map(v => js.Dynamic.global.String(v).asInstanceOf[String])))

    /**
     * Two-element `[lo, hi]` array → `(Int, Int)`. JS numbers are doubles; the truncation matches the widget
     * convention of expressing ranges as JSON integers but reading them as doubles defensively. Returns
     * `default` when the field is absent, malformed, or has fewer than 2 elements.
     */
    def intRange(name: String, default: (Int, Int)): (Int, Int) =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[js.Array[Double]]].toOption.map(_.toList) match
        case Some(lo :: hi :: _) => (lo.toInt, hi.toInt)
        case _                   => default

    /**
     * Two-element `[lo, hi]` array → `(Double, Double)`. Returns `default` on any non-conforming input.
     */
    def doubleRange(name: String, default: (Double, Double)): (Double, Double) =
      d.selectDynamic(name).asInstanceOf[js.UndefOr[js.Array[Double]]].toOption.map(_.toList) match
        case Some(lo :: hi :: _) => (lo, hi)
        case _                   => default
