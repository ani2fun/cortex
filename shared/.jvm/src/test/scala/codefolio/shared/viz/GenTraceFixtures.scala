package codefolio.shared.viz

import io.circe.syntax.*
import codefolio.shared.viz.VizGraph.given // bring the VizCases / VizGraph Encoders into scope
import java.nio.file.{Files, Path}
import java.nio.charset.StandardCharsets

/**
 * Phase 0 — emit each [[HeapTraceFixtures]] entry as a `VizCases` JSON file, ready for the Playwright
 * trace-shapes tests to fetch and render (ADR-0025). The JSON is the bit-exact payload the d3 renderer
 * receives in production (after `HeapToGraph.adapt` runs in the browser).
 *
 * Invoked via the sbt command alias `genTraceFixtures`:
 *
 * {{{
 *   sbt genTraceFixtures
 *   // writes client/test/e2e/fixtures/<name>.json for each fixture in HeapTraceFixtures.all
 * }}}
 *
 * The command's working directory is the build root (per `Compile / run / baseDirectory`), so we resolve
 * outputs relative to `client/test/e2e/fixtures/`. The directory is created on first run.
 */
object GenTraceFixtures:

  private val OutputDir: Path = Path.of("client", "test", "e2e", "fixtures")

  def main(args: Array[String]): Unit =
    Files.createDirectories(OutputDir)

    val rows = HeapTraceFixtures.all.map { f =>
      HeapToGraph.adapt(
        trace = f.trace,
        source = f.source,
        layoutHint = f.layoutHint,
        rootHint = f.rootHint,
        vizCase = None,
        title = f.title,
        vizKind = f.vizKind
      ) match
        case Left(msg) =>
          System.err.println(s"[genTraceFixtures] FAIL ${f.name}: $msg")
          sys.exit(1)
        case Right(cases) =>
          val json    = cases.asJson.noSpaces
          val outPath = OutputDir.resolve(s"${f.name}.json")
          Files.writeString(outPath, json, StandardCharsets.UTF_8)
          (f.name, cases.cases.size, cases.cases.flatMap(_.steps).size, json.length)
    }

    rows.foreach { case (name, cases, steps, bytes) =>
      println(s"[genTraceFixtures] wrote $name: $cases case(s), $steps step(s), $bytes bytes")
    }
    println(s"[genTraceFixtures] ${rows.size} fixture(s) emitted to $OutputDir")
