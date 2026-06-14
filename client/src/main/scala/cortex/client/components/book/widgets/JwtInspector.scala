package cortex.client.components.book.widgets

import cortex.client.components.book.widgets.PayloadDecoder.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

import scala.scalajs.js
import scala.util.Try

/**
 * JWT-inspector widget — paste (or edit) a JSON Web Token and watch it decode live, jwt.io style. The raw
 * token is shown with its three dot-separated segments colour-coded (header / payload / signature); the
 * header and payload are base64url-decoded and pretty-printed; and a claims table annotates the standard
 * registered claims (`iss`, `sub`, `aud`, `azp`, `exp`, …) in plain English, with an expiry badge computed
 * from `exp`.
 *
 * Everything is client-side and decode-only — the widget never verifies the signature (that needs the
 * issuer's public key) and never sends the token anywhere. The point is to make "a JWT is just base64url JSON
 * you can read" viscerally obvious, and to let the reader mutate a claim and see the decode update.
 *
 * Payload schema (JSON):
 * {{{
 * {
 *   "title": "A Keycloak access token, decoded",
 *   "token": "eyJhbGciOiJSUzI1Ni␣….eyJpc3Mi␣….SflKxwRJ␣…"
 * }
 * }}}
 *
 *   - `token` seeds the editable textarea; the reader can replace it with any token.
 *   - `title` is an optional heading.
 *
 * Unlike the SVG widgets this one is pure HTML — React owns everything; there is no
 * `dangerouslySetInnerHtml`.
 */
