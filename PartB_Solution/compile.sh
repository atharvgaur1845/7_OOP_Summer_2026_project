#!/usr/bin/env bash
# Compile all main Java sources into out/ (targets Java 17 bytecode for portability).
source "$(dirname "$0")/_env.sh"

echo "Using javac: $JAVAC"
"$JAVAC" -version

rm -rf "$OUT"
mkdir -p "$OUT"

CP_COMPILE="$OUT"
[[ -f "$GSON" ]] && CP_COMPILE="$CP_COMPILE:$GSON"

# Collect sources into an argfile to stay within command-line limits.
SRC_LIST="$(mktemp)"
find "$SRC_MAIN" -name '*.java' > "$SRC_LIST"

"$JAVAC" --release 17 -d "$OUT" -cp "$CP_COMPILE" @"$SRC_LIST"
rm -f "$SRC_LIST"

echo "Compiled $(find "$OUT" -name '*.class' | wc -l) classes into $OUT"
