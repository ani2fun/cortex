package cortex.client.components.ui

/**
 * Reimplementation of portfolio-app's shadcn `Button` primitive — the CVA variant table, ported to a Scala
 * enum. We don't expose a React component wrapper; call sites just use `<.button(^.className :=
 * Button.classes(...))` which matches scalajs-react's idiom and avoids an extra layer.
 *
 * Drop the original `asChild` (Radix Slot) escape hatch — portfolio-app doesn't actually use it.
 */
object Button:

  enum Variant:
    case Default, Destructive, Outline, Secondary, Ghost, Link

  enum Size:
    case Default, Sm, Lg, Icon

  private val baseClasses =
    "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium " +
      "ring-offset-background transition-colors " +
      "focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 " +
      "disabled:pointer-events-none disabled:opacity-50"

  private def variantCls(v: Variant): String = v match
    case Variant.Default     => "bg-primary text-primary-foreground hover:bg-primary/90"
    case Variant.Destructive => "bg-destructive text-destructive-foreground hover:bg-destructive/90"
    case Variant.Outline =>
      "border border-input bg-background hover:bg-accent hover:text-accent-foreground"
    case Variant.Secondary => "bg-secondary text-secondary-foreground hover:bg-secondary/80"
    case Variant.Ghost     => "hover:bg-accent hover:text-accent-foreground"
    case Variant.Link      => "text-primary underline-offset-4 hover:underline"

  private def sizeCls(s: Size): String = s match
    case Size.Default => "h-10 px-4 py-2"
    case Size.Sm      => "h-9 rounded-md px-3"
    case Size.Lg      => "h-11 rounded-md px-8"
    case Size.Icon    => "h-10 w-10"

  /** Compose the className for a button with the given variant/size/extras. */
  def classes(
      variant: Variant = Variant.Default,
      size: Size = Size.Default,
      extra: String = ""
  ): String =
    Seq(baseClasses, variantCls(variant), sizeCls(size), extra).filter(_.nonEmpty).mkString(" ")
