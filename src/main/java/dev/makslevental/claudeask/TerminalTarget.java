package dev.makslevental.claudeask;

import org.jetbrains.annotations.Nullable;

/** A terminal tab in which Claude Code runs, and a way to send it a message. */
public interface TerminalTarget {
    /** A key for this tab that stays the same while the IDE runs (the shell's process id when known). */
    long id();

    /** The tab's title. */
    String title();

    /** The terminal's working directory, if known. */
    @Nullable
    String directory();

    /** False when the tab was only guessed to run Claude from its title, because no process could be read. */
    boolean isVerified();

    /** Whether this is the tab currently selected in the Terminal tool window. */
    boolean isSelected();

    /**
     * Sends the message as one paste and submits it. Must not move focus.
     *
     * @return false if the terminal refused the text
     * @throws Exception if writing failed
     */
    boolean send(String message) throws Exception;

    default String label() {
        return TargetChoice.label(title(), directory(), System.getProperty("user.home"), isVerified());
    }
}
