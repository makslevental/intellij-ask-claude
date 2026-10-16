#!/bin/sh
# Compiles and runs the plain-Java tests for the pure classes. Needs only a JDK 21+ (the IDE's bundled one is
# used if an IDE is installed, otherwise JAVA_HOME or the one on PATH).
set -e
cd "$(dirname "$0")/.."
. scripts/lib.sh

IDE=$( (find_ide 2>/dev/null) || true)
JAVAC=$(find_javac "$IDE")
JAVA="$(dirname "$JAVAC")/java"

OUT=build/test-classes
rm -rf "$OUT"; mkdir -p "$OUT"
"$JAVAC" --release 21 -d "$OUT" \
  src/main/java/dev/makslevental/claudeask/SelectionPointer.java \
  src/main/java/dev/makslevental/claudeask/PromptEncoder.java \
  src/main/java/dev/makslevental/claudeask/ClaudeProcessMatcher.java \
  src/main/java/dev/makslevental/claudeask/TargetChoice.java \
  src/main/java/dev/makslevental/claudeask/ClaudeExecutable.java \
  src/test/java/dev/makslevental/claudeask/Tests.java
"$JAVA" -cp "$OUT" dev.makslevental.claudeask.Tests
