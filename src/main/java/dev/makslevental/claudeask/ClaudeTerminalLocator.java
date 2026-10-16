package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the tab of the IDE's Terminal tool window in which Claude Code is running. A tab qualifies when its
 * shell, or any process below the shell, is Claude. Both kinds of terminal are searched (the reworked one first,
 * which is the default in recent IDEs). Nothing here activates or focuses a tool window.
 */
public final class ClaudeTerminalLocator {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalLocator.class);

    private ClaudeTerminalLocator() {}

    @Nullable
    public static TerminalTarget find(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (toolWindow == null) {
            LOG.info("Ask Claude: no Terminal tool window");
            return null;
        }
        Content selected = toolWindow.getContentManager().getSelectedContent();
        String selectedTitle = selected != null ? selected.getTabName() : null;

        try {
            TerminalTarget target = ReworkedTerminals.find(project, selectedTitle);
            if (target != null) return target;
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: reworked terminal classes unavailable: " + missing);
        }

        List<Content> contents = new ArrayList<>(List.of(toolWindow.getContentManager().getContents()));
        if (selected != null && contents.remove(selected)) contents.add(0, selected);
        try {
            return ClassicTerminals.find(project, toolWindow, contents);
        } catch (LinkageError missing) {
            LOG.info("Ask Claude: classic terminal classes unavailable: " + missing);
            return null;
        }
    }
}
