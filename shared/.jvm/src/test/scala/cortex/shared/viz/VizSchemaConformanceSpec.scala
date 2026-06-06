package cortex.shared.viz

import zio.test.*

import java.nio.file.{Files, Path, Paths}
import scala.deriving.Mirror
import scala.compiletime.constValueTuple
import scala.jdk.CollectionConverters.*

/**
 * Phase 1 drift guard (ADR-0026) — the Scala half of the schema single-source.
 *
 * `viz-schema.yaml` is the source of truth for the viz render contract; the TS side is generated from it
 * (`npm run codegen:viz`). The Scala side stays HAND-WRITTEN in [[VizGraph]] (circe `Encoder`), so this spec
 * asserts the hand-written case-class fields match the yaml — field-for-field, per type. If someone adds a
 * field to `VizGraph.scala` without updating `viz-schema.yaml` (or vice versa), this fails, the same way the
 * retired `bin/gen-ts-types.py` drift guard worked, but now anchored on the yaml.
 *
 * Scope: NAME-level conformance (property names == case-class field names) plus the "every property is
 * required" invariant (circe always emits every field, so the yaml must mark them all required →
 * openapi-typescript emits `T`, not `T?`). It does not check types — that would be brittle; the high-value
 * drift (added/removed/renamed field) is caught here, and `types-codegen.test.ts` pins the nullable-field
 * types on the TS side.
 *
 * Uses snakeyaml (JVM-only test dep) rather than circe-yaml to avoid pulling a yaml codec into the
 * cross-compiled main sources.
 */
object VizSchemaConformanceSpec extends ZIOSpecDefault:

  /** Case-class field names, in declaration order, via the product Mirror. */
  private inline def fieldNames[T](using m: Mirror.ProductOf[T]): List[String] =
    constValueTuple[m.MirroredElemLabels].productIterator.map(_.toString).toList

  /** Locate viz-schema.yaml: walk up from the working dir (sbt runs tests at repo root). */
  private def findSchema(): Path =
    val start = Paths.get("").toAbsolutePath
    Iterator
      .iterate(start)(_.getParent)
      .takeWhile(_ != null)
      .take(6)
      .map(_.resolve("viz-schema.yaml"))
      .find(Files.exists(_))
      .getOrElse(
        throw new AssertionError(
          s"viz-schema.yaml not found walking up from $start — run tests from the repo root."
        )
      )

  /** The yaml's `components.schemas` as a name → schema-map. */
  private lazy val schemas: Map[String, java.util.Map[String, Any]] =
    val yaml    = new org.yaml.snakeyaml.Yaml()
    val content = Files.readString(findSchema())
    val root    = yaml.load[java.util.Map[String, Any]](content)
    val comps   = root.get("components").asInstanceOf[java.util.Map[String, Any]]
    val sch     = comps.get("schemas").asInstanceOf[java.util.Map[String, Any]]
    sch.asScala.view.mapValues(_.asInstanceOf[java.util.Map[String, Any]]).toMap

  private def propertyNames(typeName: String): Set[String] =
    val schema = schemas.getOrElse(typeName, throw new AssertionError(s"no schema `$typeName` in yaml"))
    val props  = schema.get("properties").asInstanceOf[java.util.Map[String, Any]]
    props.keySet().asScala.toSet

  private def requiredNames(typeName: String): Set[String] =
    val schema = schemas.getOrElse(typeName, throw new AssertionError(s"no schema `$typeName` in yaml"))
    Option(schema.get("required"))
      .map(_.asInstanceOf[java.util.List[String]].asScala.toSet)
      .getOrElse(Set.empty)

  /** One conformance check: yaml props == Scala fields, AND every prop is required. */
  private def conforms(typeName: String, scalaFields: List[String]): TestResult =
    val props    = propertyNames(typeName)
    val required = requiredNames(typeName)
    val scala    = scalaFields.toSet
    assertTrue(
      props == scala,   // names match Scala ↔ yaml
      required == props // every property required (circe always emits → matches types.ts non-optional)
    ) ?? s"$typeName: yamlProps=$props scalaFields=$scala required=$required"

  // The 10 viz render types. Adding one here + in the yaml + VizGraph.scala is
  // the deliberate three-touch when the render contract grows.
  private val ExpectedTypes = Set(
    "VizField",
    "VizNode",
    "VizEdge",
    "VizCursor",
    "Annotation",
    "VizLocal",
    "VizFrame",
    "VizGraphStep",
    "VizGraph",
    "VizCases"
  )

  override def spec: Spec[Any, Any] = suite("VizSchemaConformance")(
    test("the yaml declares exactly the expected viz render types") {
      assertTrue(schemas.keySet == ExpectedTypes) ??
        s"yaml schemas=${schemas.keySet} expected=$ExpectedTypes"
    },
    test("VizField")(conforms("VizField", fieldNames[VizField])),
    test("VizNode")(conforms("VizNode", fieldNames[VizNode])),
    test("VizEdge")(conforms("VizEdge", fieldNames[VizEdge])),
    test("VizCursor")(conforms("VizCursor", fieldNames[VizCursor])),
    test("Annotation")(conforms("Annotation", fieldNames[Annotation])),
    test("VizLocal")(conforms("VizLocal", fieldNames[VizLocal])),
    test("VizFrame")(conforms("VizFrame", fieldNames[VizFrame])),
    test("VizGraphStep")(conforms("VizGraphStep", fieldNames[VizGraphStep])),
    test("VizGraph")(conforms("VizGraph", fieldNames[VizGraph])),
    test("VizCases")(conforms("VizCases", fieldNames[VizCases]))
  )
