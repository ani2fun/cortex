---
title: Build Toolchain — sbt, Vite, Tailwind, Liquibase, scalafmt
summary: How `sbt compile`, `npm run dev`, `bin/dev`, and `docker compose up` actually work — and why each tool was picked over its peers.
---

The build tooling sits underneath everything else and the choices ripple through the rest of the codebase. This chapter answers "why this tool and not its peer", what each plugin in the chain actually does, and which knobs matter.

## sbt — the canonical Scala build tool

The Scala ecosystem has two real build tools: **sbt** (older, more feature-complete, widely used) and **Mill** (newer, sometimes faster, less plugin coverage). We use sbt because:

- **The plugins exist.** `sbt-scalajs`, `sbt-openapi-codegen`, `sbt-revolver`, `sbt-native-packager` are all sbt-only. Some have Mill ports; most don't.
- **Cross-projects are a first-class concept.** The `crossProject(JSPlatform, JVMPlatform)` shape used in `build.sbt` is sbt-native; replicating it in Mill requires more boilerplate.
- **Ecosystem familiarity.** Every Scala dev knows the sbt CLI; switching tools costs time without buying anything for a project this size.

The build root is `build.sbt`. It defines four sbt projects:

```scala
lazy val shared = crossProject(JSPlatform, JVMPlatform)…   // cross-compiles
lazy val sharedJVM = shared.jvm
lazy val sharedJS  = shared.js
lazy val server = (project in file("server"))…             // JVM only
lazy val client = (project in file("client"))…             // JS only
lazy val root = (project in file(".")).aggregate(…)        // umbrella
```

The aggregate `root` exists so `sbt compile` from the repo top-level builds everything in one go.

**If you switch to Mill:** the codegen plugin port doesn't exist as of writing. You'd be reimplementing the OpenAPI step manually, which defeats the purpose of having one.

## The plugin set in `project/plugins.sbt`

Six plugins. Each has a single, sharp responsibility:

```scala
addSbtPlugin("org.scala-js"                % "sbt-scalajs"              % "1.21.0")
addSbtPlugin("org.portable-scala"          % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("com.github.sbt"              % "sbt-native-packager"      % "1.10.4")
addSbtPlugin("io.spray"                    % "sbt-revolver"             % "0.10.0")
addSbtPlugin("com.softwaremill.sttp.tapir" % "sbt-openapi-codegen"      % "1.11.22")
addSbtPlugin("org.scalameta"               % "sbt-scalafmt"             % "2.5.2")
```

| Plugin | Purpose |
|---|---|
| `sbt-scalajs` | Compiles Scala source to JavaScript. Defines `fastLinkJS` (debug, fast) and `fullLinkJS` (release, optimised). |
| `sbt-scalajs-crossproject` | Adds the `crossProject(JSPlatform, JVMPlatform)` DSL. Without it, you'd hand-write two parallel projects with the same source set. |
| `sbt-native-packager` | Wraps the JVM build into a launcher. `server/Universal/stage` produces `server/target/universal/stage/` with `bin/cortex-server`, `lib/*.jar`, and a startup script. The Docker runtime stage copies this directly. |
| `sbt-revolver` | Adds `~reStart` (auto-restart on file change) and `reStop`. The dev server runs in a forked JVM so editor saves trigger a quick restart instead of waiting for a slow `sbt run` boot. |
| `sbt-openapi-codegen` | The codegen pipeline (see [Shared & Codegen](/cortex/cortex-onboarding/deep-dive-shared-and-codegen)). Reads `api/openapi.yaml`, emits Scala. |
| `sbt-scalafmt` | Wires `scalafmt` into sbt. `sbt scalafmtAll` formats; `sbt scalafmtCheckAll` fails CI on drift. |

**If you remove `sbt-revolver`:** every server change becomes `Ctrl-C` + `sbt server/run` + 5s of cold start. With it, edits restart in <1s.

**If you remove `sbt-native-packager`:** the Docker runtime stage has nothing to copy. You'd build a fat-jar instead, which works but is harder to introspect (no separate `lib/` to mount, no `bin/cortex-server` launcher).

## Why `sbt-revolver`'s `reStart` and not `~run`

`sbt ~run` works — it watches for changes and restarts. But `~run` blocks the sbt shell, so you can't run other commands. `~reStart` runs the server in a forked JVM in the background; you stay in the sbt shell and can do `client/fastLinkJS` or `test` while the server is running.

The other reason: **sbt-revolver's lifecycle is explicit.** `reStop` kills the forked JVM cleanly; `kill $sbt_pid` against `~run` leaves the child hanging. Without `reStop`, port 8080 stays held by a zombie JVM and the next launch fails. (The pre-flight in `bin/dev` exists partly to clean up after this footgun.)

