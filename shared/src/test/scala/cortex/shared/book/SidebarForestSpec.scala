package cortex.shared.book

import cortex.shared.api.Endpoints.ChapterRef
import cortex.shared.book.SidebarForest.{Leaf, Section}
import zio.test.*

object SidebarForestSpec extends ZIOSpecDefault:

  private def chapter(slug: String, group: List[String] = Nil): ChapterRef =
    ChapterRef(
      slug = slug,
      title = slug.capitalize,
      groupPath = if group.isEmpty then None else Some(group)
    )

  override def spec: Spec[Any, Any] = suite("SidebarForest")(
    test("flat chapters become root-level Leaves") {
      val forest = SidebarForest.build(Seq(chapter("intro"), chapter("setup")))
      assertTrue(
        forest.size == 2,
        forest.head.isInstanceOf[Leaf],
        forest(1).isInstanceOf[Leaf]
      )
    },
    test("chapters with the same first group merge into one Section") {
      val forest = SidebarForest.build(
        Seq(
          chapter("data-arrays", List("Data Structures")),
          chapter("data-lists", List("Data Structures"))
        )
      )
      forest match
        case Section("Data Structures", 0, children) :: Nil =>
          assertTrue(children.size == 2, children.forall(_.isInstanceOf[Leaf]))
        case _ => assertTrue(false)
    },
    test("nested groups become nested Sections with increasing depth") {
      val forest = SidebarForest.build(Seq(chapter("a-b-c", List("A", "B"))))
      forest match
        case Section("A", 0, Section("B", 1, (_: Leaf) :: Nil) :: Nil) :: Nil =>
          assertTrue(true)
        case _ => assertTrue(false)
    },
    test("root mixes leaves and sections in first-seen order") {
      val forest = SidebarForest.build(
        Seq(
          chapter("intro"),
          chapter("ds-arr", List("DS")),
          chapter("ds-lists", List("DS")),
          chapter("conclusion")
        )
      )
      assertTrue(
        forest.size == 3,
        forest(0).isInstanceOf[Leaf],
        forest(1).isInstanceOf[Section],
        forest(2).isInstanceOf[Leaf]
      )
    },
    test("containsActive walks the tree to find the active slug") {
      val forest = SidebarForest.build(
        Seq(
          chapter("a-b", List("A", "B")),
          chapter("c")
        )
      )
      assertTrue(
        forest.head.containsActive("a-b"),
        !forest.head.containsActive("c"),
        forest(1).containsActive("c"),
        !forest(1).containsActive("a-b")
      )
    }
  )
