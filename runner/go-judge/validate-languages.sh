#!/usr/bin/env bash
# Validate every language in Languages.scala runs in the go-judge sandbox.
# Each program prints a unique token; we assert status=Accepted + token in stdout.
#
# Usage: ./validate-languages.sh [BASE_URL]   (default http://localhost:5050)
# Scala is intentionally skipped: scala-cli fetches its toolchain via coursier
# (network) and go-judge sandboxes have no network — best-effort, tracked separately.
set -uo pipefail
BASE="${1:-http://localhost:5050}"
ENVJSON='["PATH=/usr/bin:/bin:/usr/local/bin","HOME=/w","GOCACHE=/w/.cache","GOPATH=/w/go"]'
fail=0

# probe LANG FILENAME SOURCE SH_COMMAND EXPECTED
probe() {
  local lang="$1" fname="$2" src="$3" cmd="$4" expect="$5" req resp status out
  req=$(jq -nc --arg f "$fname" --arg s "$src" --arg c "$cmd" --argjson e "$ENVJSON" '{cmd:[{
    args:["/bin/sh","-c",$c], env:$e,
    files:[{content:""},{name:"stdout",max:65536},{name:"stderr",max:65536}],
    cpuLimit:60000000000, clockLimit:90000000000, memoryLimit:1073741824, procLimit:256,
    copyIn:{($f):{content:$s}}
  }]}')
  resp=$(curl -s -m 95 "$BASE/run" -H 'Content-Type: application/json' -d "$req")
  status=$(printf '%s' "$resp" | jq -r '.[0].status // "ERR"' 2>/dev/null)
  out=$(printf '%s' "$resp" | jq -r '.[0].files.stdout // ""' 2>/dev/null)
  if [ "$status" = "Accepted" ] && printf '%s' "$out" | grep -q "$expect"; then
    echo "PASS  $lang"
  else
    echo "FAIL  $lang  status=$status"
    printf '%s' "$resp" | jq -c '.[0]|{status,exitStatus,stdout:.files.stdout,stderr:.files.stderr,error}' 2>/dev/null || printf '%s\n' "$resp"
    fail=1
  fi
}

echo "Validating languages against $BASE"
probe "python(71)"     main.py   "print('PY_OK')"                                              "python3 main.py"                                  "PY_OK"
probe "java(62)"       Main.java "public class Main{public static void main(String[] a){System.out.println(\"JAVA_OK\");}}" "javac Main.java && java -cp . Main"  "JAVA_OK"
probe "c(50)"          main.c    "#include <stdio.h>
int main(){printf(\"C_OK\\n\");return 0;}"                                                      "gcc main.c -o m && ./m"                           "C_OK"
probe "cpp(54)"        main.cpp  "#include <iostream>
int main(){std::cout<<\"CPP_OK\"<<std::endl;return 0;}"                                         "g++ main.cpp -o m && ./m"                         "CPP_OK"
probe "go(60)"         main.go   "package main
import \"fmt\"
func main(){fmt.Println(\"GO_OK\")}"                                                            "go run main.go"                                   "GO_OK"
probe "rust(73)"       main.rs   "fn main(){println!(\"RUST_OK\");}"                            "rustc -O main.rs -o m && ./m"                     "RUST_OK"
probe "kotlin(78)"     main.kt   "fun main(){println(\"KOTLIN_OK\")}"                           "kotlinc main.kt -include-runtime -d m.jar 2>/dev/null && java -jar m.jar"  "KOTLIN_OK"
probe "typescript(74)" main.ts   "const x: string = \"TS_OK\"; console.log(x);"                "tsx main.ts"                                      "TS_OK"
probe "javascript(63)" main.js   "console.log(\"JS_OK\")"                                       "node main.js"                                     "JS_OK"
probe "sql(82)"        main.sql  "SELECT 'SQL_OK';"                                             "sqlite3 :memory: < main.sql"                      "SQL_OK"
echo "---"
[ "$fail" = 0 ] && echo "ALL VALIDATED (Scala skipped — no-network caveat)" || echo "SOME FAILED (see above)"
exit $fail