**If you switch to `~run`:** flip from a single-pane workflow to a two-terminal workflow, and remember to kill the run before each restart.

## The forked-JVM cwd setting

`build.sbt`:

```scala
Compile / run / baseDirectory := (LocalRootProject / baseDirectory).value,
reStart / baseDirectory       := (LocalRootProject / baseDirectory).value,
```

Without this, `sbt server/run` launches the JVM with `server/` as its working directory. Then `application.conf`'s default `static-dir = "./client/dist"` resolves to `server/client/dist` — which doesn't exist. The fallback "if dist isn't there, skip static routes" kicks in, and you get a working API but no SPA.

Pinning the cwd to the build root keeps relative paths consistent across `sbt server/run`, `sbt server/reStart`, and `bin/dev`.

**If you remove these lines:** `bin/dev` works (it sets cwd manually), but bare `sbt server/run` serves the API only. You'll waste an afternoon wondering why static files aren't loading.

## Vite — picking the bundler

The frontend bundler choices are basically:

- **Vite** — esbuild for dev, Rollup for production. Native ES modules in dev (no bundle step), HMR is instant.
- **webpack** — the historical default. Slower dev startup, more configuration surface.
- **Parcel** — zero-config but less plugin coverage.

We use Vite for two reasons:

1. **Speed.** `npm run dev` boots in ~500ms; webpack takes 5–10s on a similarly-sized project. Big difference when you restart often.
2. **`@scala-js/vite-plugin-scalajs`** exists and is maintained. The webpack equivalent is older and has rough edges around watching the linker output.

**If you switch to webpack:** the Scala.js linker integration is rougher; HMR for Scala edits is unreliable. Most contributors to scalajs-react projects have moved to Vite.

## The Vite plugin set

`client/vite.config.mjs`:

```javascript
plugins: [
  tailwindcss(),
  scalaJSPlugin({ cwd: "..", projectID: "client" }),
]
```

Only two plugins. The Tailwind v4 plugin handles all CSS — no PostCSS, no separate Tailwind step. The Scala.js plugin handles all Scala — exposes the linker output as an importable module.

**Why `cwd: ".."`?** Vite runs from `client/`. The Scala.js linker is invoked via sbt, which expects to be in the build root. Without `cwd: ".."`, the plugin tries to invoke sbt inside `client/`, which has no `build.sbt` of its own. The fix is one line; finding the bug if you don't know to look for it takes hours. (Mentioned in [Local Development](/cortex/cortex-onboarding/working-on-it-local-development)'s foot-guns list.)

**`projectID: "client"`** tells the plugin which sbt module's linker output to import. Maps to `client/fastLinkJS` in the sbt task graph.

## Manual chunks — what we lazy-load and why

