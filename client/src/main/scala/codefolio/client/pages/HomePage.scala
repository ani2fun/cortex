package codefolio.client.pages

import codefolio.client.components.sections.{About, Certifications, Experience, Hero, Projects, SelectedWork}
import codefolio.client.util.PageTitle
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import org.scalajs.dom

/**
 * Landing page — composes every portfolio section in order: Hero, SelectedWork, About, Experience, Projects,
 * Certifications. The Cortex and Blog landing pages are reachable via the Hero CTAs (and the Header nav), not
 * as homepage sections. Header and Footer are applied by the Layout, not here.
 *
 * On mount, scroll to the URL fragment (e.g. `/#about`) so deep-links work after a hard reload.
 */
object HomePage:

  val Component =
    ScalaFnComponent
      .withHooks[Unit]
      .useEffectOnMountBy { _ =>
        PageTitle.set(PageTitle.Default) >> Callback {
          val hash = dom.window.location.hash.stripPrefix("#")
          if hash.nonEmpty then
            val target = dom.document.getElementById(hash)
            if target != null then target.scrollIntoView(true)
        }
      }
      .render { _ =>
        <.main(
          ^.className := "container",
          Hero.Component(),
          SelectedWork.Component(),
          About.Component(),
          Experience.Component(),
          Projects.Component(),
          Certifications.Component()
        )
      }
