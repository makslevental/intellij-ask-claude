#!/bin/sh
# Compiles and runs the plain-Java tests for the pure classes. Needs only a JDK (CLion's bundled one works).
set -e
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$HOME/Applications/CLion.app/Contents/jbr/Contents/Home}"
OUT=build/test-classes
rm -rf "$OUT"; mkdir -p "$OUT"
"$JAVA_HOME/bin/javac" --release 21 -d "$OUT" \
  src/main/java/dev/makslevental/claudeask/SelectionPointer.java \
  src/main/java/dev/makslevental/claudeask/PromptEncoder.java \
  src/main/java/dev/makslevental/claudeask/ClaudeProcessMatcher.java \
  src/main/java/dev/makslevental/claudeask/TargetChoice.java \
  src/test/java/dev/makslevental/claudeask/Tests.java
"$JAVA_HOME/bin/java" -cp "$OUT" dev.makslevental.claudeask.Tests
