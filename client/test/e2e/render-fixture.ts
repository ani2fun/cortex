// Phase 0 trace-shape harness driver (ADR-0025).
//
// Reads ?fixture=<name> from the URL, fetches the matching VizCases JSON from
// ./fixtures/<name>.json, and calls the production d3 `renderWidget` entry on
// `#harness-root`. Surfaces lifecycle to Playwright via `data-render-state` on
// the root + `window.__controller` for assertions on the controller API.

import { renderWidget, type WidgetController } from "../../src/d3/index.ts";

declare global {
  interface Window {
    __controller?: WidgetController | null;
  }
}

const params = new URLSearchParams(window.location.search);
const fixtureName = params.get("fixture") ?? "";

const header = document.getElementById("harness-header") as HTMLElement;
const root = document.getElementById("harness-root") as HTMLElement;
const errBox = document.getElementById("harness-error") as HTMLPreElement;

function fail(message: string): void {
  root.dataset.renderState = "error";
  errBox.hidden = false;
  errBox.textContent = message;
  console.error("[render-fixture]", message);
}

async function main(): Promise<void> {
  if (fixtureName === "") {
    fail("Missing ?fixture=<name> URL parameter.");
    return;
  }
  // Naive shape check — fixture names are kebab-case ASCII; reject anything
  // else to keep the harness from acting as a path-traversal probe.
  if (!/^[a-z0-9-]+$/.test(fixtureName)) {
    fail(`Refusing fixture name "${fixtureName}" — must match /^[a-z0-9-]+$/.`);
    return;
  }
  header.textContent = `Fixture: ${fixtureName}`;

  let jsonStr: string;
  try {
    const res = await fetch(`./fixtures/${fixtureName}.json`);
    if (!res.ok) {
      fail(`fetch ${fixtureName}.json → ${res.status} ${res.statusText}`);
      return;
    }
    jsonStr = await res.text();
  } catch (e) {
    fail(`fetch failed: ${e instanceof Error ? e.message : String(e)}`);
    return;
  }

  let controller: WidgetController | null = null;
  try {
    controller = renderWidget("harness-root", jsonStr);
  } catch (e) {
    fail(`renderWidget threw: ${e instanceof Error ? e.message : String(e)}`);
    return;
  }
  if (controller === null) {
    fail("renderWidget returned null — see DevTools console for the parse / mount error.");
    return;
  }
  window.__controller = controller;
  root.dataset.renderState = "ready";
}

void main();
