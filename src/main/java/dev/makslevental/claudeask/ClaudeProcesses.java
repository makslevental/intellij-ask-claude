package dev.makslevental.claudeask;

import java.util.Optional;

/** Process-tree check shared by both kinds of terminal. */
final class ClaudeProcesses {
    private ClaudeProcesses() {}

    /** True when the process, or anything below it, is Claude Code. */
    static boolean isClaudeRunning(ProcessHandle shell) {
        return isClaude(shell) || shell.descendants().anyMatch(ClaudeProcesses::isClaude);
    }

    static boolean isClaudeRunning(long pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        return handle.isPresent() && isClaudeRunning(handle.get());
    }

    private static boolean isClaude(ProcessHandle handle) {
        ProcessHandle.Info info = handle.info();
        return ClaudeProcessMatcher.isClaude(info.command().orElse(null), info.arguments().orElse(null));
    }
}
