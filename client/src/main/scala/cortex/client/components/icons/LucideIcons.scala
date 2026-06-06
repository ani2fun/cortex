package cortex.client.components.icons

import japgolly.scalajs.react.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Typed bindings for the small subset of lucide-react icons we use.
 *
 * Each icon is a React function component that accepts standard SVG props plus the lucide-specific extras. We
 * expose them through scalajs-react's `JsComponent` machinery so they can be composed with VDOM like any
 * other React component — no `js.Dynamic` or hand-rolled wrappers.
 *
 * To add an icon: add a `@JSImport` for the named export and a `val` line binding the raw object to a
 * `JsComponent[IconProps, Children.None, Null]`.
 */
object LucideIcons:

  trait IconProps extends js.Object:
    var className: js.UndefOr[String]            = js.undefined
    var size: js.UndefOr[Int]                    = js.undefined
    var strokeWidth: js.UndefOr[Double]          = js.undefined
    var color: js.UndefOr[String]                = js.undefined
    var absoluteStrokeWidth: js.UndefOr[Boolean] = js.undefined

  /** Build IconProps with just a className. Most call sites only need this. */
  def withClass(c: String): IconProps =
    val p = (new js.Object).asInstanceOf[IconProps]
    p.className = c
    p

  // ---- Raw imports (named exports of `lucide-react`) ----------------------

  @js.native @JSImport("lucide-react", "Sun") private object SunRaw                     extends js.Object
  @js.native @JSImport("lucide-react", "Moon") private object MoonRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "Menu") private object MenuRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "X") private object XRaw                         extends js.Object
  @js.native @JSImport("lucide-react", "ArrowRight") private object ArrowRightRaw       extends js.Object
  @js.native @JSImport("lucide-react", "BookOpen") private object BookOpenRaw           extends js.Object
  @js.native @JSImport("lucide-react", "Loader2") private object Loader2Raw             extends js.Object
  @js.native @JSImport("lucide-react", "Play") private object PlayRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "Pause") private object PauseRaw                 extends js.Object
  @js.native @JSImport("lucide-react", "RotateCcw") private object RotateCcwRaw         extends js.Object
  @js.native @JSImport("lucide-react", "Square") private object SquareRaw               extends js.Object
  @js.native @JSImport("lucide-react", "Maximize2") private object Maximize2Raw         extends js.Object
  @js.native @JSImport("lucide-react", "ZoomIn") private object ZoomInRaw               extends js.Object
  @js.native @JSImport("lucide-react", "ZoomOut") private object ZoomOutRaw             extends js.Object
  @js.native @JSImport("lucide-react", "Check") private object CheckRaw                 extends js.Object
  @js.native @JSImport("lucide-react", "Copy") private object CopyRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "ChevronRight") private object ChevronRightRaw   extends js.Object
  @js.native @JSImport("lucide-react", "ChevronDown") private object ChevronDownRaw     extends js.Object
  @js.native @JSImport("lucide-react", "ListTree") private object ListTreeRaw           extends js.Object
  @js.native @JSImport("lucide-react", "ArrowLeft") private object ArrowLeftRaw         extends js.Object
  @js.native @JSImport("lucide-react", "ArrowUp") private object ArrowUpRaw             extends js.Object
  @js.native @JSImport("lucide-react", "Heart") private object HeartRaw                 extends js.Object
  @js.native @JSImport("lucide-react", "Star") private object StarRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "Trophy") private object TrophyRaw               extends js.Object
  @js.native @JSImport("lucide-react", "ExternalLink") private object ExternalLinkRaw   extends js.Object
  @js.native @JSImport("lucide-react", "Search") private object SearchRaw               extends js.Object
  @js.native @JSImport("lucide-react", "Download") private object DownloadRaw           extends js.Object
  @js.native @JSImport("lucide-react", "Pencil") private object PencilRaw               extends js.Object
  @js.native @JSImport("lucide-react", "Eye") private object EyeRaw                     extends js.Object
  @js.native @JSImport("lucide-react", "EyeOff") private object EyeOffRaw               extends js.Object
  @js.native @JSImport("lucide-react", "ChevronLeft") private object ChevronLeftRaw     extends js.Object
  @js.native @JSImport("lucide-react", "Focus") private object FocusRaw                 extends js.Object
  @js.native @JSImport("lucide-react", "Type") private object TypeRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "Link2") private object Link2Raw                 extends js.Object
  @js.native @JSImport("lucide-react", "Highlighter") private object HighlighterRaw     extends js.Object
  @js.native @JSImport("lucide-react", "Quote") private object QuoteRaw                 extends js.Object
  @js.native @JSImport("lucide-react", "BookMarked") private object BookMarkedRaw       extends js.Object
  @js.native @JSImport("lucide-react", "Lock") private object LockRaw                   extends js.Object
  @js.native @JSImport("lucide-react", "ChevronsLeft") private object ChevronsLeftRaw   extends js.Object
  @js.native @JSImport("lucide-react", "ChevronsRight") private object ChevronsRightRaw extends js.Object

  @js.native @JSImport("lucide-react", "AlertTriangle") private object AlertTriangleRaw
      extends js.Object

  @js.native @JSImport("lucide-react", "Terminal") private object TerminalRaw extends js.Object

  @js.native @JSImport("lucide-react", "Network") private object NetworkRaw extends js.Object

  @js.native @JSImport("lucide-react", "Info") private object InfoRaw extends js.Object

  @js.native @JSImport("lucide-react", "Share2") private object Share2Raw extends js.Object

  // ---- Components (call as Sun(withClass("h-5 w-5"))) --------------------

  val Sun           = JsComponent[IconProps, Children.None, Null](SunRaw)
  val Moon          = JsComponent[IconProps, Children.None, Null](MoonRaw)
  val Menu          = JsComponent[IconProps, Children.None, Null](MenuRaw)
  val X             = JsComponent[IconProps, Children.None, Null](XRaw)
  val ArrowRight    = JsComponent[IconProps, Children.None, Null](ArrowRightRaw)
  val BookOpen      = JsComponent[IconProps, Children.None, Null](BookOpenRaw)
  val Loader2       = JsComponent[IconProps, Children.None, Null](Loader2Raw)
  val Play          = JsComponent[IconProps, Children.None, Null](PlayRaw)
  val Pause         = JsComponent[IconProps, Children.None, Null](PauseRaw)
  val RotateCcw     = JsComponent[IconProps, Children.None, Null](RotateCcwRaw)
  val Square        = JsComponent[IconProps, Children.None, Null](SquareRaw)
  val Maximize2     = JsComponent[IconProps, Children.None, Null](Maximize2Raw)
  val ZoomIn        = JsComponent[IconProps, Children.None, Null](ZoomInRaw)
  val ZoomOut       = JsComponent[IconProps, Children.None, Null](ZoomOutRaw)
  val Check         = JsComponent[IconProps, Children.None, Null](CheckRaw)
  val Copy          = JsComponent[IconProps, Children.None, Null](CopyRaw)
  val ChevronRight  = JsComponent[IconProps, Children.None, Null](ChevronRightRaw)
  val ChevronDown   = JsComponent[IconProps, Children.None, Null](ChevronDownRaw)
  val ListTree      = JsComponent[IconProps, Children.None, Null](ListTreeRaw)
  val ArrowLeft     = JsComponent[IconProps, Children.None, Null](ArrowLeftRaw)
  val ArrowUp       = JsComponent[IconProps, Children.None, Null](ArrowUpRaw)
  val Heart         = JsComponent[IconProps, Children.None, Null](HeartRaw)
  val Star          = JsComponent[IconProps, Children.None, Null](StarRaw)
  val Trophy        = JsComponent[IconProps, Children.None, Null](TrophyRaw)
  val ExternalLink  = JsComponent[IconProps, Children.None, Null](ExternalLinkRaw)
  val Search        = JsComponent[IconProps, Children.None, Null](SearchRaw)
  val Download      = JsComponent[IconProps, Children.None, Null](DownloadRaw)
  val Pencil        = JsComponent[IconProps, Children.None, Null](PencilRaw)
  val Eye           = JsComponent[IconProps, Children.None, Null](EyeRaw)
  val EyeOff        = JsComponent[IconProps, Children.None, Null](EyeOffRaw)
  val ChevronLeft   = JsComponent[IconProps, Children.None, Null](ChevronLeftRaw)
  val Focus         = JsComponent[IconProps, Children.None, Null](FocusRaw)
  val Type          = JsComponent[IconProps, Children.None, Null](TypeRaw)
  val Link2         = JsComponent[IconProps, Children.None, Null](Link2Raw)
  val Highlighter   = JsComponent[IconProps, Children.None, Null](HighlighterRaw)
  val Quote         = JsComponent[IconProps, Children.None, Null](QuoteRaw)
  val BookMarked    = JsComponent[IconProps, Children.None, Null](BookMarkedRaw)
  val Lock          = JsComponent[IconProps, Children.None, Null](LockRaw)
  val ChevronsLeft  = JsComponent[IconProps, Children.None, Null](ChevronsLeftRaw)
  val ChevronsRight = JsComponent[IconProps, Children.None, Null](ChevronsRightRaw)
  val AlertTriangle = JsComponent[IconProps, Children.None, Null](AlertTriangleRaw)
  val Terminal      = JsComponent[IconProps, Children.None, Null](TerminalRaw)
  val Network       = JsComponent[IconProps, Children.None, Null](NetworkRaw)
  val Info          = JsComponent[IconProps, Children.None, Null](InfoRaw)
  val Share2        = JsComponent[IconProps, Children.None, Null](Share2Raw)