object JwtInspector:

  final case class Spec(title: Option[String], token: String)
  final case class Props(payload: String)

  // ===========================================================================
  // Lenient parse — this widget never shows a payload error; a malformed
  // payload just yields an empty token the reader can type into.
  // ===========================================================================

  private def parseSpec(payload: String): Spec =
    PayloadDecoder.run(payload)(d => Spec(d.optString("title"), d.string("token").trim)) match
      case Right(s) => s
      case Left(_)  => Spec(None, "")

  // ===========================================================================
  // base64url + JSON helpers
  // ===========================================================================

  /** base64url → UTF-8 string. Returns None on malformed input. */
  private def b64urlDecode(seg: String): Option[String] =
    Try {
      val base64 = seg.replace('-', '+').replace('_', '/')
      val padded = base64 + ("=" * ((4 - base64.length % 4) % 4))
      val binary = js.Dynamic.global.atob(padded).asInstanceOf[String]
      // UTF-8-safe: lift each Latin-1 byte to a %XX escape, then decodeURIComponent.
      Try {
        val percent = binary.iterator.map(ch => "%" + f"${ch.toInt & 0xff}%02X").mkString
        js.Dynamic.global.decodeURIComponent(percent).asInstanceOf[String]
      }.getOrElse(binary)
    }.toOption

  /** Pretty-print a JSON string with 2-space indent. Returns the input unchanged if it does not parse. */
  private def prettyJson(raw: String): String =
    Try(js.Dynamic.global.JSON.stringify(js.JSON.parse(raw), null, 2).asInstanceOf[String]).getOrElse(raw)

  private def displayValue(raw: js.Any): String =
    if js.typeOf(raw) == "string" then raw.asInstanceOf[String]
    else Try(js.Dynamic.global.JSON.stringify(raw).asInstanceOf[String]).getOrElse(String.valueOf(raw))

  // ===========================================================================
  // Claim dictionary — registered + common Keycloak claims, in display order.
  // ===========================================================================

  private val ClaimDocs: Map[String, String] = Map(
    "iss"   -> "Issuer — the realm URL that minted this token. The server checks it matches exactly.",
    "sub"   -> "Subject — the user's stable, opaque id. Never reuse a username here.",
    "aud"   -> "Audience — who this token is for. The API rejects a token meant for someone else.",
    "azp"   -> "Authorized party — the client that requested the token (Keycloak's public-client id).",
    "exp"   -> "Expires at — Unix seconds. After this instant the token is dead.",
    "iat"   -> "Issued at — when the token was minted.",
    "nbf"   -> "Not before — the token is invalid until this instant.",
    "jti"   -> "JWT id — a unique id for this token (useful for revocation lists).",
    "typ"   -> "Token type — usually \"Bearer\" or \"JWT\".",
    "scope" -> "Granted scopes — what the bearer is allowed to ask for.",
    "preferred_username" -> "Human-friendly username (here: the GitHub login brokered through Keycloak).",
    "email"              -> "The user's email, if the email scope was granted.",
    "name"               -> "The user's display name.",
    "azp_note"           -> "",
    "realm_access"       -> "Realm roles assigned to the user — Keycloak's RBAC, carried in the token.",
    "resource_access"    -> "Per-client roles."
  )

  private val ClaimOrder: List[String] =
    List(
      "iss",
      "sub",
      "aud",
      "azp",
      "exp",
      "iat",
      "nbf",
      "preferred_username",
      "email",
      "name",
      "scope",
      "realm_access",
      "resource_access",
      "jti"
    )

  private def formatUnix(sec: Double): String =
    Try {
      val d = new js.Date(sec * 1000.0)
      d.toISOString()
    }.getOrElse(sec.toString)

  // ===========================================================================
  // Component
  // ===========================================================================

  val Component =
    ScalaFnComponent
      .withHooks[Props]
      .useStateBy(p => parseSpec(p.payload).token)
      .render { (p, tokenS) =>
        val spec  = parseSpec(p.payload)
        val token = tokenS.value
        val parts = token.split('.').toList

        val segChildren: List[VdomNode] = parts.zipWithIndex.flatMap { case (seg, i) =>
          val segClass = i match
            case 0 => "jwt-inspector__seg jwt-inspector__seg--header"
            case 1 => "jwt-inspector__seg jwt-inspector__seg--payload"
            case _ => "jwt-inspector__seg jwt-inspector__seg--signature"
          val dot: List[VdomNode] =
            if i > 0 then List(<.span(^.key := s"dot-$i", ^.className := "jwt-inspector__dot", "."))
            else Nil
          dot :+ <.span(^.key := s"seg-$i", ^.className := segClass, seg)
        }
        val segView: VdomNode =
          <.div(^.className := "jwt-inspector__token", segChildren.toVdomArray)

        val headerJson  = parts.lift(0).flatMap(b64urlDecode)
        val payloadJson = parts.lift(1).flatMap(b64urlDecode)

        def jsonPanel(title: String, decoded: Option[String]): VdomNode =
          <.section(
            ^.className := "jwt-inspector__panel",
            <.h4(^.className := "jwt-inspector__panel-title", title),
            decoded match
              case Some(raw) => <.pre(^.className := "jwt-inspector__json", prettyJson(raw))
              case None => <.p(^.className := "jwt-inspector__error-inline", "Could not decode this segment.")
          )

        val claimsTable: VdomNode =
          payloadJson.flatMap(raw => Try(js.JSON.parse(raw)).toOption) match
            case None => EmptyVdom
            case Some(obj) =>
              val dyn     = obj.asInstanceOf[js.Dynamic]
              val present = Try(js.Object.keys(obj.asInstanceOf[js.Object]).toList).getOrElse(Nil)
              val ordered = ClaimOrder.filter(present.contains) ++ present.filterNot(ClaimOrder.contains)
              val nowSec  = Try(js.Dynamic.global.Date.now().asInstanceOf[Double] / 1000.0).getOrElse(0.0)

              <.table(
                ^.className := "jwt-inspector__claims",
                <.tbody(
                  ordered.toVdomArray { key =>
                    val raw   = dyn.selectDynamic(key).asInstanceOf[js.Any]
                    val value = displayValue(raw)
                    val timeNote =
                      if key == "exp" || key == "iat" || key == "nbf" then
                        Try(value.toDouble).toOption.map(formatUnix)
                      else None
                    val expBadge: VdomNode =
                      if key == "exp" then
                        Try(value.toDouble).toOption match
                          case Some(exp) if nowSec > 0 && exp < nowSec =>
                            <.span(
                              ^.className := "jwt-inspector__badge jwt-inspector__badge--expired",
                              "expired"
                            )
                          case Some(_) if nowSec > 0 =>
                            <.span(
                              ^.className := "jwt-inspector__badge jwt-inspector__badge--valid",
                              "not expired"
                            )
                          case _ => EmptyVdom
                      else EmptyVdom
                    <.tr(
                      ^.key       := key,
                      ^.className := "jwt-inspector__claim-row",
                      <.td(^.className := "jwt-inspector__claim-key", key),
                      <.td(
                        ^.className := "jwt-inspector__claim-val",
                        <.code(value),
                        timeNote.map(t =>
                          <.span(^.className := "jwt-inspector__claim-time", s" → $t"): VdomNode
                        ).getOrElse(EmptyVdom),
                        expBadge
                      ),
                      <.td(
                        ^.className := "jwt-inspector__claim-desc",
                        ClaimDocs.getOrElse(key, "")
                      )
                    )
                  }
                )
              )

        <.div(
          ^.className := "jwt-inspector not-prose",
          spec.title
            .map(t => <.p(^.className := "jwt-inspector__title", t): VdomNode)
            .getOrElse(EmptyVdom),
          <.label(
            ^.className := "jwt-inspector__input-label",
            ^.htmlFor   := "jwt-inspector-input",
            "Token (editable — paste your own)"
          ),
          <.textarea(
            ^.id        := "jwt-inspector-input",
            ^.className := "jwt-inspector__input",
            ^.rows      := 3,
            ^.value     := token,
            ^.onChange ==> { (e: ReactEventFromInput) =>
              val v = e.target.value
              tokenS.setState(v)
            }
          ),
          segView,
          if parts.length < 2 then
            <.p(
              ^.className := "jwt-inspector__error-inline",
              "A JWT has three dot-separated parts: header.payload.signature. Paste one above."
            )
          else
            <.div(
              ^.className := "jwt-inspector__grid",
              jsonPanel("Header", headerJson),
              jsonPanel("Payload (claims)", payloadJson)
            )
          ,
          claimsTable
        )
      }
