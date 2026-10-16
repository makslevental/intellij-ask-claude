#!/bin/sh
# Builds the plugin without Gradle, against the jars of an IDE installed on this machine (no network needed).
# Output: build/ask-claude.zip (install via Settings > Plugins > gear > Install Plugin from Disk).
# The IDE is found automatically (the newest one of build 262+); set IDE_HOME to choose another.
set -e
cd "$(dirname "$0")/.."
. scripts/lib.sh

IDE=$(find_ide)
JAVAC=$(find_javac "$IDE")

CP=""
for jar in "$IDE"/lib/*.jar "$IDE"/plugins/terminal/lib/*.jar "$IDE"/plugins/terminal/lib/modules/*.jar; do
  [ -f "$jar" ] && CP="$CP$jar:"
done

rm -rf build/classes build/plugin build/ask-claude.zip
mkdir -p build/classes build/plugin/ask-claude/lib

"$JAVAC" --release 21 -Xlint:all,-processing -d build/classes -cp "$CP" $(find src/main/java -name '*.java')
cp -R src/main/resources/. build/classes/
(cd build/classes && zip -q -r ../plugin/ask-claude/lib/ask-claude.jar .)
(cd build/plugin && zip -q -r ../ask-claude.zip ask-claude)
echo "built build/ask-claude.zip"
