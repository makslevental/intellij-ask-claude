package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds every tab of the IDE's Terminal tool window in which Claude Code is running. A tab qualifies when its
 * shell, or any process below the shell, is Claude. Both kinds of terminal are searched. There can be several
 * sessions, so this returns all of them and leaves the choice to the user; it never guesses between two.
 * Nothing here activates or focuses a tool window.
 *
 * <p>In two steps: {@link #tabs} reads the tool window on the UI thread, then {@link #claudeTargets} walks the
 * process trees, which can be slow, off it.
 */
public final class ClaudeTerminalLocator {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalLocator.class);

    private ClaudeTerminalLocator() {}

    /** Every tab of the Terminal tool window, in tab order. Call on the UI thread. */
    public static List<TerminalTab> tabs(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (toolWindow == null) {
            LOG.info("Ask Claude: no Terminal tool window");
            return List.of();
        }
        Content selected = toolWindow.getContentManager().getSelectedContent();
        List<TerminalTab> all = new ArrayList<>();

        try {
            all.addAll(ReworkedTerminals.tabs(project, selected));
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: reworked terminal classes unavailable: " + missing);
        }
        try {
            List<Content> contents = List.of(toolWindow.getContentManager().getContents());
            all.addAll(ClassicTerminals.tabs(contents, selected));
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: classic terminal classes unavailable: " + missing);
        }
        return all;
    }

    /**
     * The tabs that run Claude. When none does, and exactly one tab whose process could not be read is titled like
     * "Claude", that one is returned instead, marked as unverified. Walks process trees: call off the UI thread.
     */
    public static List<TerminalTarget> claudeTargets(List<TerminalTab> tabs) {
        List<TerminalTarget> found = new ArrayList<>();
        List<TerminalTab> titled = new ArrayList<>();
        for (TerminalTab tab : tabs) {
            long pid = tab.processId();
            boolean claude = pid > 0 && ClaudeProcesses.isClaudeRunning(pid);
            LOG.info("Ask Claude: tab '" + tab.title() + "' pid " + (pid > 0 ? pid : "unknown") + " runs claude: " + claude);
            if (claude) {
                found.add(new TerminalTarget(tab, true));
            } else if (pid <= 0 && tab.mayGuessFromTitle() && tab.title() != null
                    && tab.title().toLowerCase(Locale.ROOT).contains("claude")) {
                titled.add(tab);
            }
        }
        // Process ids are not always readable. Only when none matched, and exactly one unreadable tab is titled like
        // Claude, offer that one, labelled as a guess.
        if (found.isEmpty() && titled.size() == 1) {
            LOG.info("Ask Claude: no process matched; offering the one tab titled like Claude");
            found.add(new TerminalTarget(titled.get(0), false));
        }
        LOG.info("Ask Claude: " + found.size() + " Claude Code terminal(s) found");
        return found;
    }
}
