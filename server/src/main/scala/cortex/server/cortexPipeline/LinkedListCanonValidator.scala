package cortex.server.cortexPipeline

import io.circe.parser
import io.circe.{Json, ParsingFailure}

import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*

/**
 * Build-time validator for `d3 widget=linked-list` payloads across every chapter markdown file under
 * `content/cortex/` (recursive walk).
 *
 * Hard-rejects the same canon the [[cortex.client.components.book.widgets.LinkedList]] widget enforces
 * at runtime (see ADR-0016): marker names ∈ `{head, tail, previous, current, next, slow, fast, dummy, start,
 * end, headA, headB, tailA, tailB}`, no `color` on payload markers, node `style` ∈ `{new, removed,
 * highlight}`, `sections[]` startIdx strictly increasing and in bounds. Runtime enforcement catches drift the
 * instant the author saves a chapter under `bin/dev`; this validator catches drift in CI for chapters nobody
 * re-opened locally.
 *
 * Wired as an sbt command alias `validateCortexPayloads` (see `build.sbt`); CI runs it alongside
 * `scalafmtCheckAll`.
 *
 * Exits 0 on a clean repo, 1 (with a list of violations) otherwise. The first arg is an optional content
 * root; defaults to `content/cortex` relative to the current working directory.
 */
object LinkedListCanonValidator:

  private val CanonicalMarkers: Set[String] =
    Set(
      "head",
      "tail",
      "previous",
      "current",
      "next",
      "slow",
      "fast",
      "dummy",
      "start",
      "end",
      "headA",
      "headB",
      "headC",
      "tailA",
      "tailB",
      "tailC"
    )

  private val CanonicalNodeStyles: Set[String] =
    Set("new", "removed", "highlight")

  // Pattern: ```d3 widget=linked-list\n<payload>\n```
  //
  // `(?ms)` enables MULTILINE so `^` / `$` match line boundaries, and DOTALL so `.` spans newlines.
  // `\R` matches any line break (LF, CRLF). The lazy `(.*?)` stops at the first ``` line — chapter
  // markdown sometimes contains other code fences before/after, so greedy matching would swallow them.
  private val WidgetBlockPattern =
    """(?ms)^```d3\s+widget=linked-list\s*\R(.*?)\R```\s*$""".r

  final case class Violation(file: Path, blockIdx: Int, msg: String):

    override def toString: String =
      s"${file.toString}: block #$blockIdx: $msg"

  def main(args: Array[String]): Unit =
    val root = args.headOption.map(Paths.get(_)).getOrElse(Paths.get("content/cortex"))
    if !Files.exists(root) then
      System.err.println(s"linked-list canon validator: content root not found: $root")
      sys.exit(2)

    val mdFiles: List[Path] =
      val stream = Files.walk(root)
      try
        stream
          .iterator()
          .asScala
          .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".md"))
          .toList
      finally stream.close()

    val violations = mdFiles.flatMap(validateFile)

    val totalBlocks = mdFiles.foldLeft(0) { (acc, f) =>
      acc + countBlocks(f)
    }

    if violations.isEmpty then
      println(
        s"linked-list canon validator: ${mdFiles.size} markdown files scanned, $totalBlocks linked-list widget block(s), no violations"
      )
      sys.exit(0)
    else
      violations.foreach(v => println(v.toString))
      println(
        s"\nlinked-list canon validator: ${violations.size} violation(s) across $totalBlocks widget block(s) in ${mdFiles.size} files"
      )
      sys.exit(1)

  private def countBlocks(file: Path): Int =
    val content = new String(Files.readAllBytes(file))
    WidgetBlockPattern.findAllMatchIn(content).size

  private def validateFile(file: Path): List[Violation] =
    val content = new String(Files.readAllBytes(file))
    WidgetBlockPattern.findAllMatchIn(content).toList.zipWithIndex.flatMap { case (m, blockIdx) =>
      val payload = m.group(1)
      validatePayload(file, blockIdx, payload)
    }

  private def validatePayload(file: Path, blockIdx: Int, payloadJson: String): List[Violation] =
    parser.parse(payloadJson) match
      // circe's parse error is always a ParsingFailure, so this covers every Left
      // (the previous `case Left(other)` fallback was unreachable — E030).
      case Left(ParsingFailure(msg, _)) =>
        List(Violation(file, blockIdx, s"JSON parse failure: $msg"))
      case Right(json) =>
        validateSpec(file, blockIdx, json)

  private def validateSpec(file: Path, blockIdx: Int, json: Json): List[Violation] =
    val violations             = scala.collection.mutable.ListBuffer.empty[Violation]
    def add(msg: String): Unit = violations += Violation(file, blockIdx, msg)

    val cur = json.hcursor

    // ── Top-level nodes ─────────────────────────────────────────────────────
    val topNodes = cur.downField("nodes").values.getOrElse(Iterable.empty).toList
    topNodes.zipWithIndex.foreach { case (node, nodeIdx) =>
      validateNodeStyle(node, s"top nodes[$nodeIdx]", add)
    }

    // ── Steps ───────────────────────────────────────────────────────────────
    val steps = cur.downField("steps").values.getOrElse(Iterable.empty).toList
    steps.zipWithIndex.foreach { case (step, stepIdx) =>
      // Per-step nodes (override)
      val stepNodes = step.hcursor.downField("nodes").values.getOrElse(Iterable.empty).toList
      stepNodes.zipWithIndex.foreach { case (node, nodeIdx) =>
        validateNodeStyle(node, s"steps[$stepIdx].nodes[$nodeIdx]", add)
      }
      // Markers
      val markers = step.hcursor.downField("markers").values.getOrElse(Iterable.empty).toList
      markers.zipWithIndex.foreach { case (marker, markerIdx) =>
        val name = marker.hcursor.downField("name").as[String].toOption.getOrElse("")
        if name.nonEmpty && !CanonicalMarkers.contains(name) then
          add(
            s"steps[$stepIdx].markers[$markerIdx]: name '$name' not in canon (expected one of: ${CanonicalMarkers.toList.sorted.mkString(", ")})"
          )
        if marker.hcursor.downField("color").succeeded then
          add(
            s"steps[$stepIdx].markers[$markerIdx] '$name': has `color` field — drop it (colour is canon-resolved, not author-controlled)"
          )
      }
    }

    // ── Sections ────────────────────────────────────────────────────────────
    val sections  = cur.downField("sections").values.getOrElse(Iterable.empty).toList
    val stepCount = steps.size
    var lastStart = -1
    sections.zipWithIndex.foreach { case (section, sIdx) =>
      val name     = section.hcursor.downField("name").as[String].toOption.getOrElse("")
      val startIdx = section.hcursor.downField("startIdx").as[Int].toOption.getOrElse(-1)
      if name.isEmpty then add(s"sections[$sIdx]: missing `name`")
      if startIdx < 0 || startIdx >= stepCount then
        add(
          s"sections[$sIdx] '$name': startIdx=$startIdx out of bounds for steps.size=$stepCount"
        )
      if startIdx <= lastStart then
        add(
          s"sections[$sIdx] '$name': startIdx=$startIdx not strictly greater than previous startIdx=$lastStart"
        )
      lastStart = startIdx
    }

    violations.toList

  private def validateNodeStyle(node: Json, path: String, add: String => Unit): Unit =
    val style = node.hcursor.downField("style").as[String].toOption.filter(_.nonEmpty)
    style.foreach { s =>
      if !CanonicalNodeStyles.contains(s) then
        add(
          s"$path: style '$s' not in canon (expected one of: ${CanonicalNodeStyles.toList.sorted.mkString(", ")})"
        )
    }
