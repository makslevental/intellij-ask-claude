package dev.makslevental.claudeask;

import org.jetbrains.annotations.Nullable;

/**
 * A tab of the Terminal tool window, read on the UI thread. Whether Claude runs in it is decided later, off that
 * thread, from {@link #processId()}.
 */
public interface TerminalTab {
    /** A key for this tab that stays the same while the IDE runs (the shell's process id when known). */
    long id();

    /** The tab's title. */
    String title();

    /** The shell's process id, or -1 when it could not be read. */
    long processId();

    /** Whether this is the tab currently selected in the Terminal tool window. */
    boolean isSelected();

    /** Whether the tab may be offered on its title alone when its process could not be read. */
    boolean mayGuessFromTitle();

    /** The terminal's working directory, if known. */
    @Nullable
    String directory();

    /**
     * Sends the message as one paste and submits it. Must not move focus.
     *
     * @return false if the terminal refused the text
     * @throws Exception if writing failed
     */
    boolean send(String message) throws Exception;
}
