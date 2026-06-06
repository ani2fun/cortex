package codefolio.client.components.cortex

import scala.scalajs.js

/**
 * The one robust Python tracer (ADR-0018). Wraps user Python in a `sys.settrace` harness that captures, per
 * step, a **heap snapshot** — the reachable object graph, not just `repr` strings — so the same trace can
 * drive both the `python trace` step-through ([[TracedCodeBlock]]) and the engine-free Visualise diagram
 * ([[VisualiseModal]]).
 *
 * The server-side `/api/run` is unchanged: the wrapped program prints its trace as JSON between
 * `__CFHEAP_BEGIN__` / `__CFHEAP_END__` markers, after the user's own stdout. [[parse]] splits the two back
 * apart via the shared [[MarkedTrace]] decoder and returns a [[MarkedTrace.TraceResult]].
 */
object PythonTracer:

  private val BeginMarker = "__CFHEAP_BEGIN__"
  private val EndMarker   = "__CFHEAP_END__"

  private def base64Encode(s: String): String =
    js.Dynamic.global
      .btoa(js.Dynamic.global.unescape(js.Dynamic.global.encodeURIComponent(s)))
      .asInstanceOf[String]

  // ---------------------------------------------------------------------------
  // Harness — wraps user source in a sys.settrace tracer that snapshots the heap
  // each step and dumps {steps, truncated} as JSON between the marker pair.
  // ---------------------------------------------------------------------------

  /** Wrap user Python in the heap-snapshot tracer harness. The result is a runnable Python program. */
  def wrap(userSource: String): String =
    val encoded = base64Encode(userSource)
    s"""import sys, json, base64, math, types
       |
       |_cf_source = base64.b64decode("$encoded").decode("utf-8")
       |_cf_steps = []
       |_cf_truncated = [False]
       |_cf_step_limit = 600
       |_cf_max_objects = 400
       |_cf_max_depth = 60
       |_cf_max_payload = 512 * 1024
       |
       |# Modules whose objects are stdlib/library internals — not user data. We render
       |# instances of types from these modules as opaque (no field recursion), so
       |# `from collections import deque` / `from typing import Optional` don't drag the
       |# entire metaclass tree into every heap snapshot.
       |_cf_opaque_modules = frozenset((
       |    "typing", "_collections_abc", "collections.abc", "abc",
       |    "_typeshed", "_collections", "_weakrefset", "weakref",
       |))
       |
       |# True when `v` is class/module/function/built-in machinery that we want to
       |# represent as a single opaque node — no `__dict__` recursion. Without this guard,
       |# importing `deque` would dump deque's ~50 method/wrapper entries into the heap
       |# every step, blowing the trace past the 512 KB payload cap and starving the
       |# `autoDetectRoot` heuristic (which then picks `deque` as the visualisation root).
       |def _cf_is_opaque(v):
       |    if isinstance(v, type): return True
       |    if isinstance(v, types.ModuleType): return True
       |    if isinstance(v, (types.FunctionType, types.BuiltinFunctionType,
       |                       types.MethodType, types.BuiltinMethodType,
       |                       types.MethodWrapperType, types.WrapperDescriptorType,
       |                       types.MethodDescriptorType, types.GetSetDescriptorType,
       |                       types.MemberDescriptorType)):
       |        return True
       |    mod = getattr(type(v), "__module__", "")
       |    return mod in _cf_opaque_modules
       |
       |def _cf_scalar(v):
       |    if v is None or isinstance(v, bool) or isinstance(v, int):
       |        return (True, v)
       |    if isinstance(v, float):
       |        return (True, v if math.isfinite(v) else repr(v))
       |    if isinstance(v, str):
       |        return (True, v if len(v) <= 80 else v[:80] + "\\u2026")
       |    return (False, None)
       |
       |# Snapshot the full call stack — a list of (fn_name, locals_items) pairs, innermost first — into
       |# the ADR-0021 frames/heap shape. One shared heap across frames so an object referenced from two
       |# frames (a closure capture, a recursion's outer-frame argument) is a single node, not duplicated.
       |def _cf_snapshot(frame_specs):
       |    heap = {}
       |    def visit(v, depth):
       |        is_s, sv = _cf_scalar(v)
       |        if is_s:
       |            return sv
       |        oid = str(id(v))
       |        if oid in heap:
       |            return {"ref": oid}
       |        if len(heap) >= _cf_max_objects or depth >= _cf_max_depth:
       |            _cf_truncated[0] = True
       |            return {"ref": oid}
       |        # Render classes / modules / functions / typing-internals as opaque — see
       |        # `_cf_is_opaque`. The object still gets an id and appears once in the heap,
       |        # so any frame local referencing it still has something to point at; we just
       |        # don't recurse into its `__dict__`.
       |        if _cf_is_opaque(v):
       |            heap[oid] = {"type": "object", "cls": type(v).__name__, "fields": {}}
       |            return {"ref": oid}
       |        heap[oid] = None
       |        if isinstance(v, (list, tuple)):
       |            kind = "list" if isinstance(v, list) else "tuple"
       |            heap[oid] = {"type": kind,
       |                         "items": [visit(x, depth + 1) for x in list(v)[:_cf_max_objects]]}
       |        elif isinstance(v, dict):
       |            entries = []
       |            for dk, dv in list(v.items())[:_cf_max_objects]:
       |                entries.append([visit(dk, depth + 1), visit(dv, depth + 1)])
       |            heap[oid] = {"type": "dict", "entries": entries}
       |        else:
       |            d = getattr(v, "__dict__", None)
       |            if d is None:
       |                d = {}
       |                for sl in (getattr(type(v), "__slots__", ()) or ()):
       |                    if isinstance(sl, str) and hasattr(v, sl):
       |                        d[sl] = getattr(v, sl)
       |            fields = {}
       |            for fk, fv in list(d.items()):
       |                if isinstance(fk, str) and not fk.startswith("_cf_"):
       |                    fields[fk] = visit(fv, depth + 1)
       |            heap[oid] = {"type": "object", "cls": type(v).__name__, "fields": fields}
       |        return {"ref": oid}
       |    frames_out = []
       |    for fn_name, items in frame_specs:
       |        locs = {}
       |        for k, v in items:
       |            if isinstance(k, str) and not k.startswith("_cf_") and not k.startswith("__"):
       |                locs[k] = visit(v, 0)
       |        frames_out.append({"fn": fn_name, "locals": locs})
       |    return frames_out, heap
       |
       |# Walk frame.f_back to collect every traced-file frame on the call stack, innermost first. Frames
       |# from the harness wrapper itself (filename != "<traced>") are skipped, so `frames[0]` is always
       |# the user's currently-executing function and `frames[-1]` is the user's outermost scope.
       |def _cf_collect_frames(frame):
       |    specs = []
       |    cur = frame
       |    while cur is not None:
       |        if cur.f_code.co_filename == "<traced>":
       |            specs.append((cur.f_code.co_name, list(cur.f_locals.items())))
       |        cur = cur.f_back
       |    return specs
       |
       |def _cf_tracer(frame, event, arg):
       |    if event in ("line", "call", "return") and frame.f_code.co_filename == "<traced>":
       |        if frame.f_lineno <= 0:
       |            return _cf_tracer
       |        try:
       |            frames_data, heap = _cf_snapshot(_cf_collect_frames(frame))
       |            _cf_steps.append({
       |                "line": frame.f_lineno,
       |                "event": event,
       |                "frames": frames_data,
       |                "heap": heap,
       |            })
       |        except Exception:
       |            pass
       |        if len(_cf_steps) >= _cf_step_limit:
       |            _cf_truncated[0] = True
       |            sys.settrace(None)
       |    return _cf_tracer
       |
       |try:
       |    _cf_compiled = compile(_cf_source, "<traced>", "exec")
       |    _cf_ns = {"__name__": "__main__"}
       |    sys.settrace(_cf_tracer)
       |    try:
       |        exec(_cf_compiled, _cf_ns)
       |    finally:
       |        sys.settrace(None)
       |finally:
       |    while True:
       |        _cf_payload = json.dumps({"steps": _cf_steps, "truncated": _cf_truncated[0]})
       |        if len(_cf_payload) <= _cf_max_payload or len(_cf_steps) <= 1:
       |            break
       |        # Drop the LAST quarter (not the first) — keeps the algorithm's initial
       |        # state and early progression, matching the "showing the first part of the
       |        # run" banner. The tail's converged state is usually less informative than
       |        # the setup + first iterations.
       |        _cf_steps = _cf_steps[:-(len(_cf_steps) // 4 + 1)]
       |        _cf_truncated[0] = True
       |    sys.stdout.write("\\n$BeginMarker")
       |    sys.stdout.write(_cf_payload)
       |    sys.stdout.write("$EndMarker\\n")
       |""".stripMargin

  // ---------------------------------------------------------------------------
  // Parsing — split the marker-delimited JSON out of stdout and decode it.
  // ---------------------------------------------------------------------------

  /**
   * Split the harness output: everything before `__CFHEAP_BEGIN__` is the user's program stdout; the JSON
   * between the markers is the trace. Missing markers (compile error, fatal harness exception) → an empty
   * trace and the raw stdout as program output. Delegates to [[MarkedTrace.parse]].
   */
  def parse(stdout: String): MarkedTrace.TraceResult = MarkedTrace.parse(stdout)
