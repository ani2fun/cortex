package codefolio.client.components.cortex

import japgolly.scalajs.react.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Thin Scala.js facade over `@monaco-editor/react`'s `Editor` component, mediated by the TS shim at
 * `@markdown/monaco`. The shim owns Monaco lifecycle concerns (worker registration, language tokeniser
 * imports, custom theme, `loader.config({ monaco })`) so this file only declares the React prop surface we
 * actually use.
 *
 * Pass `options` as a `js.Dynamic.literal(...)` of Monaco editor options
 * (https://microsoft.github.io/monaco-editor/api/interfaces/editor.IStandaloneEditorConstructionOptions.html);
 * the wrapper diffs option changes between renders and calls `editor.updateOptions(...)` under the hood, so
 * flipping `readOnly` does not remount.
 *
 * `onMount(editor, monaco)` receives both the editor instance and the top-level Monaco namespace as
 * `js.Dynamic` — enough for `editor.addCommand`, `editor.onDidContentSizeChange`, `editor.layout`,
 * `monaco.KeyMod`, and `monaco.KeyCode` without writing a fuller facade.
 */
object MonacoEditor:

  @js.native @JSImport("@markdown/monaco", JSImport.Default)
  private object EditorRaw extends js.Object

  trait EditorProps extends js.Object:
    var value: js.UndefOr[String]                                            = js.undefined
    var language: js.UndefOr[String]                                         = js.undefined
    var theme: js.UndefOr[String]                                            = js.undefined
    var height: js.UndefOr[js.Any]                                           = js.undefined
    var width: js.UndefOr[js.Any]                                            = js.undefined
    var options: js.UndefOr[js.Object]                                       = js.undefined
    var loading: js.UndefOr[String]                                          = js.undefined
    var className: js.UndefOr[String]                                        = js.undefined
    var onChange: js.UndefOr[js.Function2[js.UndefOr[String], js.Any, Unit]] = js.undefined
    var onMount: js.UndefOr[js.Function2[js.Dynamic, js.Dynamic, Unit]]      = js.undefined

  val Component = JsComponent[EditorProps, Children.None, Null](EditorRaw)
