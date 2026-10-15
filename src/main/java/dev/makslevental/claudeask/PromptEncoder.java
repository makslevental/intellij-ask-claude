package dev.makslevental.claudeask;

/**
 * Turns the pointer and the question into the bytes written to Claude Code's terminal. The text is sent as
 * a bracketed paste so that an {@code @file} reference is plain text and does not open the file picker; Enter
 * is sent separately by the caller, after a short pause, so the paste is complete before it arrives.
 */
public final class PromptEncoder {
    public static final String PASTE_START = "\u001b[200~";
    public static final String PASTE_END = "\u001b[201~";
    public static final String ENTER = "\r";

    private PromptEncoder() {}

    /** Removes control characters that could end the paste early or inject keystrokes; keeps newlines and tabs. */
    public static String sanitize(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') continue; // CRLF: keep the \n
                out.append('\n');
            } else if (c == '\n' || c == '\t' || (c >= 0x20 && c != 0x7f)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** The text of the message: the pointer, a space, then the question. */
    public static String message(String pointer, String question) {
        return sanitize(pointer).strip() + " " + sanitize(question).strip();
    }

    /** The message wrapped as a bracketed paste, or plain when {@code bracketed} is false. */
    public static String paste(String message, boolean bracketed) {
        return bracketed ? PASTE_START + message + PASTE_END : message;
    }
}
