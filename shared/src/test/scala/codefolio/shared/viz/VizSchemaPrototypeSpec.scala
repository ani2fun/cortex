package codefolio.shared.viz

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import zio.test.*

/**
 * Phase 0 deliverable 8 / gap 4 (ADR-0025) — Scala-side round-trip prototype.
 *
 * This spec proves that the `oneOf` + `discriminator` pattern round-trips cleanly through `circe` — i.e. that
 * **circe itself** can encode/decode the discriminated unions. The case classes + codecs below are
 * HAND-WRITTEN.
 *
 * IMPORTANT (post-audit correction): this spec does NOT prove that `sbt-openapi-codegen` *emits* working
 * codecs. When the real plugin (1.11.22) was run against `viz-schema-prototype.yaml`, it did NOT produce
 * compiling Scala (broken tapir Schema file referencing phantom `*Kind` types; circe serdes only emitted for
 * endpoint bodies; tuple-as-array unsupported; enum codec needs an extra dependency). See ADR-0025
 * §"Schema-design prototype" for the full table. The Phase-1 recommendation is therefore yaml → TS codegen +
 * hand-written Scala + circe with a conformance test, NOT plugin-generated Scala.
 *
 * Two known divergences between this spec and the real schema, kept because the spec still serves its narrow
 * purpose (circe CAN round-trip discriminated unions): (1) tuples here are array-encoded `[name, value]`, but
 * the real schema must use object encoding `{name, value}` (array-of-oneOf fails codegen); (2)
 * `HeapScalar.Null` here uses the string `"Null"`, matching the (now quote-fixed) yaml.
 */
