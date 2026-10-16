package dev.makslevental.claudeask;

/** A terminal tab in which Claude Code runs, and a way to send it a message. */
public interface TerminalTarget {
    /** The tab's title, for log lines. */
    String title();

    /**
     * Sends the message as one paste and submits it. Must not move focus.
     *
     * @return false if the terminal refused the text
     * @throws Exception if writing failed
     */
    boolean send(String message) throws Exception;
}
