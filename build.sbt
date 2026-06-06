import org.scalajs.linker.interface.ModuleKind

ThisBuild / scalaVersion := "3.6.2"
ThisBuild / organization := "codefolio"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wvalue-discard",
  // Silence warnings from codegen-emitted sources — we don't author them.
  "-Wconf:src=.*src_managed/.*:s",
  // Scala 3's E198 (unused local definition) sometimes mis-attributes a
  // warning to a chained method's closing paren. Silence locally; revisit
  // when upstream improves the diagnostic.
  "-Wconf:msg=unused local definition:s"
)

// ---- Versions ------------------------------------------------------------

val tapirV        = "1.11.22"
val zioV          = "2.1.14"
val zioHttpV      = "3.0.1"
val zioConfigV    = "4.0.3"
val circeV        = "0.14.10"
val sttpClientV   = "3.10.2"
val scalajsReactV = "3.0.0"
val postgresV     = "42.7.4"
val hikariV       = "6.2.1"
val liquibaseV    = "4.30.0"
val logbackV      = "1.5.12"
val zioLogbackV   = "2.4.0"
val lettuceV      = "6.5.1.RELEASE"
val mongoV        = "5.2.1"
val nimbusJoseV   = "9.48"

// ---- shared --------------------------------------------------------------
//
// `shared` is cross-compiled for the JVM (server) and JS (client). The
// sbt-openapi-codegen plugin reads `api/openapi.yaml` and writes generated
// tapir endpoints + case classes into each platform's `src_managed` dir
// under the package `codefolio.shared.api`. Both server and client depend
// on this module, so the API spec is the single source of truth and a
// schema change breaks compilation on both sides.

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .enablePlugins(OpenapiCodegenPlugin)
  .settings(
    name := "codefolio-shared",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-core"       % tapirV,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % tapirV,
      "io.circe"                    %%% "circe-generic"    % circeV,
      "dev.zio"                     %%% "zio-test"         % zioV % Test,
      "dev.zio"                     %%% "zio-test-sbt"     % zioV % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    openapiSwaggerFile  := (ThisBuild / baseDirectory).value / "api" / "openapi.yaml",
    openapiPackage      := "codefolio.shared.api",
    openapiObject       := "Endpoints",
    openapiJsonSerdeLib := "circe"
  )

// Run the sharedJVM Test/runMain entries (e.g. genTraceFixtures) from the
// build root so any relative paths they write to resolve under the repo,
// not under `shared/.jvm/`.
lazy val sharedJVM = shared.jvm.settings(
  Test / run / baseDirectory := (LocalRootProject / baseDirectory).value,
  // JVM-only test dep: parse viz-schema.yaml in VizSchemaConformanceSpec
  // (asserts the hand-written VizGraph.scala fields match the yaml — ADR-0026).
  libraryDependencies += "org.yaml" % "snakeyaml" % "2.3" % Test
)

lazy val sharedJS = shared.js

// ---- server --------------------------------------------------------------

lazy val server = (project in file("server"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(sharedJVM)
  .settings(
    name                := "codefolio-server",
    Compile / mainClass := Some("codefolio.server.Main"),
    // Run the server with the build root as its working directory so the
    // default `./client/dist` static path (and any other relative path the
    // user puts in application.conf) resolves the same way under
    //   sbt server/run, sbt server/reStart, and bin/dev.
    Compile / run / baseDirectory := (LocalRootProject / baseDirectory).value,
    reStart / baseDirectory       := (LocalRootProject / baseDirectory).value,
    libraryDependencies ++= Seq(
      "dev.zio"                     %% "zio"                     % zioV,
      "dev.zio"                     %% "zio-http"                % zioHttpV,
      "com.softwaremill.sttp.tapir" %% "tapir-zio"               % tapirV,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % tapirV,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirV,
      "dev.zio"                     %% "zio-config"              % zioConfigV,
      "dev.zio"                     %% "zio-config-typesafe"     % zioConfigV,
      "dev.zio"                     %% "zio-config-magnolia"     % zioConfigV,
      "dev.zio"                     %% "zio-logging-slf4j2"      % zioLogbackV,
      "org.postgresql"               % "postgresql"              % postgresV,
      "com.zaxxer"                   % "HikariCP"                % hikariV,
      "org.liquibase"                % "liquibase-core"          % liquibaseV,
      "ch.qos.logback"               % "logback-classic"         % logbackV,
      // Bridge java.util.logging (used by Liquibase) -> SLF4J -> Logback,
      // so Liquibase INFO logs no longer show up as [ERROR] on stderr.
      "org.slf4j" % "jul-to-slf4j" % "2.0.16",
      // Redis (cache) and MongoDB (event log). Both Java drivers wrapped in
      // ZIO blocking effects, matching the JDBC pattern used for Postgres.
      "io.lettuce"  % "lettuce-core"        % lettuceV,
      "org.mongodb" % "mongodb-driver-sync" % mongoV,
      // OIDC token validation. Pure Java; fetches the JWKS lazily on first
      // verify (RemoteJWKSet's built-in 5-min cache), validates signature
      // + iss + exp + azp/aud. Wired by server/auth/KeycloakAuthBackend.
      "com.nimbusds" % "nimbus-jose-jwt" % nimbusJoseV,
      "dev.zio"     %% "zio-test"        % zioV % Test,
      "dev.zio"     %% "zio-test-sbt"    % zioV % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

// ---- client --------------------------------------------------------------
//
// ScalaJS module. The `client/` directory also hosts the Vite project
// (package.json, vite.config.mjs, index.html, main.js, tailwind.css). The
// `@scala-js/vite-plugin-scalajs` plugin imports the linker output from
// `client/target/...` via the `scalajs:main.js` virtual import.

lazy val client = (project in file("client"))
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(sharedJS)
  .settings(
    name                            := "codefolio-client",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    libraryDependencies ++= Seq(
      "com.github.japgolly.scalajs-react" %%% "core"              % scalajsReactV,
      "com.github.japgolly.scalajs-react" %%% "extra"             % scalajsReactV,
      "com.softwaremill.sttp.tapir"       %%% "tapir-sttp-client" % tapirV,
      "com.softwaremill.sttp.client3"     %%% "core"              % sttpClientV
    )
  )

// ---- root aggregate ------------------------------------------------------

lazy val root = (project in file("."))
  .aggregate(sharedJVM, sharedJS, server, client)
  .settings(
    name           := "codefolio",
    publish / skip := true
  )

// ---- command aliases -----------------------------------------------------

// `sbt validateCortexPayloads` — build-time canon check for every
// `d3 widget=linked-list` payload across content/cortex/**/*.md. Exits 1 on
// violations (with a per-violation report). Wired into CI alongside
// `scalafmtCheckAll`. The validator lives in the server module because that's
// where the cortex pipeline already imports circe; reuses the same JSON dep
// instead of pulling another into the build.
addCommandAlias(
  "validateCortexPayloads",
  "server/runMain codefolio.server.cortexPipeline.LinkedListCanonValidator"
)

// `sbt genTraceFixtures` — Phase 0 (ADR-0025). Adapts each HeapTraceFixtures
// entry through HeapToGraph and writes the resulting VizCases JSON to
// `client/test/e2e/fixtures/<name>.json`, ready for the Playwright trace-shapes
// tests to fetch and render. Lives in the shared test scope so it can see the
// (test-only) fixtures.
addCommandAlias(
  "genTraceFixtures",
  "sharedJVM/Test/runMain codefolio.shared.viz.GenTraceFixtures"
)
