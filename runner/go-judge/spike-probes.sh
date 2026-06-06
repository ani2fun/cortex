#!/usr/bin/env bash
# Gate G1 / G3a probes for the go-judge code-execution backend.
#
# Validates that the sandbox can (1) run Python, (2) see the system Java
# compiler, and — critically — (3) perform the Visualise tracer's runtime
# in-process Java compilation (javax.tools at runtime: compile -> load ->
# invoke). Probe 3 is the exact mechanism that gave NZEC on Piston.
#
# Usage:
#   ./spike-probes.sh [BASE_URL]          # default http://localhost:5050
#   # in-cluster (Gate G3a):
#   kubectl -n apps-prod port-forward svc/go-judge 5050:5050 &
#   ./spike-probes.sh http://localhost:5050
#
# Exits 0 only if all probes return status=Accepted with the expected output.
set -uo pipefail
BASE="${1:-http://localhost:5050}"
fail=0

# run NAME REQUEST_JSON EXPECTED_SUBSTRING
run() {
  local name="$1" req="$2" expect="$3" resp status out
  resp="$(curl -s -m 60 "$BASE/run" -H 'Content-Type: application/json' -d "$req")"
  status="$(printf '%s' "$resp" | jq -r '.[0].status // "ERR"' 2>/dev/null)"
  out="$(printf '%s' "$resp" | jq -r '.[0].files.stdout // ""' 2>/dev/null)"
  if [ "$status" = "Accepted" ] && printf '%s' "$out" | grep -q "$expect"; then
    echo "PASS  $name  ($status; stdout: $(printf '%s' "$out" | tr '\n' '|'))"
  else
    echo "FAIL  $name  status=$status"
    printf '%s' "$resp" | jq -c '.[0]|{status,exitStatus,stdout:.files.stdout,stderr:.files.stderr,error}' 2>/dev/null || printf '%s\n' "$resp"
    fail=1
  fi
}

PROBE2='public class Main { public static void main(String[] a){
  javax.tools.JavaCompiler c = javax.tools.ToolProvider.getSystemJavaCompiler();
  System.out.println("compiler=" + (c==null?"NULL":"PRESENT:"+c.getClass().getSimpleName()));
  System.out.println("version="+System.getProperty("java.version")); } }'

PROBE3='import javax.tools.*; import java.net.*; import java.util.*; import java.io.*;
public class Main { public static void main(String[] args) throws Exception {
  String src = "public class Gen { public static String hi(){ return \"compiled-at-runtime-OK\"; } }";
  JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
  if (compiler == null) { System.out.println("NO_COMPILER"); return; }
  File tmp = java.nio.file.Files.createTempDirectory("cf").toFile();
  File srcFile = new File(tmp, "Gen.java");
  try (Writer w = new FileWriter(srcFile)) { w.write(src); }
  DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
  StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null);
  Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(srcFile);
  boolean ok = compiler.getTask(null, fm, diags, Arrays.asList("-d", tmp.getAbsolutePath()), null, units).call();
  System.out.println("compileOK=" + ok);
  if (!ok) { for (Diagnostic<?> d : diags.getDiagnostics()) System.out.println(d); return; }
  URLClassLoader cl = new URLClassLoader(new URL[]{ tmp.toURI().toURL() });
  Class<?> gen = Class.forName("Gen", true, cl);
  System.out.println("runtimeInvoke=" + gen.getMethod("hi").invoke(null)); } }'

P1=$(jq -nc --arg s "print('ok from go-judge')" '{cmd:[{args:["/usr/bin/python3","-c",$s],
  env:["PATH=/usr/bin:/bin","HOME=/w"],files:[{content:""},{name:"stdout",max:65536},{name:"stderr",max:65536}],
  cpuLimit:10000000000,clockLimit:12000000000,memoryLimit:268435456,procLimit:128}]}')

P2=$(jq -nc --arg s "$PROBE2" '{cmd:[{args:["/bin/sh","-c","javac Main.java && java -cp . Main"],
  env:["PATH=/usr/bin:/bin","HOME=/w"],files:[{content:""},{name:"stdout",max:65536},{name:"stderr",max:65536}],
  cpuLimit:20000000000,clockLimit:25000000000,memoryLimit:536870912,procLimit:256,copyIn:{"Main.java":{content:$s}}}]}')

P3=$(jq -nc --arg s "$PROBE3" '{cmd:[{args:["/bin/sh","-c","javac Main.java && java -cp . Main"],
  env:["PATH=/usr/bin:/bin","HOME=/w"],files:[{content:""},{name:"stdout",max:65536},{name:"stderr",max:65536}],
  cpuLimit:30000000000,clockLimit:35000000000,memoryLimit:536870912,procLimit:256,copyIn:{"Main.java":{content:$s}}}]}')

echo "go-judge probes against $BASE"
run "python-liveness"                  "$P1" "ok from go-judge"
run "java-jdk-compiler-present"        "$P2" "compiler=PRESENT"
run "java-runtime-compile-load-invoke" "$P3" "runtimeInvoke=compiled-at-runtime-OK"
echo "---"
[ "$fail" = 0 ] && echo "GATE G1: PASS" || echo "GATE G1: FAIL"
exit $fail
