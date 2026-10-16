# claude-intellij-ask

An IntelliJ-platform plugin (built and compiled against CLion 2026.2). Select code, press a shortcut, type a
question in a small popup, and it is sent to the Claude Code session running in the IDE's **Terminal** tool
window, together with a file and line-range reference. The message is submitted; the editor keeps focus.

```text
select lines 10-20 of src/Foo.kt, press ⌃⌥⇧K, type "why does this loop twice?", Enter
  →  Claude Code receives:  @src/Foo.kt#L10-20 why does this loop twice?   (and starts answering)
```

## Status

Compiles against CLion 2026.2.3 and the pure logic has tests. In a first run in CLion, the plugin loaded and
opened its popup; the reworked-terminal path was added after that run showed the tabs were not classic widgets,
and has not yet been run end to end. See "If it does not work".

## Use

1. Start `claude` in the IDE's Terminal tool window (any tab; the plugin finds it).
2. Select code in an editor (or just put the caret on a line) and press **Ctrl+Alt+Shift+K**, or right-click and
   choose **Ask Claude...**.
3. Type the question. **Enter** sends, **Shift+Enter** or **Option+Enter** (Alt+Enter outside macOS) adds a new line,
   **Esc** cancels. A multi-line question is sent as one message.

The popup takes keyboard focus while you type, and gives it back to the editor when it closes. The Terminal tool
window is never activated.

### Several Claude sessions

The plugin never guesses between sessions. It finds **every** tab of the Terminal tool window that runs Claude
Code (by checking each tab's shell and the processes below it), and:

* **none found**: you are told straight away, before you type anything;
* **one found**: it is used, with no extra UI;
* **several found**: the popup shows a **Send to** list with each tab's title and working directory
  (`✳ Claude Code — ~/dev/project`). It starts on the session you used last, otherwise the tab selected in the
  Terminal tool window. Change it with the mouse or **Alt+Up / Alt+Down**.

A tab whose process could not be read, and which was picked only because its title says "Claude", is labelled
"(guessed from the title)". Only the IDE's own Terminal tool window is searched, so another project's window or an
external terminal is never a target.

## How it works

* `SelectionPointer` builds `@path#L10-20` (relative to the project, absolute outside it; a selection ending at
  column 0 does not include that last line).
* `ClaudeTerminalLocator` walks the Terminal tool window's tabs and lists the tabs whose shell, or any process
  below it, is Claude Code (`ClaudeProcessMatcher`); the user picks when there are several (`TargetChoice`). Both
  kinds of terminal are supported:
  * **reworked terminal** (the default in recent IDEs; `ReworkedTerminals`): the tab's shell process id comes
    from its terminal session, and the text goes in through the IDE's own `TerminalSendTextBuilder` as a
    bracketed paste followed by Enter;
  * **classic terminal** (`ClassicTerminals`): the text is written to the terminal's process as a bracketed
    paste, a 150 ms pause, then Enter.
  If no process can be read for any tab, a single tab titled like "Claude" is offered, labelled as a guess.
* Neither path activates the Terminal tool window, which is what keeps focus in the editor.

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
  an external terminal. If it is running there, check the log: each tab is listed with its process id and whether
  Claude was found below it.
* **Text appears but is not submitted**, or appears garbled: Claude Code may not be in bracketed-paste mode. For
  the classic terminal, `PromptEncoder.paste(message, false)` in `ClassicTerminals` sends plain typing instead.

## Limitations

* Terminal tool window only; an external terminal cannot be reached.
* Targets build 262 (CLion 2026.2) and declares a dependency on the terminal frontend module, which older IDEs
  do not have. The terminal classes it uses are IDE-internal and may change between releases.
* A path containing spaces is sent as-is; Claude Code's handling of that has not been checked.
* If Claude is busy, the message is queued like anything you type.
