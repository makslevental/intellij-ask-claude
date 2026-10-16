package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.jediterm.terminal.TtyConnector;

import java.io.IOException;

/**
 * Writes a message to the terminal where Claude Code runs and presses Enter. It writes to the terminal's
 * process, so no window is activated and the editor keeps focus.
 */
public final class ClaudeTerminalSender {
    /** Pause between the paste and Enter, so Claude Code has taken the whole paste before Enter arrives. */
    private static final long ENTER_DELAY_MS = 150;

    private ClaudeTerminalSender() {}

    /** Returns false (after telling the user) when no terminal running Claude Code is found. */
    public static boolean send(Project project, String message) {
        ClaudeTerminalLocator.Found found = ClaudeTerminalLocator.find(project);
        if (found == null) {
            Notices.show(project, "No Claude Code is running in the IDE's Terminal tool window. Start `claude` there first.",
                    NotificationType.WARNING);
            return false;
        }
        TtyConnector connector = found.connector();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                connector.write(PromptEncoder.paste(message, true));
                Thread.sleep(ENTER_DELAY_MS);
                connector.write(PromptEncoder.ENTER);
            } catch (IOException e) {
                Notices.show(project, "Could not write to the Claude Code terminal: " + e.getMessage(), NotificationType.ERROR);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return true;
    }
}
