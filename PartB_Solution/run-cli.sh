#!/usr/bin/env bash
# Run the headless CLI allocator.
#   ./run-cli.sh <participants.(csv|json)> <rooms.(csv|json)> [outputDir] [--format csv|json]
# With no arguments, runs the bundled sample data.
source "$(dirname "$0")/_env.sh"

if [[ ! -d "$OUT" || -z "$(ls -A "$OUT" 2>/dev/null)" ]]; then
  echo "Classes not found — compiling first…"
  "$PROJECT_DIR/compile.sh"
fi

if [[ $# -eq 0 ]]; then
  echo "No args given — running bundled sample (data/participants.csv, data/rooms.csv)."
  set -- "$PROJECT_DIR/data/participants.csv" "$PROJECT_DIR/data/rooms.csv" "$PROJECT_DIR/out_data" --format csv
fi

exec "$JAVA" -cp "$CP" com.bits.festival.accommodation.cli.Main "$@"
