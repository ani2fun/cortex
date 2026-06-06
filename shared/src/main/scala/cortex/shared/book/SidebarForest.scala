package cortex.shared.book

import cortex.shared.api.Endpoints.ChapterRef

/**
 * Pure tree builder for the Cortex reader's left-panel **Sidebar Forest** (CONTEXT.md term).
 *
 * Takes a flat sequence of `ChapterRef` (each carrying an optional `groupPath`) and assembles them into a
 * forest where Sections nest other Nodes and Leaves wrap individual Chapters. Section ordering is
 * first-seen-wins within each level — preserving the server-side traversal order, which is
 * numeric-prefix-sorted.
 *
 * Lives in `shared` so the React component renders it and JVM tests can assert tree shape directly.
 */
object SidebarForest:

  sealed trait Node:
    def containsActive(activeSlug: String): Boolean

  final case class Section(title: String, depth: Int, children: List[Node]) extends Node:
    def containsActive(activeSlug: String): Boolean = children.exists(_.containsActive(activeSlug))

  final case class Leaf(chapter: ChapterRef) extends Node:
    def containsActive(activeSlug: String): Boolean = chapter.slug == activeSlug

  /**
   * Group chapters into a forest by their `groupPath`. Section ordering is determined by first-seen-wins
   * within each level — preserving the server-side traversal order, which is numeric-prefix-sorted.
   */
  def build(chapters: Seq[ChapterRef]): List[Node] =
    val root = scala.collection.mutable.LinkedHashMap.empty[String, Either[Leaf, SectionBuilder]]
    chapters.foreach { ch =>
      val path = ch.groupPath.getOrElse(Seq.empty)
      if path.isEmpty then
        // Leaves at the root use their slug as the LinkedHashMap key (unique per book by construction).
        root.update(s"leaf:${ch.slug}", Left(Leaf(ch)))
      else
        val first = path.head
        val key   = s"sec:$first"
        val sec = root.get(key) match
          case Some(Right(sb)) => sb
          case _ =>
            val sb = SectionBuilder(first, depth = 0)
            root.update(key, Right(sb))
            sb
        sec.insert(path.tail.toList, ch)
    }
    root.values.iterator.map {
      case Left(leaf) => leaf
      case Right(sb)  => sb.build
    }.toList

  /** Mutable builder — internal helper for [[build]]. */
  final private class SectionBuilder(title: String, depth: Int):

    private val children =
      scala.collection.mutable.LinkedHashMap.empty[String, Either[Leaf, SectionBuilder]]

    def insert(remainingPath: List[String], ch: ChapterRef): Unit =
      remainingPath match
        case Nil =>
          children.update(s"leaf:${ch.slug}", Left(Leaf(ch)))
        case head :: rest =>
          val key = s"sec:$head"
          val sub = children.get(key) match
            case Some(Right(sb)) => sb
            case _ =>
              val sb = SectionBuilder(head, depth + 1)
              children.update(key, Right(sb))
              sb
          sub.insert(rest, ch)

    def build: Section =
      Section(
        title,
        depth,
        children.values.iterator.map {
          case Left(leaf) => leaf
          case Right(sb)  => sb.build
        }.toList
      )
