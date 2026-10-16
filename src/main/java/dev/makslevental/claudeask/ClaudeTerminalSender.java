package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

/** Sends a message to the terminal where Claude Code runs. Writes to the terminal, so the editor keeps focus. */
public final class ClaudeTerminalSender {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalSender.class);

    private ClaudeTerminalSender() {}

    /** Returns false (after telling the user) when no terminal running Claude Code is found. */
    public static boolean send(Project project, String message) {
        TerminalTarget target = ClaudeTerminalLocator.find(project);
        if (target == null) {
            Notices.show(project, "No Claude Code is running in the IDE's Terminal tool window. Start `claude` there first.",
                    NotificationType.WARNING);
            return false;
        }
        LOG.info("Ask Claude: sending " + message.length() + " characters to tab '" + target.title() + "'");
        // Off the UI thread: the classic terminal sleeps briefly between the paste and Enter.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (!target.send(message)) {
                    Notices.show(project, "The terminal did not accept the text.", NotificationType.WARNING);
                }
            } catch (Exception e) {
                LOG.warn("Ask Claude: send failed", e);
                Notices.show(project, "Could not send to the Claude Code terminal: " + e.getMessage(), NotificationType.ERROR);
            }
        });
        return true;
    }
}
