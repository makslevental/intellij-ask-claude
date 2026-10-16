# intellij-ask-claude

An IntelliJ-platform plugin (built and compiled against CLion 2026.2). Select code, press a shortcut, type a
question in a small popup, and it is sent to the Claude Code session running in the IDE's **Terminal** tool
window (started for you if none is running), together with a file and line-range reference. The message is
submitted; the editor keeps focus.

<img width="1024" height="929" alt="image" src="https://github.com/user-attachments/assets/0732d1eb-add5-4f9a-b0b0-4d35ade0237a" />



## Requirements

* CLion (or another IDE) of build 262 or later, with its bundled **Terminal** plugin enabled.
* The Claude Code CLI (`claude`). It can already be running in the IDE's Terminal tool window, or the plugin starts it.

## Use

1. Optionally start `claude` in the IDE's Terminal tool window (any tab; the plugin finds it). If you don't, the first
   question starts it.
2. Select code in an editor (or just put the caret on a line) and press **Ctrl+Alt+Shift+K**, or right-click and
   choose **Ask Claude...**.
3. Optionally tick **Send text** (off by default; your choice is remembered), then type the question. The
   reference names the lines, and Claude Code reads only those lines from disk. For a selection of part of a line the
   exact columns are added after it, like `@src/Foo.kt#L11 (selected 11:3 to 11:6)` (1-based; the end is just after
   the last selected character). So the text is only needed for unsaved edits. **Enter** sends, **Shift+Enter** or
   **Option+Enter** (Alt+Enter outside macOS) adds a new line, **Esc** cancels. A multi-line question is sent as one
   message.

The popup takes keyboard focus while you type, and gives it back to the editor when it closes. The Terminal tool
window never takes focus.

### No Claude session yet

When no tab runs Claude Code, the popup says so ("sending starts one in a new Terminal tab"). Sending opens a new
Terminal tab named **Claude Code** in the project directory, running `claude` with your message as its first prompt.
The Terminal tool window is shown on that tab, but the editor keeps focus. The tab runs `claude` itself, not a shell,
so it closes when Claude exits. Later questions go to this session.

`claude` is looked for on the IDE's `PATH`, then in `~/.local/bin`, `~/.claude/local`, `/opt/homebrew/bin` and
`/usr/local/bin` (`claude.exe` / `claude.cmd` on Windows). If it is somewhere else, set it in the settings.

### Several Claude sessions

The plugin never guesses between sessions. It finds **every** tab of the Terminal tool window that runs Claude
Code (by checking each tab's shell and the processes below it), and:

* **none found**: a new session is started (see above);
* **one found**: it is used, with no extra UI;
* **several found**: the popup shows a **Send to** list with each tab's title and working directory
  (`✳ Claude Code — ~/dev/project`). It starts on the session you used last, otherwise the tab selected in the
  Terminal tool window. Change it with the mouse or **Alt+Up / Alt+Down**.

A tab whose process could not be read, and which was picked only because its title says "Claude", is labelled
"(guessed from the title)". Only the IDE's own Terminal tool window is searched, so another project's window or an
external terminal is never a target.

## Build and install

```sh
scripts/test.sh          # plain-Java tests for the pure classes (needs a JDK 21+)
scripts/build-local.sh   # compiles against an installed IDE's jars (no network) -> build/ask-claude.zip
```

The scripts find an installed IDE of build 262 or newer that has the Terminal plugin (macOS apps, Linux `/opt`,
and JetBrains Toolbox installs), use the newest one, and say which. To pick another, set `IDE_HOME`, e.g.
`IDE_HOME=/Applications/CLion.app scripts/build-local.sh`. They need a JDK 21+; the IDE's bundled runtime is used
when it includes a compiler, otherwise `JAVA_HOME` or `javac` on the `PATH`. They are shell scripts, so on Windows
use Gradle instead.

Then in the IDE: Settings → Plugins → ⚙ → **Install Plugin from Disk...** → `build/ask-claude.zip`, and restart.

`build.gradle.kts` is the standard IntelliJ Platform Gradle setup, but it was written without being able to run
Gradle, so treat it as a starting point. It builds against `ideHome` (a property) or `IDE_HOME` (an environment
variable) if set, and otherwise downloads the IDE named in `gradle.properties`.

## If it does not work

The plugin logs what it inspects. In the IDE: Help → Show Log in Finder (`idea.log`), then search for
`Ask Claude:`. It reports each Terminal tab, whether it has a process, and whether Claude was found in it.

* **A new session is started although Claude is already running**: Claude must run in the IDE's own Terminal, not
  an external terminal. If it is running there, check the log: each tab is listed with its process id and whether
  Claude was found below it.
* **"Could not find the Claude Code executable"**: set **Claude Code executable** in the settings.
* **Text appears but is not submitted**, or appears garbled: Claude Code may not be in bracketed-paste mode. For
  the classic terminal, `PromptEncoder.paste(message, false)` in `ClassicTerminals` sends plain typing instead.

## Settings

**Settings | Tools | Ask Claude** has two options.

**Claude Code executable**: the program started when no session is running. Empty (the default) means find
`claude` automatically, as described above.

**Claude Code process name**: a regular expression
(case-insensitive, default `claude`). A terminal tab counts as running Claude Code when one of its processes has an
*executable file name* that contains a match. So `claude` matches `claude` and `claude-code-1.2.3.darwin-arm64`.

* It is matched against the executable's **file name only**, not the whole command line or the arguments. That keeps
  `vim claude.md`, or a tool run from a folder called `claude-something`, from being mistaken for Claude.
* Always on, whatever you set: an executable named exactly `claude` / `claude.exe`, and a script install
  (`node /usr/local/bin/claude`, where one of the first two arguments is a file named `claude`).
* Change it if you start Claude through a wrapper or under another name, for example `my-claude-wrapper`.
  An invalid expression is refused when you apply the settings.

## Limitations

* Terminal tool window only; an external terminal cannot be reached.
* Targets build 262 (CLion 2026.2) and declares a dependency on the terminal frontend module, which older IDEs
  do not have. No internal API is used, but the reworked terminal's API (tabs, views, send-text) is
  still marked experimental, so it may change between releases; the classic terminal's API is stable.
* A path containing spaces is sent as-is; Claude Code's handling of that has not been checked.
* If Claude is busy, the message is queued like anything you type.
