package dev.makslevental.claudeask;

import org.jetbrains.annotations.Nullable;

/**
 * A terminal tab in which Claude Code runs.
 *
 * @param isVerified false when the tab was only guessed to run Claude from its title, because no process could be read
 */
public record TerminalTarget(TerminalTab tab, boolean isVerified) {

    public long id() {
        return tab.id();
    }

    public String title() {
        return tab.title();
    }

    @Nullable
    public String directory() {
        return tab.directory();
    }

    public boolean isSelected() {
        return tab.isSelected();
    }

    /** See {@link TerminalTab#send}. */
    public boolean send(String message) throws Exception {
        return tab.send(message);
    }

    public String label() {
        return TargetChoice.label(title(), directory(), System.getProperty("user.home"), isVerified);
    }
}
