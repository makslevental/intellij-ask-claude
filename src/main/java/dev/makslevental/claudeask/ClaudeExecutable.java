package dev.makslevental.claudeask;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the Claude Code executable to start in a new terminal tab. The tab runs it directly, with no shell to look
 * it up, so it needs a full path. Uses only the file system, so it can be tested without the IDE.
 */
public final class ClaudeExecutable {
    private ClaudeExecutable() {}

    /**
     * @param configured   the path set in the settings, or blank to search
     * @param pathEnv      the PATH environment variable, or null
     * @param fallbackDirs directories searched after the PATH (see {@link #fallbackDirs})
     * @param windows      whether to look for {@code claude.exe} / {@code claude.cmd} on a {@code ;}-separated PATH
     * @return the executable's path, or null when there is none. A configured path that is not an executable gives
     *         null rather than a search, so a typo is reported instead of silently replaced.
     */
    public static String find(String configured, String pathEnv, List<String> fallbackDirs, boolean windows) {
        if (configured != null && !configured.isBlank()) {
            Path path = path(configured.strip());
            return path != null && isExecutable(path, windows) ? path.toString() : null;
        }
        List<String> dirs = new ArrayList<>();
        if (pathEnv != null) dirs.addAll(List.of(pathEnv.split(windows ? ";" : ":")));
        dirs.addAll(fallbackDirs);
        List<String> names = windows ? List.of("claude.exe", "claude.cmd") : List.of("claude");
        for (String dir : dirs) {
            if (dir.isBlank()) continue;
            Path base = path(dir.strip());
            if (base == null) continue;
            for (String name : names) {
                Path candidate = base.resolve(name);
                if (isExecutable(candidate, windows)) return candidate.toString();
            }
        }
        return null;
    }

    /** Where the installers put {@code claude}, for when the IDE's PATH does not include it. */
    public static List<String> fallbackDirs(String home, boolean windows) {
        List<String> dirs = new ArrayList<>();
        if (home != null && !home.isBlank()) {
            dirs.add(home + "/.local/bin");
            dirs.add(home + "/.claude/local");
        }
        if (!windows) {
            dirs.add("/opt/homebrew/bin");
            dirs.add("/usr/local/bin");
        }
        return dirs;
    }

    private static boolean isExecutable(Path path, boolean windows) {
        return Files.isRegularFile(path) && (windows || Files.isExecutable(path));
    }

    private static Path path(String text) {
        try {
            return Path.of(text);
        } catch (InvalidPathException invalid) {
            return null;
        }
    }
}
