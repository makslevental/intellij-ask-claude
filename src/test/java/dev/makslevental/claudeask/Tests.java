package dev.makslevental.claudeask;

/** Plain tests with no framework: run with scripts/test.sh. Exits non-zero on the first failure count. */
public final class Tests {
    private static int failures = 0;

    private static void eq(String name, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("(pass) " + name);
        } else {
            failures++;
            System.out.println("(fail) " + name + "\n    expected: " + expected + "\n    actual:   " + actual);
        }
    }

    private static void throwsIllegal(String name, Runnable r) {
        try {
            r.run();
            failures++;
            System.out.println("(fail) " + name + ": no exception");
        } catch (java.util.regex.PatternSyntaxException e) {
            System.out.println("(pass) " + name);
        }
    }

    private static void throwsIae(String name, Runnable r) {
        try {
            r.run();
            failures++;
            System.out.println("(fail) " + name + ": no exception");
        } catch (IllegalArgumentException e) {
            System.out.println("(pass) " + name);
        }
    }

    public static void main(String[] args) {
        eq("range pointer", "@src/Foo.kt#L10-20", SelectionPointer.build("src/Foo.kt", 10, 20));
        eq("single line pointer", "@src/Foo.kt#L12", SelectionPointer.build("src/Foo.kt", 12, 12));
        eq("last before first collapses to one line", "@a.c#L5", SelectionPointer.build("a.c", 5, 3));
        eq("backslashes become slashes", "@src/Foo.kt#L1", SelectionPointer.build("src\\Foo.kt", 1, 1));
        throwsIae("empty path rejected", () -> SelectionPointer.build(" ", 1, 1));
        throwsIae("line 0 rejected", () -> SelectionPointer.build("a", 0, 1));

        eq("a span within one line", "(selected 11:3 to 11:6)", SelectionPointer.span(11, 3, 11, 6));
        eq("a span over several lines", "(selected 10:5 to 14:12)", SelectionPointer.span(10, 5, 14, 12));
        throwsIae("span: column 0 rejected", () -> SelectionPointer.span(1, 0, 1, 2));
        eq("the span follows a valid line mention and stays outside it", "@src/Foo.kt#L11 (selected 11:3 to 11:6) why?",
                PromptEncoder.message(SelectionPointer.build("src/Foo.kt", 11, 11) + " " + SelectionPointer.span(11, 3, 11, 6), "why?"));

        eq("a whole-line selection is not partial", false, SelectionPointer.isPartial("", ""));
        eq("indentation before the selection is not partial", false, SelectionPointer.isPartial("    ", ""));
        eq("trailing whitespace after the selection is not partial", false, SelectionPointer.isPartial("", "  \t"));
        eq("code before the selection is partial", true, SelectionPointer.isPartial("int x = ", ""));
        eq("code after the selection is partial", true, SelectionPointer.isPartial("", ";"));
        eq("code on both sides is partial", true, SelectionPointer.isPartial("foo(", ")"));

        eq("caret only: that line", 4, SelectionPointer.lastLine(3, 3, 7, false));
        eq("mid-line selection ends on its line", 6, SelectionPointer.lastLine(3, 5, 9, true));
        eq("whole-line selection ends before the next line", 5, SelectionPointer.lastLine(3, 5, 0, true));
        eq("selection inside one line", 4, SelectionPointer.lastLine(3, 3, 8, true));

        eq("path inside the project is relative", "src/Foo.kt", SelectionPointer.displayPath("/p/x/src/Foo.kt", "/p/x"));
        eq("path outside the project stays absolute", "/other/Foo.kt", SelectionPointer.displayPath("/other/Foo.kt", "/p/x"));
        eq("a sibling directory is not inside the project", "/p/xy/Foo.kt", SelectionPointer.displayPath("/p/xy/Foo.kt", "/p/x"));
        eq("no project base: absolute", "/a/b.c", SelectionPointer.displayPath("/a/b.c", null));

        eq("message joins pointer and question", "@a.c#L1-2 why?", PromptEncoder.message("@a.c#L1-2", "  why?  "));
        eq("escape characters are removed", "hi[201~x", PromptEncoder.sanitize("hi\u001b[201~x"));
        eq("newlines and tabs are kept", "a\n\tb", PromptEncoder.sanitize("a\n\tb"));
        eq("CRLF becomes LF", "a\nb", PromptEncoder.sanitize("a\r\nb"));
        eq("lone CR becomes LF", "a\nb", PromptEncoder.sanitize("a\rb"));
        eq("DEL is removed", "ab", PromptEncoder.sanitize("a\u007fb"));
        eq("bracketed paste wraps the text", "\u001b[200~hello\u001b[201~", PromptEncoder.paste("hello", true));
        eq("plain paste leaves the text", "hello", PromptEncoder.paste("hello", false));


        eq("claude binary", true, ClaudeProcessMatcher.isClaude("/usr/local/bin/claude", new String[0]));
        eq("versioned binary", true,
                ClaudeProcessMatcher.isClaude("/opt/example/claude-code/1.2.3/claude-code-1.2.3.darwin-arm64", null));
        eq("vendor-prefixed versioned binary", true,
                ClaudeProcessMatcher.isClaude("/opt/example/acme-claude-code-1.2.3.darwin-arm64", null));
        eq("node running a script called claude", true,
                ClaudeProcessMatcher.isClaude("/usr/bin/node", new String[] {"/x/bin/claude"}));
        eq("a shell is not claude", false, ClaudeProcessMatcher.isClaude("/bin/zsh", new String[] {"-l"}));
        eq("an editor opening a claude file is not claude", false,
                ClaudeProcessMatcher.isClaude("/usr/bin/vim", new String[] {"notes.txt", "claude.md"}));
        eq("unknown command and args", false, ClaudeProcessMatcher.isClaude(null, null));
        eq("a directory named claude-notes is not claude", false,
                ClaudeProcessMatcher.isClaude("/bin/ls", new String[] {"/home/me/claude-notes/"}));


        eq("default: the last used tab if still present", 2, TargetChoice.defaultIndex(java.util.List.of(10L, 11L, 12L), java.util.List.of(true, false, false), 12L));
        eq("default: the selected tab when the last one is gone", 1, TargetChoice.defaultIndex(java.util.List.of(10L, 11L), java.util.List.of(false, true), 99L));
        eq("default: the selected tab when nothing was used yet", 1, TargetChoice.defaultIndex(java.util.List.of(10L, 11L), java.util.List.of(false, true), null));
        eq("default: the first when nothing is selected", 0, TargetChoice.defaultIndex(java.util.List.of(10L, 11L), java.util.List.of(false, false), null));
        eq("label with title and directory", "\u2733 Claude Code \u2014 ~/dev/proj", TargetChoice.label("\u2733 Claude Code", "/Users/me/dev/proj", "/Users/me", true));
        eq("label outside home keeps the path", "t \u2014 /opt/x", TargetChoice.label("t", "/opt/x", "/Users/me", true));
        eq("label for home itself", "t \u2014 ~", TargetChoice.label("t", "/Users/me", "/Users/me", true));
        eq("a sibling of home is not shortened", "t \u2014 /Users/meagain/x", TargetChoice.label("t", "/Users/meagain/x", "/Users/me", true));
        eq("label without directory", "t", TargetChoice.label("t", null, "/Users/me", true));
        eq("label for a guessed tab", "t (guessed from the title)", TargetChoice.label("t", "", "/Users/me", false));
        eq("blank title falls back", "Terminal", TargetChoice.label(" ", null, null, true));


        eq("a multi-line question keeps its line breaks", "@a.c#L1 first\nsecond\n  third", PromptEncoder.message("@a.c#L1", "first\nsecond\n  third\n"));
        eq("a multi-line paste keeps its line breaks", "\u001b[200~a\nb\u001b[201~", PromptEncoder.paste("a\nb", true));


        eq("selected code is appended as a fenced block",
                "@a.c#L1-2 why?\n\nSelected code:\n```c\nint x;\n  y();\n```",
                PromptEncoder.message("@a.c#L1-2", "why?", "int x;\n  y();\n", "c"));
        eq("a blank selection adds nothing", "@a.c#L1 why?", PromptEncoder.message("@a.c#L1", "why?", "  \n ", "c"));
        eq("a null selection adds nothing", "@a.c#L1 why?", PromptEncoder.message("@a.c#L1", "why?", null, "c"));
        eq("no language: a bare fence", "@a.c#L1 q\n\nSelected code:\n```\nx\n```", PromptEncoder.message("@a.c#L1", "q", "x", null));
        eq("unsafe characters leave the language tag", "c++x", PromptEncoder.safeLanguage("c++`\n x"));
        eq("fence grows past backticks in the code", "````", PromptEncoder.fenceFor("a ``` b"));
        eq("fence is three backticks at least", "```", PromptEncoder.fenceFor("plain"));
        eq("a single backtick needs no longer fence", "```", PromptEncoder.fenceFor("a `x` b"));
        eq("selection with its own fence gets a longer one",
                "@a.md#L1 q\n\nSelected code:\n````md\n```c\nx\n```\n````",
                PromptEncoder.message("@a.md#L1", "q", "```c\nx\n```", "md"));
        eq("CRLF in the selection becomes LF",
                "@a.c#L1 q\n\nSelected code:\n```\na\nb\n```",
                PromptEncoder.message("@a.c#L1", "q", "a\r\nb\r\n", ""));
        String longCode = "x".repeat(PromptEncoder.MAX_SELECTED_CHARS + 5);
        String cut = PromptEncoder.message("@a.c#L1", "q", longCode, "c");
        eq("an overlong selection is cut with a note", true, cut.endsWith("\n```\n(\u2026 5 more characters not shown)"));
        eq("the cut selection keeps the maximum", true, cut.contains("x".repeat(PromptEncoder.MAX_SELECTED_CHARS) + "\n```"));


        java.util.regex.Pattern custom = ClaudeProcessMatcher.compile("my-wrapper|cc-cli");
        eq("a custom pattern matches the executable name", true, ClaudeProcessMatcher.isClaude("/opt/bin/my-wrapper", null, custom));
        eq("a custom pattern is not applied to arguments", false,
                ClaudeProcessMatcher.isClaude("/usr/bin/vim", new String[] {"/x/cc-cli/index.js"}, custom));
        eq("a custom pattern replaces the default substring", false, ClaudeProcessMatcher.isClaude("/opt/claude-code-1.0", null, custom));
        eq("an executable named claude always matches", true, ClaudeProcessMatcher.isClaude("/usr/local/bin/claude", null, custom));
        eq("patterns are case-insensitive", true, ClaudeProcessMatcher.isClaude("/opt/CLAUDE-CODE-1", null));
        eq("a blank pattern falls back to the default", ClaudeProcessMatcher.DEFAULT_PATTERN, ClaudeProcessMatcher.compile("  ").pattern());
        eq("an invalid pattern falls back to the default", ClaudeProcessMatcher.DEFAULT_PATTERN, ClaudeProcessMatcher.compile("(").pattern());
        throwsIllegal("the strict compiler rejects an invalid pattern", () -> ClaudeProcessMatcher.compileStrict("("));


        eq("the default pattern is claude", "claude", ClaudeProcessMatcher.DEFAULT_PATTERN);
        eq("an executable with claude in its name matches", true, ClaudeProcessMatcher.isClaude("/opt/foo/claude-wrapper", null));
        eq("claude as a directory name is not enough", false, ClaudeProcessMatcher.isClaude("/Users/me/claude-project/bin/tsc", null));
        eq("running a project called claude-x is not claude", false,
                ClaudeProcessMatcher.isClaude("/usr/bin/node", new String[] {"/Users/me/claude-project/build.js"}));

        System.out.println(failures == 0 ? "\nall passed" : "\n" + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
