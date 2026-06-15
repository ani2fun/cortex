package cortex.client.components.book.workbench

import cortex.client.components.book.BlockDiscovery
import cortex.shared.book.Block
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

/**
 * ChapterPage's `kind: problem` host. Problem-mode markdown renders to a single `.problem-workbench`
 * placeholder; this component parses it out of the HTML on a DETACHED element (never inserted into the
 * document) and renders [[ProblemWorkbench]] as a normal React child, injecting the page header data (title,
 * difficulty badge, topic pills) that only the frontmatter knows — packaged HTML can't carry it.
 */
object ProblemPageHost:

  final case class Props(
      html: String,
      title: String,
      difficulty: Option[String],
      topics: List[String],
      book: String,
      chapter: String
  )

  private def parseBlock(html: String): Option[Block.ProblemWorkbench] =
    val holder = dom.document.createElement("div").asInstanceOf[dom.html.Element]
    holder.innerHTML = html
    BlockDiscovery.discover(holder).blocks.collectFirst { case (_, pw: Block.ProblemWorkbench) =>
      pw
    }

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.html)(props => _ => parseBlock(props.html))
      .render { (props, parsed) =>
        parsed.value match
          case Some(pw) =>
            ProblemWorkbench.Component(
              ProblemWorkbench.Props(
                title = props.title,
                difficulty = props.difficulty,
                topics = props.topics,
                descriptionHtml = pw.descriptionHtml,
                sections = pw.sections,
                tabs = pw.tabs,
                spec = pw.spec,
                heightCss = "calc(100vh - 10rem)",
                submitCtx = Some(WorkbenchEditor.SubmitContext(props.book, props.chapter)),
                coachProblemId = Some(s"${props.book}/${props.chapter}")
              )
            )
          case None =>
            // The page-level marker said "workbench" but the placeholder failed to decode — the
            // console has the BlockDiscovery warning with the specific reason.
            <.p(
              ^.className := "pwb__empty",
              "This problem page failed to load its workbench — see the console for details."
            )
      }
