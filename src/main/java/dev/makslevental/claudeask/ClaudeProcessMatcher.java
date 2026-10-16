package dev.makslevental.claudeask;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Decides whether a process is the Claude Code CLI. Pure, so it can be tested without the IDE.
 *
 * <p>The CLI is called {@code claude}. A process matches when
 * <ul>
 *   <li>its executable's file name contains the configurable pattern (default {@link #DEFAULT_PATTERN}); this also
 *       covers an executable that is a symlink target with a longer name, such as {@code claude-1.2.3}; or</li>
 *   <li>one of its first two arguments is a file named exactly {@code claude} (or {@code claude.exe}): that is how
 *       a script install looks, {@code node /usr/local/bin/claude}.</li>
 * </ul>
 * The pattern is deliberately not applied to arguments: with a short pattern like {@code claude} that would also
 * match {@code vim claude.md} or anything run from a project folder called {@code claude-something}.
 */
public final class ClaudeProcessMatcher {
    /** The pattern used until the user changes it: a substring, matched case-insensitively. */
    public static final String DEFAULT_PATTERN = "claude";

    private static final Pattern DEFAULT = Pattern.compile(DEFAULT_PATTERN, Pattern.CASE_INSENSITIVE);

    private ClaudeProcessMatcher() {}

    /**
     * Compiles a user-supplied pattern (a regular expression, matched case-insensitively anywhere in the name).
     *
     * @throws PatternSyntaxException if it is not a valid regular expression
     */
    public static Pattern compileStrict(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /** Like {@link #compileStrict} but falls back to the default for a blank or invalid pattern. */
    public static Pattern compile(String regex) {
        if (regex == null || regex.isBlank()) return DEFAULT;
        try {
            return compileStrict(regex.strip());
        } catch (PatternSyntaxException invalid) {
            return DEFAULT;
        }
    }

    /** {@link #isClaude(String, String[], Pattern)} with the default pattern. */
    public static boolean isClaude(String command, String[] args) {
        return isClaude(command, args, DEFAULT);
    }

    /**
     * @param command the executable path, or null if the OS did not report it
     * @param args    the arguments (without the executable), or null
     * @param pattern the configurable part of the rule, applied to the executable's file name
     */
    public static boolean isClaude(String command, String[] args, Pattern pattern) {
        String name = baseName(command);
        if (isClaudeName(name) || pattern.matcher(name).find()) return true;
        if (args == null) return false;
        for (int i = 0; i < Math.min(2, args.length); i++) {
            if (args[i] != null && isClaudeName(baseName(args[i]))) return true;
        }
        return false;
    }

    private static boolean isClaudeName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.equals("claude") || n.equals("claude.exe");
    }

    private static String baseName(String path) {
        if (path == null) return "";
        String p = path.replace('\\', '/');
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }
}
