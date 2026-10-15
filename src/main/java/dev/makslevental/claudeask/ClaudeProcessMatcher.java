package dev.makslevental.claudeask;

import java.util.Locale;

/**
 * Decides whether a process is the Claude Code CLI. Pure, so it can be tested without the IDE.
 *
 * <p>Claude Code is started in several ways: a binary named {@code claude}, a versioned binary such as
 * {@code claude-code-1.2.3.darwin-arm64} (possibly with a vendor prefix), or {@code node .../claude-code/cli.js}. All of them have the name
 * {@code claude} somewhere in the executable or its first arguments.
 */
public final class ClaudeProcessMatcher {
    private ClaudeProcessMatcher() {}

    /**
     * @param command the executable path, or null if the OS did not report it
     * @param args    the arguments (without the executable), or null
     */
    public static boolean isClaude(String command, String[] args) {
        if (isClaudeName(baseName(command))) return true;
        if (args == null) return false;
        // Interpreters: `node /path/to/claude-code/cli.js`. Only the first two arguments can be the script.
        for (int i = 0; i < Math.min(2, args.length); i++) {
            String arg = args[i];
            if (arg == null) continue;
            if (isClaudeName(baseName(arg))) return true;
            if (arg.toLowerCase(Locale.ROOT).contains("/claude-code/")) return true;
        }
        return false;
    }

    private static boolean isClaudeName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.equals("claude") || n.equals("claude.exe") || n.contains("claude-code");
    }

    private static String baseName(String path) {
        if (path == null) return "";
        String p = path.replace('\\', '/');
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }
}
