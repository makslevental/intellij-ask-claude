package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

/** Sends a message to a chosen Claude Code terminal. Writes to the terminal, so the editor keeps focus. */
public final class ClaudeTerminalSender {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalSender.class);

    private ClaudeTerminalSender() {}

    public static void send(Project project, TerminalTarget target, String message) {
        LOG.info("Ask Claude: sending " + message.length() + " characters to tab '" + target.title() + "'");
        LastChoice.set(project, target.id());
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
    }
}
