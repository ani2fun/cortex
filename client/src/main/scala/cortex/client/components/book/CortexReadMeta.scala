package cortex.client.components.book

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * Small editorial mono strip rendered under the chapter title: `<words> words`.
 *
 * Per chat-4 user feedback the time-based signals ("4 min read", percentage indicators) were dropped — the
 * reader shouldn't feel pressure to finish in a specific time. Word count helps frame the chapter (length
 * expectation) without putting a clock on the user.
 */
object CortexReadMeta:

  /**
   * Compute words from the raw markdown body. Whitespace split; markdown punctuation is irrelevant at this
   * resolution (the difference between 1240 and 1257 words is rounding noise to the reader).
   */
  private def countWords(raw: String): Int =
    if raw == null || raw.isEmpty then 0
    else raw.trim.split("\\s+").count(_.nonEmpty)

  /** Format a word count with thousands separator — 1240 → "1,240". */
  private def formatWords(n: Int): String =
    if n < 1000 then n.toString
    else
      val s  = n.toString
      val sb = new StringBuilder
      var i  = 0
      while i < s.length do
        if i > 0 && (s.length - i) % 3 == 0 then sb.append(',')
        sb.append(s.charAt(i))
        i += 1
      sb.toString

  final case class Props(rawMarkdown: String)

  val Component = ScalaFnComponent[Props] { props =>
    val words = countWords(props.rawMarkdown)
    if words == 0 then EmptyVdom
    else
      <.div(
        ^.className  := "cortex-reader-readmeta",
        ^.aria.label := "Article metadata",
        <.span(<.strong(formatWords(words)), " words")
      )
  }
