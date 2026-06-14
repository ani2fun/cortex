package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js

/**
 * Keycloak-realm-explorer widget — an interactive picture of a Keycloak realm's object model. A left-hand
 * tree lists the realm and its groups (clients, users, roles, identity providers); clicking any node reveals
 * its configuration in a right-hand detail panel — a key/value table plus a plain-English note. It mirrors
 * the shape of Cortex's real `cortex-realm.json`, so the reader can map "realm → clients → cortex-web" onto
 * the JSON they will actually edit.
 *
 * Pure HTML, React-owned. The only state is which node is selected.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "The cortex realm",
 *   "realm": {
 *     "name":  "cortex",
 *     "fields": [ { "key": "sslRequired", "value": "none" }, … ],
 *     "note":  "A realm is an isolated universe of users, clients and roles.",
 *     "groups": [
 *       {
 *         "id": "clients", "label": "Clients",
 *         "items": [
 *           {
 *             "id": "cortex-web", "label": "cortex-web", "badge": "public client",
 *             "fields": [ { "key": "protocol", "value": "openid-connect" }, … ],
 *             "note": "The SPA. No secret — PKCE proves it started the flow."
 *           }
 *         ]
 *       }
 *     ]
 *   }
 * }
 * }}}
 *
 *   - `fields` are ordered key/value pairs (kept as an array, not an object, so display order is the
 *     author's).
 *   - `badge` and `note` are optional per item; `note` is optional on the realm too.
 */
