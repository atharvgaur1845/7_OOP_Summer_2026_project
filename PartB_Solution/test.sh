#!/usr/bin/env bash
# Compile and run the JUnit 5 test suite using the bundled standalone console launcher.
source "$(dirname "$0")/_env.sh"

if [[ ! -f "$JUNIT" ]]; then
  echo "ERROR: $JUNIT not found. Place junit-platform-console-standalone.jar in lib/." >&2
  exit 1
fi

# Ensure main classes are built first.
"$PROJECT_DIR/compile.sh"

echo "Compiling tests…"
TEST_LIST="$(mktemp)"
find "$SRC_TEST" -name '*.java' > "$TEST_LIST"

TEST_CP="$OUT:$JUNIT"
[[ -f "$GSON" ]] && TEST_CP="$TEST_CP:$GSON"

"$JAVAC" --release 17 -d "$OUT" -cp "$TEST_CP" @"$TEST_LIST"
rm -f "$TEST_LIST"

echo "Running tests…"
RUN_CP="$OUT"
[[ -f "$GSON" ]] && RUN_CP="$RUN_CP:$GSON"

exec "$JAVA" -jar "$JUNIT" --class-path "$RUN_CP" --scan-classpath --details=tree \
  --disable-banner
