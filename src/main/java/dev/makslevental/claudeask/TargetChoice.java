package dev.makslevental.claudeask;

import java.util.List;

/** Pure helpers for choosing between several Claude Code terminals. */
public final class TargetChoice {
    private TargetChoice() {}

    /**
     * Which entry the dropdown starts on: the one used last time if it is still there, otherwise the tab that is
     * selected in the Terminal tool window, otherwise the first.
     *
     * @param ids      a stable id for each candidate
     * @param selected whether each candidate is the tab selected in the tool window
     * @param lastUsed the id sent to last time, or null
     */
    public static int defaultIndex(List<Long> ids, List<Boolean> selected, Long lastUsed) {
        if (lastUsed != null) {
            int i = ids.indexOf(lastUsed);
            if (i >= 0) return i;
        }
        int s = selected.indexOf(Boolean.TRUE);
        return s >= 0 ? s : 0;
    }

    /** The dropdown text: "title — directory", with the home directory shortened to ~. */
    public static String label(String title, String directory, String home, boolean verified) {
        StringBuilder out = new StringBuilder(title == null || title.isBlank() ? "Terminal" : title);
        if (directory != null && !directory.isBlank()) {
            String d = directory;
            if (home != null && !home.isBlank() && (d.equals(home) || d.startsWith(home + "/"))) {
                d = "~" + d.substring(home.length());
            }
            out.append(" — ").append(d);
        }
        if (!verified) out.append(" (guessed from the title)");
        return out.toString();
    }
}