object KeycloakRealmExplorer:

  final case class Field(key: String, value: String)

  final case class Item(
      id: String,
      label: String,
      badge: Option[String],
      fields: List[Field],
      note: Option[String]
  )

  final case class Group(id: String, label: String, items: List[Item])
  final case class Realm(name: String, fields: List[Field], note: Option[String], groups: List[Group])
  final case class Spec(title: Option[String], realm: Realm)

  final case class Props(payload: String)

  private val RealmKey = "__realm__"

  // ===========================================================================
  // Parsing
  // ===========================================================================

  private def parseField(d: js.Dynamic): Field =
    Field(key = d.string("key").trim, value = d.string("value").trim)

  private def parseItem(d: js.Dynamic): Item =
    Item(
      id = d.string("id").trim,
      label = d.string("label").trim,
      badge = d.optString("badge"),
      fields = d.dynList("fields").map(parseField).filter(_.key.nonEmpty),
      note = d.optString("note")
    )

  private def parseGroup(d: js.Dynamic): Group =
    Group(
      id = d.string("id").trim,
      label = d.string("label").trim,
      items = d.dynList("items").map(parseItem).filter(_.id.nonEmpty)
    )

  private def parsePayload(json: String): Either[String, Spec] =
    PayloadDecoder.run(json) { d =>
      val realmD = d.optObj("realm").getOrElse(throw PayloadDecoder.missing("realm"))
      val name   = realmD.string("name").trim
      if name.isEmpty then throw PayloadDecoder.invalid("realm.name is required")
      val groups = realmD.dynList("groups").map(parseGroup).filter(_.id.nonEmpty)
      if groups.isEmpty then throw PayloadDecoder.invalid("realm.groups must be non-empty")
      Spec(
        title = d.optString("title"),
        realm = Realm(
          name = name,
          fields = realmD.dynList("fields").map(parseField).filter(_.key.nonEmpty),
          note = realmD.optString("note"),
          groups = groups
        )
      )
    }

  // ===========================================================================
  // Selection model — a flat key → detail lookup so the panel is a pure read.
  // ===========================================================================

  private def itemKey(groupId: String, itemId: String): String = s"$groupId::$itemId"

  final private case class Detail(
      title: String,
      badge: Option[String],
      fields: List[Field],
      note: Option[String]
  )

  private def detailFor(spec: Spec, key: String): Detail =
    if key == RealmKey || key.isEmpty then
      Detail(s"Realm: ${spec.realm.name}", Some("realm"), spec.realm.fields, spec.realm.note)
    else
      spec.realm.groups.iterator
        .flatMap(g => g.items.iterator.map(it => itemKey(g.id, it.id) -> it))
        .collectFirst { case (k, it) if k == key => Detail(it.label, it.badge, it.fields, it.note) }
        .getOrElse(Detail(s"Realm: ${spec.realm.name}", Some("realm"), spec.realm.fields, spec.realm.note))

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useMemoBy(_.payload)(_ => payload => parsePayload(payload))
      .useState(RealmKey)
      .render { (_, specM, selS) =>
        specM.value match
          case Left(err) =>
            <.div(
              ^.className := "d3-widget__error",
              <.p(^.className   := "d3-widget__error-title", "Keycloak-realm-explorer payload error"),
              <.pre(^.className := "d3-widget__error-message", err)
            )
          case Right(spec) =>
            val selected = if selS.value.isEmpty then RealmKey else selS.value
            val detail   = detailFor(spec, selected)

            val tree: VdomNode =
              <.nav(
                ^.className := "keycloak-realm-explorer__tree",
                <.button(
                  ^.tpe := "button",
                  ^.className := s"keycloak-realm-explorer__realm-btn${
                      if selected == RealmKey then " keycloak-realm-explorer__realm-btn--active" else ""
                    }",
                  ^.onClick --> selS.setState(RealmKey),
                  s"🌐 ${spec.realm.name}"
                ),
                spec.realm.groups.toVdomArray { g =>
                  <.div(
                    ^.key       := g.id,
                    ^.className := "keycloak-realm-explorer__group",
                    <.p(^.className := "keycloak-realm-explorer__group-label", g.label),
                    <.ul(
                      ^.className := "keycloak-realm-explorer__items",
                      g.items.toVdomArray { it =>
                        val key = itemKey(g.id, it.id)
                        <.li(
                          ^.key := key,
                          <.button(
                            ^.tpe := "button",
                            ^.className := s"keycloak-realm-explorer__item-btn${
                                if selected == key then " keycloak-realm-explorer__item-btn--active" else ""
                              }",
                            ^.onClick --> selS.setState(key),
                            <.span(^.className := "keycloak-realm-explorer__item-label", it.label),
                            it.badge
                              .map(b =>
                                <.span(^.className := "keycloak-realm-explorer__item-badge", b): VdomNode
                              )
                              .getOrElse(EmptyVdom)
                          )
                        )
                      }
                    )
                  )
                }
              )

            val panel: VdomNode =
              <.div(
                ^.className := "keycloak-realm-explorer__detail",
                <.div(
                  ^.className := "keycloak-realm-explorer__detail-head",
                  <.h4(^.className := "keycloak-realm-explorer__detail-title", detail.title),
                  detail.badge
                    .map(b => <.span(^.className := "keycloak-realm-explorer__detail-badge", b): VdomNode)
                    .getOrElse(EmptyVdom)
                ),
                if detail.fields.isEmpty then
                  <.p(
                    ^.className := "keycloak-realm-explorer__empty",
                    "No configuration to show for this node."
                  )
                else
                  <.table(
                    ^.className := "keycloak-realm-explorer__fields",
                    <.tbody(
                      detail.fields.toVdomArray { f =>
                        <.tr(
                          ^.key       := f.key,
                          ^.className := "keycloak-realm-explorer__field-row",
                          <.td(^.className := "keycloak-realm-explorer__field-key", f.key),
                          <.td(^.className := "keycloak-realm-explorer__field-val", <.code(f.value))
                        )
                      }
                    )
                  )
                ,
                detail.note
                  .map(n => <.p(^.className := "keycloak-realm-explorer__note", n): VdomNode)
                  .getOrElse(EmptyVdom)
              )

            <.div(
              ^.className := "keycloak-realm-explorer not-prose",
              spec.title
                .map(t => <.p(^.className := "keycloak-realm-explorer__title", t): VdomNode)
                .getOrElse(EmptyVdom),
              <.div(
                ^.className := "keycloak-realm-explorer__layout",
                tree,
                panel
              )
            )
      }
