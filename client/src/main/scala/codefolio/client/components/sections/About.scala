package codefolio.client.components.sections

import codefolio.client.components.ui.Section
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

/**
 * About — prose left, sticky portrait + fact list right.
 *
 * Long-form bio: opens with a Clarke epigraph, three contextual paragraphs about the work, an italic-display
 * bullet list of recurring themes, then personal lines (renovation, yoga, weight loss, PS5) and a closing
 * "happy to talk about" CTA. The fact list mirrors the bio's headline points (currently / based / education /
 * languages / completed diploma) — rebuilt from the new copy so the two sides stay coherent.
 *
 * The fact list is hardcoded inline. It's five lines that change once a year; routing them through JSON would
 * only buy a build-time indirection and an extra place to forget to update.
 */
object About:

  /** Compact label/value rows shown beside the portrait. */
  private val factList: List[(String, String)] = List(
    "CURRENTLY" -> "Europcar International · Backend",
    "BASED"     -> "Paris, France · open to remote (EU) or hybrid",
    "COMPLETED" -> "Diplôme Data Engineer · Liora × Sorbonne (Apr 2026)",
    "EDUCATION" -> "M.Sc. ISEP Paris · B.E. Univ. Mumbai",
    "LANGUAGES" -> "English · Hindi · Marathi · learning French"
  )

  val Component = ScalaFnComponent[Unit] { _ =>
    Section("about", "about")(
      <.div(
        ^.className := "about__layout",
        <.div(
          ^.className := "about__prose",
          <.div(^.className := "about__eyebrow", "ABOUT"),
          <.h2(
            ^.className := "about__title",
            "A decade in production systems."
          ),
          <.blockquote(
            ^.className := "about__quote",
            <.p(
              ^.className := "about__quote-text",
              "The only way to discover the limits of the possible is to go beyond them into the impossible."
            ),
            <.cite(^.className := "about__quote-author", "— Arthur C. Clarke")
          ),
          <.p(
            ^.className := "about__paragraph",
            "I build the backend systems that power large applications: the part users don't see, but everything depends on."
          ),
          <.p(
            ^.className := "about__paragraph",
            "As a Software Engineer / Data Engineer with ",
            <.span(^.className := "about__emphasis", "10+ years of experience"),
            ", I've worked on services on the JVM (Kotlin, Java, Scala) with Spring Boot, Kafka, PostgreSQL, and cloud delivery on AWS and GCP."
          ),
          <.p(
            ^.className := "about__paragraph",
            "Some of the companies and clients I've worked with: ",
            <.span(
              ^.className := "about__emphasis",
              "Europcar, Disneyland Paris, Audi, Dassault Systèmes, UPS, Nokia Bell Labs"
            ),
            "."
          ),
          <.p(^.className := "about__paragraph", "A few things I keep coming back to:"),
          <.ul(
            ^.className := "about__list",
            <.li(
              ^.className := "about__list-item",
              <.span(^.className := "about__emphasis", "Microservices on the JVM"),
              " — with a soft spot for hexagonal architecture and Domain-Driven Design; currently shipping Kotlin services on this pattern at Europcar."
            ),
            <.li(
              ^.className := "about__list-item",
              <.span(^.className := "about__emphasis", "Data pipelines and infrastructure"),
              " — at Audi, helped deliver the labeling pipeline behind their autonomous-driving model training (Argo Workflows, Kafka, Elasticsearch)."
            ),
            <.li(
              ^.className := "about__list-item",
              <.span(^.className := "about__emphasis", "Self-hosted infrastructure"),
              " — I run a small Kubernetes homelab and care about the craft of operating real services."
            )
          ),
          <.p(
            ^.className := "about__paragraph",
            "What makes me different: I like getting my hands dirty on hard problems whether they're in code or not. I renovated my own home with the same instinct that makes me run my own k8s cluster instead of just using someone else's."
          ),
          <.p(
            ^.className := "about__paragraph",
            "I recently earned the ",
            <.span(
              ^.className := "about__emphasis",
              "Data Engineer diploma at Liora (formerly DataScientest)"
            ),
            " backed by ",
            <.span(^.className := "about__emphasis", "Sorbonne University"),
            ". I also maintain a Gradle plugin on Maven Central (",
            <.code(^.className := "about__inline-code", "eu.kakde.gradle.sonatype-maven-central-publisher"),
            ") and write technical guides at kakde.eu."
          ),
          <.p(
            ^.className := "about__paragraph",
            <.span(^.className := "about__emphasis", "Outside work:"),
            " yoga and meditation daily. Recently lost 10 kg over four months by treating it like a system to debug. Reading self-help and the Bhagavad Gita. Action and role-playing games on PS5 when the day is done."
          ),
          <.p(
            ^.className := "about__paragraph about__paragraph--cta",
            <.span(^.className := "about__emphasis", "Happy to talk about: "),
            "backend and data-engineering roles in Paris (hybrid / on-site) or fully remote in the EU."
          )
        ),
        <.aside(
          ^.className := "about__sidecard",
          <.img(
            ^.src               := "/img/portfolio/image2.webp",
            ^.alt               := "Aniket Kakde — portrait",
            VdomAttr("loading") := "lazy",
            ^.className         := "about__portrait"
          ),
          <.dl(
            ^.className := "about__facts",
            factList.toTagMod { case (label, value) =>
              <.div(
                ^.className := "about__fact",
                ^.key       := label,
                <.dt(^.className := "about__fact-label", label),
                <.dd(^.className := "about__fact-value", value)
              )
            }
          )
        )
      )
    )
  }
