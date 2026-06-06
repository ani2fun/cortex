package codefolio.client.data

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Static portfolio data — projects, work history, certifications.
 *
 * The JSON files live next to this binding in `client/src/data/`. A small TypeScript bridge
 * (`portfolioData.ts`) re-exports them so Vite inlines them as ES-module bindings at bundle time. The `@data`
 * alias is wired in `vite.config.mjs` so the path stays stable regardless of linker output layout.
 *
 * Each shape is a `js.Object` trait — fields are read directly off the deserialised JSON, no glue conversion
 * needed. Add a new field to the JSON and a corresponding `val` here; everything else is type-checked.
 */
object PortfolioData:

  // ---- Projects -----------------------------------------------------------

  @js.native
  trait ProjectImage extends js.Object:
    val url: String = js.native
    val alt: String = js.native

  @js.native
  trait Project extends js.Object:
    val name: String                  = js.native
    val tags: js.Array[String]        = js.native
    val projectUrl: String            = js.native
    val githubUrl: String             = js.native
    val image: ProjectImage           = js.native
    val description: String           = js.native
    val category: js.UndefOr[String]  = js.native
    val metadata: js.UndefOr[String]  = js.native
    val featured: js.UndefOr[Boolean] = js.native

  // ---- Experience ---------------------------------------------------------

  @js.native
  trait Company extends js.Object:
    val short: String = js.native
    val name: String  = js.native
    val url: String   = js.native

    /**
     * Optional name to use in Hero's logo strip when it should differ from `short` — e.g. JSON says "Nokia",
     * strip wants "Bell Labs".
     */
    val displayName: js.UndefOr[String] = js.native

  @js.native
  trait Experience extends js.Object:
    val position: String                       = js.native
    val company: Company                       = js.native
    val time: String                           = js.native
    val description: String                    = js.native
    val items: js.Array[String]                = js.native
    val results: js.UndefOr[js.Array[String]]  = js.native
    val leveragedKnowledgeIn: js.Array[String] = js.native

    /** 2–3 promoted chips shown above the bullets. */
    val primaryTech: js.UndefOr[js.Array[String]] = js.native

    /** Demoted secondary stack shown smaller below the bullets. */
    val secondaryTech: js.UndefOr[js.Array[String]] = js.native

    /** Pulled into SelectedWork strip when true. */
    val featured: js.UndefOr[Boolean] = js.native

    /** Short label in SelectedWork meta row, e.g. "Backend / Identity". */
    val roleTag: js.UndefOr[String] = js.native

    /** Short blurb for SelectedWork rows; falls back to `description`. */
    val selectedWorkBlurb: js.UndefOr[String] = js.native

    /** Location string for SelectedWork meta row, e.g. "Paris" or "Remote". */
    val location: js.UndefOr[String] = js.native

  // ---- Certifications -----------------------------------------------------

  @js.native
  trait Certification extends js.Object:
    val name: String           = js.native
    val issuer: String         = js.native
    val date: String           = js.native
    val length: String         = js.native
    val description: String    = js.native
    val tags: js.Array[String] = js.native
    val url: String            = js.native
    val highlight: Boolean     = js.native

  // ---- Module bindings ----------------------------------------------------

  @js.native @JSImport("@data/portfolioData", "projects")
  val projects: js.Array[Project] = js.native

  @js.native @JSImport("@data/portfolioData", "experience")
  val experience: js.Array[Experience] = js.native

  @js.native @JSImport("@data/portfolioData", "certifications")
  val certifications: js.Array[Certification] = js.native
