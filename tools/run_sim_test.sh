#!/bin/sh
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=${TMPDIR:-/tmp}/bolidalarmpro-sim-build
rm -rf "$OUT"
mkdir -p "$OUT"
find "$ROOT/src/main/java/ru/bolidalarmpro/sim" \
     "$ROOT/src/test/java/ru/bolidalarmpro/sim" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 1.7 -target 1.7 -d "$OUT" @"$OUT/sources.txt"
java -cp "$OUT" ru.bolidalarmpro.sim.SimSelfTest
