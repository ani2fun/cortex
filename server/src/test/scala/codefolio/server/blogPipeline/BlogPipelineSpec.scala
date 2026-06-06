package codefolio.server.blogPipeline

import zio.*
import zio.test.*

object BlogPipelineSpec extends ZIOSpecDefault:

  private val PostA =
    """---
      |title: Older Post
      |summary: An older entry.
      |publishedAt: 2025-01-01
      |tags: [scala, zio]
      |readMinutes: 8
      |eyebrow: First Steps
      |meta: Read Time=8 min; Difficulty=Beginner
      |---
      |Body of older post.
      |""".stripMargin

  private val PostB =
    """---
      |title: Newer Post
      |summary: A newer entry.
      |publishedAt: 2026-05-10
      |tags: [zio, http]
      |readMinutes: 12
      |---
      |Body of newer post.
      |""".stripMargin

  override def spec: Spec[Any, BlogFailure] = suite("BlogPipeline")(
    test("builds an index sorted descending by publishedAt and returns a post payload with pager links") {
      for
        pipeline <- BlogPipeline.from(
          FakeBlogFs(FakeBlogFs.Snapshot(mtime = 1L, posts = List("older" -> PostA, "newer" -> PostB))),
          autoReload = false
        )
        idx     <- pipeline.index
        payload <- pipeline.post("older")
      yield assertTrue(
        // Newer post first.
        idx.posts.map(_.slug) == List("newer", "older"),
        idx.posts.head.title == "Newer Post",
        idx.posts(1).title == "Older Post",
        idx.posts(1).readMinutes == Some(8),
        idx.posts(1).eyebrow == Some("First Steps"),
        idx.posts(1).tags == Some(Seq("scala", "zio")),
        // Older post: prev = nothing (it's the oldest), next = newer.
        payload.prevSlug.isEmpty,
        payload.nextSlug == Some("newer"),
        payload.frontmatter.title == "Older Post",
        payload.frontmatter.publishedAt == "2025-01-01",
        payload.frontmatter.meta.exists(_.exists(m => m.label == "Read Time" && m.value == "8 min")),
        payload.raw.trim == "Body of older post."
      )
    },
    test("nextSlug is None for the newest post and prevSlug points to the older one") {
      for
        pipeline <- BlogPipeline.from(
          FakeBlogFs(FakeBlogFs.Snapshot(mtime = 1L, posts = List("older" -> PostA, "newer" -> PostB))),
          autoReload = false
        )
        payload <- pipeline.post("newer")
      yield assertTrue(
        payload.prevSlug == Some("older"),
        payload.nextSlug.isEmpty
      )
    },
    test("rejects path-traversal-shaped slugs without touching the seam") {
      for
        pipeline <- BlogPipeline.from(
          FakeBlogFs(FakeBlogFs.Snapshot(mtime = 1L, posts = List("safe" -> "---\ntitle: Safe\n---\nBody"))),
          autoReload = false
        )
        result <- pipeline.post("../../etc/passwd").either
      yield assertTrue(result == Left(BlogFailure.NotFound))
    },
    test("falls back to humanised slug when frontmatter is missing") {
      for
        pipeline <- BlogPipeline.from(
          FakeBlogFs(FakeBlogFs.Snapshot(
            mtime = 1L,
            posts = List("untitled-post" -> "Just body, no fence.")
          )),
          autoReload = false
        )
        idx     <- pipeline.index
        payload <- pipeline.post("untitled-post")
      yield assertTrue(
        idx.posts.head.title == "Untitled Post",
        payload.frontmatter.title == "Untitled Post",
        payload.frontmatter.publishedAt == "",
        payload.raw == "Just body, no fence."
      )
    },
    test("treats unterminated frontmatter as plain body (lenient parsing)") {
      val malformed =
        """---
          |title: Half-Broken
          |publishedAt: 2025-06-01
          |
          |Body without closing fence.
          |""".stripMargin
      for
        pipeline <- BlogPipeline.from(
          FakeBlogFs(FakeBlogFs.Snapshot(mtime = 1L, posts = List("half-broken" -> malformed))),
          autoReload = false
        )
        payload <- pipeline.post("half-broken")
      yield assertTrue(
        payload.frontmatter.title == "Half Broken",
        payload.raw == malformed
      )
    },
    test("rebuilds the index when mtime advances and autoReload=true") {
      for
        fs <- ZIO.succeed(
          FakeBlogFs(FakeBlogFs.Snapshot(
            mtime = 1L,
            posts = List("a" -> "---\ntitle: A\npublishedAt: 2025-01-01\n---\nA")
          ))
        )
        pipeline <- BlogPipeline.from(fs, autoReload = true)
        first    <- pipeline.index
        _ = fs.advance(
          2L,
          List(
            "a" -> "---\ntitle: A\npublishedAt: 2025-01-01\n---\nA",
            "b" -> "---\ntitle: B\npublishedAt: 2026-01-01\n---\nB"
          )
        )
        second <- pipeline.index
      yield assertTrue(
        first.posts.map(_.slug) == List("a"),
        second.posts.map(_.slug) == List("b", "a"),
        fs.loadPostsCalls == 2
      )
    },
    test("serves from cache when mtime is unchanged") {
      for
        fs <- ZIO.succeed(
          FakeBlogFs(FakeBlogFs.Snapshot(
            mtime = 1L,
            posts = List("a" -> "---\ntitle: A\npublishedAt: 2025-01-01\n---\nA")
          ))
        )
        pipeline <- BlogPipeline.from(fs, autoReload = true)
        _        <- pipeline.index
        _        <- pipeline.index
        _        <- pipeline.index
      yield assertTrue(fs.loadPostsCalls == 1)
    }
  )