Already covered in [Client Stack](/cortex/cortex-onboarding/deep-dive-client-stack#bundle-splitting), but worth repeating because the trade-off is the entire reason the book index loads fast:

```javascript
manualChunks(id) {
  if (id.includes("node_modules")) {
    if (id.includes("/shiki") || id.includes("/@shikijs/")) return "shiki";
    if (id.includes("/mermaid")) return "mermaid";
    if (id.includes("/@terrastruct/d2")) return "d2";
    if (id.includes("/katex")) return "katex";
    if (id.includes("/prismjs")) return "prismjs";
  }
  return null;
}
```

**Without `manualChunks`:** Rollup inlines these into the main bundle. The first-load bundle goes from ~400KB to ~5MB. First paint slows by seconds on a slow connection.

**With `manualChunks`:** each library gets its own file. The TypeScript pipeline `await import("...")`s them only when a chapter actually contains a relevant block. The book index at `/` and the blog index at `/blogs` never touch them.

The `chunkSizeWarningLimit: 800` is bumped because these chunks are *intentionally* large and the default 500KB warning would always fire.

**If you bump a heavy dep without checking the home page bundle:** run `npm run build` after the upgrade and look at `dist/assets/*.js` sizes. A 200KB regression on the home chunk is a regression worth catching.

## Tailwind v4 — the v3 → v4 migration scrubbed

Tailwind v4 dropped support for `tailwind.config.ts` in favour of CSS-first config. Everything moves into the stylesheet:

```css
@import "tailwindcss";
@plugin "@tailwindcss/typography";
@source "../client/target/scala-3.6.2/cortex-client-fastopt";
@source "../client/target/scala-3.6.2/cortex-client-opt";
@custom-variant dark (&:is(.dark *));
@theme inline { /* color tokens */ }
@utility container { margin-inline: auto; }
@layer components { /* @apply rules for BEM blocks */ }
@import "./src/styles/sections/...";
```

Three v4-specific things to know:

- **`@source` directives.** Tailwind needs to scan files for class usage. v4's default config scans `client/` source. The Scala.js linker output lives in `client/target/...`, which v4 doesn't scan by default — we add explicit `@source` directives so utility classes used in Scala source aren't tree-shaken away.
- **`@apply` is stricter.** v3 silently dropped unknown utilities; v4 errors at the Vite plugin and serves a blank page. Migration scrubbed several typos (`text-2sm`, `items-left`, `custom-icon`). Don't `@apply` marker classes like `group` and `container` — they have zero CSS in v4 and must stay literal in markup.
- **`container` lost auto-centering.** v3's `theme.container.center: true` is gone. We restore it with the `@utility container { margin-inline: auto; }` rule. Removing it makes everything go left-flush with no error.

**If you upgrade Tailwind without re-checking `@apply` usage:** the page goes blank. Vite logs the rejected utility. Fix and reload.

## Why Liquibase YAML

Migrations are versioned SQL files registered in a YAML master changelog. The master is `server/src/main/resources/db/changelog/db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: changes/v1-init.sql
      relativeToChangelogFile: true
```

The included `changes/v1-init.sql`:

```sql
--liquibase formatted sql
--changeset cortex:1-create-visits
CREATE TABLE visits (id INT PRIMARY KEY, count BIGINT NOT NULL);
INSERT INTO visits (id, count) VALUES (1, 0);
--rollback DROP TABLE visits;
```

Why Liquibase over Flyway?

| | Liquibase | Flyway |
|---|---|---|
| Format | YAML / XML / JSON / SQL | SQL only (free tier) |
| Rollback | Inline annotation | Pro tier |
| Multi-DB | Yes | Yes |
| Tracking | `databasechangelog` table | `flyway_schema_history` |
| Extensibility | Java + XML | Java callbacks |

For a personal project, both work. Liquibase wins on rollback annotations (`--rollback DROP TABLE`) being free, and on the YAML master allowing flexible inclusion. If you have a specific allergy to YAML, Flyway's "just SQL" approach is simpler.

The annoying part: **Liquibase logs through `java.util.logging`**, and sbt-revolver tags everything on stderr as `[ERROR]`. We bridge JUL→SLF4J in `Main.scala` (`SLF4JBridgeHandler.install()`) — without that bridge, every dev startup looks alarming. Covered in [Server Stack](/cortex/cortex-onboarding/deep-dive-server-stack#liquibase-yaml--julslf4j-bridge).

**If you switch to Flyway:** trivially possible, but you lose inline rollback annotations and you're now writing SQL files only. Adapt to taste.

## scalafmt — the format choices that matter

`.scalafmt.conf`:

```hocon
version = 3.8.3
runner.dialect = scala3
maxColumn = 110

align.preset = more
align.openParenCallSite = false
align.openParenDefnSite = false

newlines.source = keep
docstrings.style = Asterisk

rewrite.rules = [RedundantBraces, RedundantParens, SortModifiers]
rewrite.scala3.convertToNewSyntax = true
rewrite.scala3.removeOptionalBraces = no

project.git = true
project.excludePaths = ["glob:**/target/**", "glob:**/src_managed/**"]
```

Three decisions worth understanding:

- **`maxColumn = 110`** — wider than the common 100. Scala 3's `for ... yield ...` and ZIO type signatures are long; 100 forces awkward breaks. 110 is wide enough for clarity, narrow enough to stay readable in a side-by-side diff.
- **`removeOptionalBraces = no`** — Scala 3 supports brace-less syntax (`if x then y else z`), but we keep braces in cases where they help (typically nested expressions). The flag *would* remove them aggressively; we disable it.
- **`project.git = true`** — only formats files tracked by git. Ignores `target/`, `.bloop/`, `node_modules/`, etc. Faster than full directory scans.
- **`project.excludePaths` exclude `src_managed`** — the codegen-emitted files don't go through scalafmt. They're regenerated on every build, so reformatting them is wasted work.

**If you bump `maxColumn` to 120:** lines that currently fit get reformatted to be wider; existing diffs become noisier on the next format pass. Pick a value, stick to it.

**If you remove the `src_managed` exclude:** scalafmt runs over codegen output. The next codegen run reformats it back. You get a lint loop.

## bin/dev — what makes it different from running things by hand

`./bin/dev` is what you'll run every day. Read it as a sequence of stages:

1. **Pre-flight** — kills any process holding `:8080` or `:5173`. Without this, a previous session's leftover JVM (sbt-revolver's child) holds the port and the next startup fails with "address already in use".
2. **Backing stores** — `docker compose up -d db redis mongo go-judge` and waits for healthchecks.
3. **Frontend deps** — `npm install` once on first run.
4. **sbt watcher** — `sbt -no-colors "~ all server/reStart client/fastLinkJS"` runs in the background. The `~` watches files; `all` runs both tasks per change; `server/reStart` keeps the server running.
5. **Vite dev server** — `cd client && npm run dev`.
6. **Cleanup trap** — `trap cleanup EXIT INT TERM` ensures Ctrl-C kills both processes *and* releases ports. Without the trap, `bin/dev` becomes a zombie factory.

Output streams are prefixed:

```bash
prefix() { sed -u -e "s/^/${colour}[${label}]${c_reset} /"; }
```

`[sbt]`, `[vite]`, `[dev]` — different colours per stream. Easier to scan than three terminals.

**If you start the components manually instead of via `bin/dev`:** you'll forget the cleanup trap, hold ports, and curse. The script exists to encode the dance.

## The Dockerfile — multi-stage, JRE-only runtime

`Dockerfile`:

**Stage 1 (`builder`):** `sbtscala/scala-sbt:eclipse-temurin-21.0.5_11_1.10.7_3.6.2`. Includes JDK 21, sbt 1.10.7, Scala 3.6.2 — pinned versions so the build is reproducible. Adds Node 20 for the Vite step.

```dockerfile
COPY project/build.properties project/plugins.sbt build.sbt ./
RUN sbt update                 # ← caches deps before src
COPY api shared server client content ./
RUN sbt "server/Universal/stage" "client/fullLinkJS"
WORKDIR /build/client
RUN npm install --no-audit --no-fund && npm run build
```

The deps copy + `sbt update` happens *before* the source copy. Docker layer caching means: if `build.sbt` doesn't change, the dep download isn't re-run. Big win for image build time (the dep step takes minutes; the source compile takes seconds).

**Stage 2 (`runtime`):** `eclipse-temurin:21-jre-jammy` — JRE only, no JDK. Smaller image.

```dockerfile
COPY --from=builder /build/server/target/universal/stage /app
COPY --from=builder /build/client/dist /app/static
COPY --from=builder /build/content     /app/content
ENV STATIC_DIR=/app/static
ENV CORTEX_ROOT=/app/content/cortex
ENV PORT=8080
CMD ["bin/cortex-server"]
```

Three artefacts copied from the builder: server launcher, frontend bundle, and the content tree (`cortex/` books + `blogs/` posts). No source, no sbt, no Node — the runtime image is ~200MB instead of ~2GB.

**If you collapse to a single stage:** the runtime image carries sbt, the JDK, Node, and `node_modules/`. Image size balloons. CVE surface area grows. Don't.

## The dependency cache trick

```dockerfile
COPY project/build.properties project/plugins.sbt build.sbt ./
RUN sbt update
COPY api shared server client content ./
RUN sbt "server/Universal/stage" "client/fullLinkJS"
```

The order matters. The first three files (`project/build.properties`, `plugins.sbt`, `build.sbt`) define dependencies. They change rarely. Copying them first and running `sbt update` caches the dep download in a separate Docker layer.

The actual source (`api/`, `shared/`, etc.) changes constantly. It goes in a later layer. Docker only re-runs from the first changed layer onwards — so a "code-only" rebuild reuses the cached `sbt update` layer and skips the dep download.

**If you reverse the order (source first, deps after):** every code change re-downloads all deps. CI build times double or triple.

## Where to look first when the build breaks

| Symptom | First place |
|---|---|
| `port already in use :8080` | `bin/dev` pre-flight didn't run; `kill $(lsof -ti:8080)` and retry |
| HMR not working in Vite | Scala.js linker didn't run — check sbt watcher is alive |
| Tailwind compile blank-screens | `client/src/styles/**/*.css` — typo in `@apply` |
| Docker image > 1GB | Single-stage build leaked sbt + Node into runtime |
| Docker rebuild takes minutes for no code change | Layer order broken; source copy ahead of `sbt update` |
| Liquibase logs flagged red | `Main.scala` SLF4JBridge missing |
| `sbt scalafmtCheckAll` fails on src_managed | `.scalafmt.conf` exclude broken |

## The one-line summary

The build toolchain is six sbt plugins, two Vite plugins, one Tailwind v4 entry file, one Liquibase YAML, and one Dockerfile — each with one sharp responsibility. The interesting choices are around *isolation*: forked JVMs for `reStart`, layer-cached deps in Docker, lazy chunks in the bundle. Once you've internalised "every tool here owns exactly one slice", debugging the build means picking the right tool, not understanding the whole pipeline.
