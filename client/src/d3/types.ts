// Flat type aliases over the openapi-typescript output (ADR-0026).
//
// `types.generated.ts` is generated from `viz-schema.yaml` via
// `npm run codegen:viz` and nests every schema under
// `components["schemas"][...]`. The whole d3 renderer imports flat names
// (`import type { VizGraph } from "./types"`), so this thin, STABLE shim
// re-exports each schema as a flat alias. Edit `viz-schema.yaml` + rerun
// codegen to change a shape; this file only changes when a TYPE is
// added/removed.
//
// Scala side stays hand-written (`shared/.../viz/VizGraph.scala`, circe
// Encoder); `VizSchemaConformanceSpec` asserts the case-class fields match
// `viz-schema.yaml`, so Scala ↔ yaml ↔ TS cannot silently drift.

import type { components } from "./types.generated";

type Schemas = components["schemas"];

export type VizField = Schemas["VizField"];
export type VizNode = Schemas["VizNode"];
export type VizEdge = Schemas["VizEdge"];
export type VizCursor = Schemas["VizCursor"];
export type Annotation = Schemas["Annotation"];
export type VizLocal = Schemas["VizLocal"];
export type VizFrame = Schemas["VizFrame"];
export type VizGraphStep = Schemas["VizGraphStep"];
export type VizGraph = Schemas["VizGraph"];
export type VizCases = Schemas["VizCases"];
