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
        eq("node running claude-code", true,
                ClaudeProcessMatcher.isClaude("/usr/bin/node", new String[] {"/usr/lib/node_modules/@anthropic-ai/claude-code/cli.js"}));
        eq("node running a script called claude", true,
                ClaudeProcessMatcher.isClaude("/usr/bin/node", new String[] {"/x/bin/claude"}));
        eq("a shell is not claude", false, ClaudeProcessMatcher.isClaude("/bin/zsh", new String[] {"-l"}));
        eq("an editor opening a claude file is not claude", false,
                ClaudeProcessMatcher.isClaude("/usr/bin/vim", new String[] {"notes.txt", "claude.md"}));
        eq("unknown command and args", false, ClaudeProcessMatcher.isClaude(null, null));
        eq("a directory named claude-notes is not claude", false,
                ClaudeProcessMatcher.isClaude("/bin/ls", new String[] {"/home/me/claude-notes/"}));

        System.out.println(failures == 0 ? "\nall passed" : "\n" + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
