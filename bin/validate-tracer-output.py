#!/usr/bin/env python3
"""Phase 0 deliverable 5 / gap 1 (ADR-0025) — tracer-output schema gate.

The Python tracer (`PythonTracer.scala`) and Java tracer agent (`tools/jvm_tracer/`)
both emit step JSON between `__CFHEAP_BEGIN__` / `__CFHEAP_END__` markers. Phase 1
locks the schema for this payload in `viz-schema.yaml`; if either tracer drifts
from that schema, every user's Visualise click breaks at decode time — silently
in production, since today's `MarkedTrace.parse` swallows decode errors as
`Try(...).getOrElse(empty)`.

THIS SCRIPT IS THE CI GATE that catches the drift before it ships. Phase 0
delivers the SCAFFOLD; Phase 1 wires it to actual tracer invocations.

Usage
-----
    # validate a pre-captured trace JSON (between-markers payload only) against
    # the current tracer-output schema:
    python3 bin/validate-tracer-output.py path/to/captured-trace.json

    # run the built-in self-test (a tiny inline fixture covering every variant
    # of HeapObject + HeapValue + HeapScalar):
    python3 bin/validate-tracer-output.py --self-test

    # validate everything in client/test/e2e/fixtures/ (the Phase-0 adapter
    # OUTPUT fixtures — these are VizCases, NOT raw tracer output, so they
    # will FAIL the gate by design; see "Schema scope" below):
    python3 bin/validate-tracer-output.py --check-adapter-fixtures

Schema scope
------------
The schema in this script targets the RAW TRACER OUTPUT (the JSON the Python or
Java tracer prints between the markers — what `MarkedTrace.decodeTrace` consumes).
That payload has a DIFFERENT shape from `VizCases` (the adapter OUTPUT consumed by
the d3 renderer):

  TRACER payload                          ADAPTER (VizCases) payload
  ------------------------------          --------------------------
  {steps:[{line, event,                   {cases: [{steps: [{nodes:[...],
           frames:[{fn, locals:{...}}],            edges:[...], cursor:[...],
           heap:{id: {type, cls,                   annotation, ...}], ...}],
                       fields:{...}}}],                                  ...}
   truncated}

This script validates the TRACER payload. The Phase-0 trace-shapes Playwright
tests (`client/test/e2e/trace-shapes.spec.ts`) validate the ADAPTER payload by
rendering it. The two together cover the round-trip.

Phase 1 wiring
--------------
Phase 1 extends this script to:
  1. Invoke `PythonTracer.wrap` against a canonical tree-insert program, run it
     via python3, capture the markered JSON, validate it.
  2. Invoke `tools/jvm_tracer/run_probe.sh` against an iterative BFS Java
     program, capture its markered JSON, validate it.
  3. Either gate succeeding is what makes `viz-schema.yaml` the SINGLE SOURCE
     OF TRUTH for both tracers; Phase 2's server-side `circe.decode[HeapTrace]`
     stops being a runtime liability.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

try:
    from jsonschema import Draft202012Validator, validators  # type: ignore[import-untyped]
except ImportError:
    sys.stderr.write(
        "[validate-tracer-output] missing dep `jsonschema` — install with:\n"
        "    pip3 install jsonschema\n"
    )
    sys.exit(2)


# ─── Tracer-output schema ────────────────────────────────────────────────────
#
# Mirrors what `MarkedTrace.decodeTrace` expects today (legacy shape, the one
# Phase 1 will reshape to match `viz-schema.yaml`). The HeapObject discrim is
# `type` (not `kind` — that's the Phase 1 prototype convention); the variants
# are lowercased (`list` / `tuple` / `array` / `dict` / `instance`).
#
# Locals are emitted as a `{name: value}` object (not a list of tuples) because
# the Python tracer uses `_cf_snapshot` to flatten a Python `locals()` mapping.
# Values are either:
#   - JSON scalars (number / string / bool / null) — leaf primitives,
#   - `{"ref": "<oid>"}` — reference into the heap.

TRACER_SCHEMA: dict[str, Any] = {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "required": ["steps"],
    "additionalProperties": False,
    "properties": {
        "steps": {
            "type": "array",
            "items": {"$ref": "#/$defs/Step"},
        },
        "truncated": {"type": "boolean"},
    },
    "$defs": {
        "Step": {
            "type": "object",
            "required": ["line", "frames", "heap"],
            "additionalProperties": False,
            "properties": {
                "line":  {"type": "integer", "minimum": 0},
                "event": {"type": "string", "enum": ["line", "call", "return", "exception"]},
                "frames": {
                    "type": "array",
                    "items": {"$ref": "#/$defs/Frame"},
                },
                "heap": {
                    "type": "object",
                    "patternProperties": {"^.+$": {"$ref": "#/$defs/HeapObject"}},
                    "additionalProperties": False,
                },
            },
        },
        "Frame": {
            "type": "object",
            "required": ["fn", "locals"],
            "additionalProperties": False,
            "properties": {
                "fn":     {"type": "string"},
                "locals": {
                    "type": "object",
                    "patternProperties": {".+": {"$ref": "#/$defs/Value"}},
                    "additionalProperties": False,
                },
            },
        },
        "Value": {
            # A JSON scalar OR a {"ref": "<oid>"} object.
            "oneOf": [
                {"type": ["number", "string", "boolean", "null"]},
                {
                    "type": "object",
                    "required": ["ref"],
                    "additionalProperties": False,
                    "properties": {"ref": {"type": "string"}},
                },
            ],
        },
        "HeapObject": {
            "oneOf": [
                {"$ref": "#/$defs/HeapInstance"},
                {"$ref": "#/$defs/HeapArr"},
                {"$ref": "#/$defs/HeapDict"},
            ],
        },
        "HeapInstance": {
            "type": "object",
            "required": ["type", "cls", "fields"],
            "additionalProperties": False,
            "properties": {
                "type": {"const": "instance"},
                "cls":  {"type": "string"},
                "fields": {
                    "type": "object",
                    "patternProperties": {".+": {"$ref": "#/$defs/Value"}},
                    "additionalProperties": False,
                },
            },
        },
        "HeapArr": {
            "type": "object",
            "required": ["type", "items"],
            "additionalProperties": False,
            "properties": {
                "type":  {"enum": ["list", "tuple", "array"]},
                "items": {
                    "type": "array",
                    "items": {"$ref": "#/$defs/Value"},
                },
            },
        },
        "HeapDict": {
            "type": "object",
            "required": ["type", "entries"],
            "additionalProperties": False,
            "properties": {
                "type": {"const": "dict"},
                "entries": {
                    "type": "array",
                    "items": {
                        "type": "array",
                        "minItems": 2,
                        "maxItems": 2,
                        "items": {"$ref": "#/$defs/Value"},
                    },
                },
            },
        },
    },
}


def make_validator() -> Draft202012Validator:
    Draft202012Validator.check_schema(TRACER_SCHEMA)
    return Draft202012Validator(TRACER_SCHEMA)


def validate_payload(name: str, payload: dict[str, Any], expect_pass: bool = True) -> bool:
    """Return True if the outcome matches `expect_pass`, else False.

    Prints PASS/FAIL with the actual outcome. When `expect_pass=False`, a
    schema-rejection is the expected outcome and we print
    `REJECTED-AS-EXPECTED` instead of `FAIL` to keep the log readable.
    """
    v = make_validator()
    errors = sorted(v.iter_errors(payload), key=lambda e: list(e.absolute_path))
    actually_passes = not errors
    if actually_passes:
        tag = "PASS" if expect_pass else "UNEXPECTED-PASS"
        print(f"[validate-tracer-output] {tag} {name}")
        return expect_pass
    tag = "FAIL" if expect_pass else "REJECTED-AS-EXPECTED"
    head = f"[validate-tracer-output] {tag} {name}"
    if expect_pass:
        print(f"{head} — {len(errors)} error(s):")
        for err in errors[:10]:
            path = "/".join(str(p) for p in err.absolute_path) or "<root>"
            print(f"  - at {path}: {err.message}")
        if len(errors) > 10:
            print(f"  - …and {len(errors) - 10} more")
    else:
        first = errors[0]
        path  = "/".join(str(p) for p in first.absolute_path) or "<root>"
        print(f"{head} (at {path}: {first.message})")
    return not expect_pass


# ─── Self-test fixture ───────────────────────────────────────────────────────
#
# Tiny inline trace exercising every HeapObject variant + every HeapScalar
# variant + at least one Ref. If this stops validating, the schema regressed.

SELF_TEST_FIXTURE: dict[str, Any] = {
    "steps": [
        {
            "line": 1,
            "event": "call",
            "frames": [
                {"fn": "main", "locals": {"n": 3, "name": "alice", "ok": True, "x": None}},
            ],
            "heap": {
                "root": {
                    "type": "instance",
                    "cls": "Node",
                    "fields": {"val": 1, "next": {"ref": "tail"}},
                },
                "tail": {
                    "type": "instance",
                    "cls": "Node",
                    "fields": {"val": 2, "next": None},
                },
                "buckets": {
                    "type": "list",
                    "items": [{"ref": "root"}, {"ref": "tail"}, 42, "hi"],
                },
                "tuple-test": {
                    "type": "tuple",
                    "items": [1, 2, 3],
                },
                "j-array": {
                    "type": "array",
                    "items": [True, False, None],
                },
                "counts": {
                    "type": "dict",
                    "entries": [["apple", 1], ["fig", {"ref": "root"}]],
                },
            },
        },
        {
            "line": 5,
            "event": "line",
            "frames": [
                {"fn": "main", "locals": {"n": 3, "name": "alice", "ok": True, "x": None}},
            ],
            "heap": {},
        },
    ],
    "truncated": False,
}


# ─── CLI ─────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("path", nargs="?", type=Path,
                        help="path to a JSON file containing the between-markers trace payload")
    parser.add_argument("--self-test", action="store_true",
                        help="validate the built-in fixture and exit (no file argument needed)")
    parser.add_argument("--check-adapter-fixtures", action="store_true",
                        help="ALSO validate client/test/e2e/fixtures/*.json — these are VizCases " +
                             "shapes (adapter OUTPUT) and SHOULD FAIL the tracer schema. Use to " +
                             "confirm the gate distinguishes tracer-output from adapter-output.")
    args = parser.parse_args()

    if not (args.self_test or args.path or args.check_adapter_fixtures):
        parser.print_help(sys.stderr)
        return 2

    all_pass = True
    if args.self_test:
        all_pass = validate_payload("<self-test>", SELF_TEST_FIXTURE) and all_pass

    if args.path:
        try:
            payload = json.loads(args.path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as e:
            print(f"[validate-tracer-output] FAIL {args.path}: {e}")
            return 1
        all_pass = validate_payload(str(args.path), payload) and all_pass

    if args.check_adapter_fixtures:
        repo_root = Path(__file__).resolve().parent.parent
        adapter_dir = repo_root / "client" / "test" / "e2e" / "fixtures"
        if not adapter_dir.exists():
            print(f"[validate-tracer-output] no adapter fixtures dir at {adapter_dir}")
        else:
            count = len(list(adapter_dir.glob("*.json")))
            print(f"[validate-tracer-output] expecting all {count} adapter fixture(s) to be REJECTED " +
                  "(adapter output ≠ tracer output by design):")
            for f in sorted(adapter_dir.glob("*.json")):
                payload = json.loads(f.read_text(encoding="utf-8"))
                # Negative test: an adapter fixture VALIDATING the tracer schema
                # means the schema is too permissive — the gate would let real
                # bugs through. `expect_pass=False` says "reject is the right
                # outcome".
                ok = validate_payload(f.name, payload, expect_pass=False)
                all_pass = ok and all_pass

    return 0 if all_pass else 1


if __name__ == "__main__":
    sys.exit(main())
