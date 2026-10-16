package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import java.util.List;

/**
 * Starts Claude Code in a new Terminal tab when none is running, with the message as its first prompt. The tab's
 * process is {@code claude} itself, so the message is a plain argument: no shell, no quoting, no waiting for Claude
 * to be ready to take a paste.
 */
final class ClaudeLauncher {
    private static final Logger LOG = Logger.getInstance(ClaudeLauncher.class);

    private ClaudeLauncher() {}

    /** Call on the UI thread. */
    static void start(Project project, String message) {
        String configured = AskClaudeSettings.getInstance().claudeExecutableText();
        String executable = ClaudeExecutable.find(configured, EnvironmentUtil.getValue("PATH"),
                ClaudeExecutable.fallbackDirs(System.getProperty("user.home"), SystemInfo.isWindows), SystemInfo.isWindows);
        if (executable == null) {
            String text = configured.isEmpty()
                    ? "Could not find the Claude Code executable (claude). Set it in Settings | Tools | Ask Claude."
                    : "The Claude Code executable in Settings | Tools | Ask Claude is not an executable file: " + configured;
            Notices.show(project, text, NotificationType.ERROR);
            return;
        }
        LOG.info("Ask Claude: starting " + executable + " in a new tab with " + message.length() + " characters");
        try {
            ReworkedTerminals.openTab(project, List.of(executable, message), project.getBasePath(), "Claude Code");
        } catch (LinkageError missing) {
            Notices.show(project, "Ask Claude could not open a Terminal tab (" + missing + ").", NotificationType.ERROR);
        } catch (RuntimeException e) {
            LOG.warn("Ask Claude: could not start Claude Code", e);
            Notices.show(project, "Could not start Claude Code: " + e.getMessage(), NotificationType.ERROR);
        }
    }
}
