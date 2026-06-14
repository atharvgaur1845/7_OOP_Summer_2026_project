#!/usr/bin/env bash
# Shared environment detection sourced by compile.sh / run-cli.sh / run-gui.sh / test.sh.
# Locates a JDK (with javac) in this order: $JAVA_HOME, javac on PATH, common install dirs,
# then the JDK bundled with the VS Code "Language Support for Java" extension.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB="$PROJECT_DIR/lib"
OUT="$PROJECT_DIR/out"
SRC_MAIN="$PROJECT_DIR/src/main/java"
SRC_TEST="$PROJECT_DIR/src/test/java"
GSON="$LIB/gson.jar"
JUNIT="$LIB/junit-platform-console-standalone.jar"

find_jdk() {
  # 1) JAVA_HOME
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/javac" ]]; then
    echo "$JAVA_HOME/bin"; return 0
  fi
  # 2) javac already on PATH
  if command -v javac >/dev/null 2>&1; then
    dirname "$(command -v javac)"; return 0
  fi
  # 3) Common Linux JDK locations
  for d in /usr/lib/jvm/*/bin /usr/java/*/bin /opt/jdk*/bin; do
    [[ -x "$d/javac" ]] && { echo "$d"; return 0; }
  done
  # 4) VS Code Java extension bundled JDK (handy on dev machines without a system JDK)
  for d in "$HOME"/.vscode/extensions/redhat.java-*/jre/*/bin \
           "$HOME"/.p2/pool/plugins/org.eclipse.justj.openjdk.*/jre/bin; do
    [[ -x "$d/javac" ]] && { echo "$d"; return 0; }
  done
  return 1
}

JDK_BIN="$(find_jdk || true)"
if [[ -z "${JDK_BIN:-}" ]]; then
  echo "ERROR: No JDK with 'javac' found. Install JDK 17+ or set JAVA_HOME." >&2
  exit 1
fi

JAVAC="$JDK_BIN/javac"
JAVA="$JDK_BIN/java"

# Build the runtime classpath. Gson is optional: if absent, JSON support is unavailable but
# CSV still works.
CP="$OUT"
[[ -f "$GSON" ]] && CP="$CP:$GSON"
