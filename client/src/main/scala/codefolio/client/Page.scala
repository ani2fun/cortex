package codefolio.client

/**
 * All routes the SPA knows about. The router picks one based on the URL.
 *
 * Path-parameter cases (`Chapter`, `BookRedirect`, `BlogPost`) are case classes so scalajs-react's
 * `caseClass[…]` Router DSL can pack/unpack them.
 */
sealed trait Page

object Page:
  case object Home                                        extends Page
  case object CortexIndex                                 extends Page
  final case class BookRedirect(book: String)             extends Page
  final case class Chapter(book: String, chapter: String) extends Page
  case object Blogs                                       extends Page
  final case class BlogPost(slug: String)                 extends Page
  case object Demo                                        extends Page