object VizSchemaPrototypeSpec extends ZIOSpecDefault:

  // ── Scala types — mirror viz-schema-prototype.yaml exactly ──────────────

  sealed trait HeapScalarP

  object HeapScalarP:
    final case class I(value: Long)    extends HeapScalarP
    final case class D(value: Double)  extends HeapScalarP
    final case class B(value: Boolean) extends HeapScalarP
    final case class S(value: String)  extends HeapScalarP
    case object NullV                  extends HeapScalarP

  sealed trait HeapValueP

  object HeapValueP:
    final case class Scalar(value: HeapScalarP) extends HeapValueP
    final case class Ref(id: String)            extends HeapValueP

  sealed trait HeapObjectP

  object HeapObjectP:
    /** Tuple encoded as JSON array `[name, value]`. */
    final case class Instance(cls: String, fields: List[(String, HeapValueP)]) extends HeapObjectP
    final case class Arr(arrKind: String, items: List[HeapValueP])             extends HeapObjectP
    final case class Dict(entries: List[(HeapValueP, HeapValueP)])             extends HeapObjectP

  // ── Codecs — discriminator-style, mirroring codegen output ──────────────

  given Encoder[HeapScalarP] with

    def apply(s: HeapScalarP): Json = s match
      case HeapScalarP.I(v)  => Json.obj("kind" -> "I".asJson, "value" -> v.asJson)
      case HeapScalarP.D(v)  => Json.obj("kind" -> "D".asJson, "value" -> v.asJson)
      case HeapScalarP.B(v)  => Json.obj("kind" -> "B".asJson, "value" -> v.asJson)
      case HeapScalarP.S(v)  => Json.obj("kind" -> "S".asJson, "value" -> v.asJson)
      case HeapScalarP.NullV => Json.obj("kind" -> "Null".asJson)

  given Decoder[HeapScalarP] with

    def apply(c: HCursor): Decoder.Result[HeapScalarP] =
      c.downField("kind").as[String].flatMap {
        case "I"    => c.downField("value").as[Long].map(HeapScalarP.I.apply)
        case "D"    => c.downField("value").as[Double].map(HeapScalarP.D.apply)
        case "B"    => c.downField("value").as[Boolean].map(HeapScalarP.B.apply)
        case "S"    => c.downField("value").as[String].map(HeapScalarP.S.apply)
        case "Null" => Right(HeapScalarP.NullV)
        case k      => Left(DecodingFailure(s"Unknown HeapScalar kind: $k", c.history))
      }

  given Encoder[HeapValueP] with

    def apply(v: HeapValueP): Json = v match
      case HeapValueP.Scalar(s) => Json.obj("kind" -> "Scalar".asJson, "value" -> s.asJson)
      case HeapValueP.Ref(id)   => Json.obj("kind" -> "Ref".asJson, "id" -> id.asJson)

  given Decoder[HeapValueP] with

    def apply(c: HCursor): Decoder.Result[HeapValueP] =
      c.downField("kind").as[String].flatMap {
        case "Scalar" => c.downField("value").as[HeapScalarP].map(HeapValueP.Scalar.apply)
        case "Ref"    => c.downField("id").as[String].map(HeapValueP.Ref.apply)
        case k        => Left(DecodingFailure(s"Unknown HeapValue kind: $k", c.history))
      }

  // Tuples encoded as 2-element JSON arrays.
  private given Encoder[(String, HeapValueP)] with
    def apply(t: (String, HeapValueP)): Json = Json.arr(t._1.asJson, t._2.asJson)

  private given Decoder[(String, HeapValueP)] with

    def apply(c: HCursor): Decoder.Result[(String, HeapValueP)] =
      for
        arr <- c.values.toRight(DecodingFailure("expected array tuple", c.history))
        v <- arr.toList match
          case k :: v :: Nil =>
            for
              sk <- k.as[String]
              sv <- v.as[HeapValueP]
            yield (sk, sv)
          case _ => Left(DecodingFailure("expected [string, HeapValue]", c.history))
      yield v

  private given Encoder[(HeapValueP, HeapValueP)] with
    def apply(t: (HeapValueP, HeapValueP)): Json = Json.arr(t._1.asJson, t._2.asJson)

  private given Decoder[(HeapValueP, HeapValueP)] with

    def apply(c: HCursor): Decoder.Result[(HeapValueP, HeapValueP)] =
      for
        arr <- c.values.toRight(DecodingFailure("expected array tuple", c.history))
        v <- arr.toList match
          case k :: v :: Nil =>
            for
              kv <- k.as[HeapValueP]
              vv <- v.as[HeapValueP]
            yield (kv, vv)
          case _ => Left(DecodingFailure("expected [HeapValue, HeapValue]", c.history))
      yield v

  given Encoder[HeapObjectP] with

    def apply(o: HeapObjectP): Json = o match
      case HeapObjectP.Instance(cls, fields) =>
        Json.obj("kind" -> "Instance".asJson, "cls" -> cls.asJson, "fields" -> fields.asJson)
      case HeapObjectP.Arr(arrKind, items) =>
        Json.obj("kind" -> "Arr".asJson, "arrKind" -> arrKind.asJson, "items" -> items.asJson)
      case HeapObjectP.Dict(entries) =>
        Json.obj("kind" -> "Dict".asJson, "entries" -> entries.asJson)

  given Decoder[HeapObjectP] with

    def apply(c: HCursor): Decoder.Result[HeapObjectP] =
      c.downField("kind").as[String].flatMap {
        case "Instance" =>
          for
            cls    <- c.downField("cls").as[String]
            fields <- c.downField("fields").as[List[(String, HeapValueP)]]
          yield HeapObjectP.Instance(cls, fields)
        case "Arr" =>
          for
            ak    <- c.downField("arrKind").as[String]
            items <- c.downField("items").as[List[HeapValueP]]
          yield HeapObjectP.Arr(ak, items)
        case "Dict" =>
          c.downField("entries").as[List[(HeapValueP, HeapValueP)]].map(HeapObjectP.Dict.apply)
        case k => Left(DecodingFailure(s"Unknown HeapObject kind: $k", c.history))
      }

  // ── Canonical sample from the plan (gap 4 description) ─────────────────

  private val CanonicalJson: String =
    """{"kind":"Instance","cls":"TreeNode","fields":[["val",{"kind":"Scalar","value":{"kind":"I","value":1}}]]}"""

  private val CanonicalValue: HeapObjectP =
    HeapObjectP.Instance(
      "TreeNode",
      List("val" -> HeapValueP.Scalar(HeapScalarP.I(1)))
    )

  override def spec: Spec[Any, Any] = suite("VizSchemaPrototype (oneOf round-trip)")(
    test("the canonical Instance JSON decodes via circe") {
      val decoded = decode[HeapObjectP](CanonicalJson)
      assertTrue(decoded == Right(CanonicalValue))
    },
    test("the canonical value re-encodes to the canonical JSON shape") {
      val encoded = CanonicalValue.asJson.noSpaces
      // We don't byte-equate (key order can vary); we round-trip-decode + compare.
      val redecoded = decode[HeapObjectP](encoded)
      assertTrue(redecoded == Right(CanonicalValue))
    },
    test("Arr variant round-trips with arrKind discriminator preserved") {
      val v: HeapObjectP = HeapObjectP.Arr(
        "Lst",
        List(HeapValueP.Ref("n1"), HeapValueP.Scalar(HeapScalarP.NullV))
      )
      val encoded   = v.asJson.noSpaces
      val redecoded = decode[HeapObjectP](encoded)
      assertTrue(redecoded == Right(v))
    },
    test("Dict variant round-trips with tuple entries") {
      val v: HeapObjectP = HeapObjectP.Dict(
        List(
          HeapValueP.Scalar(HeapScalarP.S("a")) -> HeapValueP.Ref("n1"),
          HeapValueP.Scalar(HeapScalarP.I(2L))  -> HeapValueP.Scalar(HeapScalarP.B(true))
        )
      )
      val encoded   = v.asJson.noSpaces
      val redecoded = decode[HeapObjectP](encoded)
      assertTrue(redecoded == Right(v))
    },
    test("all 5 HeapScalar variants round-trip") {
      val cases: List[HeapScalarP] = List(
        HeapScalarP.I(42L),
        HeapScalarP.D(3.14),
        HeapScalarP.B(false),
        HeapScalarP.S("hi"),
        HeapScalarP.NullV
      )
      val results = cases.map(c => decode[HeapScalarP](c.asJson.noSpaces))
      assertTrue(results == cases.map(Right.apply))
    },
    test("unknown kind discriminator is rejected with a clean error") {
      val bad = """{"kind":"NotAThing","value":1}"""
      val r   = decode[HeapScalarP](bad)
      assertTrue(r.isLeft, r.left.exists(_.getMessage.contains("NotAThing")))
    }
  )
