#!/bin/sh
# Builds the plugin without Gradle, against the jars of a locally installed CLion (no network needed).
# Output: build/ask-claude.zip (install via Settings > Plugins > gear > Install Plugin from Disk).
# Override the IDE location with IDE_HOME=/path/to/CLion.app/Contents.
set -e
cd "$(dirname "$0")/.."
IDE_HOME="${IDE_HOME:-$HOME/Applications/CLion.app/Contents}"
JAVAC="$IDE_HOME/jbr/Contents/Home/bin/javac"

CP=$(ls "$IDE_HOME"/lib/*.jar "$IDE_HOME"/plugins/terminal/lib/*.jar | tr '\n' ':')
rm -rf build/classes build/plugin build/ask-claude.zip
mkdir -p build/classes build/plugin/ask-claude/lib

"$JAVAC" --release 21 -Xlint:all,-processing -d build/classes -cp "$CP" $(find src/main/java -name '*.java')
cp -R src/main/resources/. build/classes/
(cd build/classes && zip -q -r ../plugin/ask-claude/lib/ask-claude.jar .)
(cd build/plugin && zip -q -r ../ask-claude.zip ask-claude)
echo "built build/ask-claude.zip"
