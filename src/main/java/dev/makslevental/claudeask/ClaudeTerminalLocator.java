package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds every tab of the IDE's Terminal tool window in which Claude Code is running. A tab qualifies when its
 * shell, or any process below the shell, is Claude. Both kinds of terminal are searched. There can be several
 * sessions, so this returns all of them and leaves the choice to the user; it never guesses between two.
 * Nothing here activates or focuses a tool window.
 */
public final class ClaudeTerminalLocator {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalLocator.class);

    private ClaudeTerminalLocator() {}

    public static List<TerminalTarget> findAll(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (toolWindow == null) {
            LOG.info("Ask Claude: no Terminal tool window");
            return List.of();
        }
        Content selected = toolWindow.getContentManager().getSelectedContent();
        List<TerminalTarget> all = new ArrayList<>();

        try {
            all.addAll(ReworkedTerminals.findAll(project, selected));
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: reworked terminal classes unavailable: " + missing);
        }
        try {
            List<Content> contents = List.of(toolWindow.getContentManager().getContents());
            all.addAll(ClassicTerminals.findAll(project, toolWindow, contents, selected));
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: classic terminal classes unavailable: " + missing);
        }
        LOG.info("Ask Claude: " + all.size() + " Claude Code terminal(s) found");
        return all;
    }
}
