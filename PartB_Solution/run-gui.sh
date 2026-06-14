#!/usr/bin/env bash
# Launch the Swing GUI (role chooser → Admin dashboard / Participant view).
# Run from the PartB_Solution directory so "Load Sample" can find data/.
source "$(dirname "$0")/_env.sh"

if [[ ! -d "$OUT" || -z "$(ls -A "$OUT" 2>/dev/null)" ]]; then
  echo "Classes not found — compiling first…"
  "$PROJECT_DIR/compile.sh"
fi

cd "$PROJECT_DIR"
exec "$JAVA" -cp "$CP" com.bits.festival.accommodation.gui.AppLauncher
