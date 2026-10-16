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

    /** Selections longer than this are cut, with a note; the pointer lets Claude read the rest from the file. */
    public static final int MAX_SELECTED_CHARS = 20_000;

    /** The text of the message: the pointer, a space, then the question. */
    public static String message(String pointer, String question) {
        return sanitize(pointer).strip() + " " + sanitize(question).strip();
    }

    /**
     * The message with the selected code appended as a fenced block:
     * {@code pointer question}, a blank line, {@code Selected code:}, then the block. A blank selection adds nothing.
     *
     * @param language the file extension (kt, cpp, ...), used as the fence's language tag; unsafe characters are dropped
     */
    public static String message(String pointer, String question, String selectedText, String language) {
        String base = message(pointer, question);
        if (selectedText == null || selectedText.isBlank()) return base;

        String code = stripTrailingNewlines(sanitize(selectedText));
        String note = "";
        if (code.length() > MAX_SELECTED_CHARS) {
            int cut = MAX_SELECTED_CHARS;
            if (Character.isHighSurrogate(code.charAt(cut - 1))) cut--; // do not split a surrogate pair
            note = "\n(\u2026 " + (code.length() - cut) + " more characters not shown)";
            code = code.substring(0, cut);
        }
        String fence = fenceFor(code);
        return base + "\n\nSelected code:\n" + fence + safeLanguage(language) + "\n" + code + "\n" + fence + note;
    }

    /** A fence of backticks longer than any run of backticks inside the code, and at least three. */
    static String fenceFor(String code) {
        int longest = 0;
        int run = 0;
        for (int i = 0; i < code.length(); i++) {
            run = code.charAt(i) == '`' ? run + 1 : 0;
            longest = Math.max(longest, run);
        }
        return "`".repeat(Math.max(3, longest + 1));
    }

    static String safeLanguage(String language) {
        if (language == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < language.length(); i++) {
            char c = language.charAt(i);
            if (Character.isLetterOrDigit(c) && c < 128 || c == '+' || c == '#' || c == '-' || c == '_' || c == '.') out.append(c);
        }
        return out.toString();
    }

    private static String stripTrailingNewlines(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '\n') end--;
        return text.substring(0, end);
    }

    /** The message wrapped as a bracketed paste, or plain when {@code bracketed} is false. */
    public static String paste(String message, boolean bracketed) {
        return bracketed ? PASTE_START + message + PASTE_END : message;
    }
}
