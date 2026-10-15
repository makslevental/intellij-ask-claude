# claude-intellij-ask

An IntelliJ-platform plugin (built and compiled against CLion 2026.2). Select code, press a shortcut, type a
question in a small popup, and it is sent to the Claude Code session running in the IDE's **Terminal** tool
window, together with a file and line-range reference. The message is submitted; the editor keeps focus.

```text
select lines 10-20 of src/Foo.kt, press ⌃⌥⇧K, type "why does this loop twice?", Enter
  →  Claude Code receives:  @src/Foo.kt#L10-20 why does this loop twice?   (and starts answering)
```

## Status

Compiles against CLion 2026.2.3 and the pure logic has tests, but **it has not been run inside an IDE yet**. The
first real run is where the terminal handling is verified. See "If it does not work".

## Use

1. Start `claude` in the IDE's Terminal tool window (any tab; the plugin finds it).
2. Select code in an editor (or just put the caret on a line) and press **Ctrl+Alt+Shift+K**, or right-click and
   choose **Ask Claude...**.
3. Type the question. **Enter** sends, **Shift+Enter** adds a new line, **Esc** cancels.

The popup takes keyboard focus while you type, and gives it back to the editor when it closes. The Terminal tool
window is never activated.

## How it works

* `SelectionPointer` builds `@path#L10-20` (relative to the project, absolute outside it; a selection ending at
  column 0 does not include that last line).
* `ClaudeTerminalLocator` walks the Terminal tool window's tabs and picks the one whose shell, or any process
  below it, is Claude Code (`ClaudeProcessMatcher`).
  The selected tab wins when several tabs run Claude.
* `ClaudeTerminalSender` writes to that terminal's process: the text as a bracketed paste (so `@file` is plain
  text and does not open the file picker), a 150 ms pause, then Enter. Writing to the process is what avoids
  moving focus.

## Build and install

```sh
scripts/test.sh          # plain-Java tests for the pure classes
scripts/build-local.sh   # compiles against your CLion's jars (no network) -> build/ask-claude.zip
```

Then in the IDE: Settings → Plugins → ⚙ → **Install Plugin from Disk...** → `build/ask-claude.zip`, and restart.

`build.gradle.kts` is the standard IntelliJ Platform Gradle setup, but it was written without being able to run
Gradle, so treat it as a starting point. Set `ideHome` in `gradle.properties`.

## If it does not work

The plugin logs what it inspects. In the IDE: Help → Show Log in Finder (`idea.log`), then search for
`Ask Claude:`. It reports each Terminal tab, whether it has a process, and whether Claude was found in it.

* **"No Claude Code is running in the IDE's Terminal tool window"**: Claude must run in the IDE's own Terminal, not
  an external terminal. If it is running there, check the log: the tab may report no process connector.
* **Text appears but is not submitted**, or appears garbled: Claude Code may not be in bracketed-paste mode;
  `PromptEncoder.paste(message, false)` in `ClaudeTerminalSender` sends it as plain typing instead.

## Limitations

* Terminal tool window only; an external terminal cannot be reached.
* Targets build 262 (CLion 2026.2). Older IDEs are untested. The terminal classes it uses are IDE-internal and
  may change between releases.
* A path containing spaces is sent as-is; Claude Code's handling of that has not been checked.
* If Claude is busy, the message is queued like anything you type.
