package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.ui.TerminalWidget;
import com.intellij.ui.content.Content;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.ProxyTtyConnector;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the terminal tab of the IDE's Terminal tool window in which Claude Code is running: a tab qualifies when its shell,
 * or any process below the shell, is Claude.
 * Nothing here activates or focuses a tool window.
 */
public final class ClaudeTerminalLocator {
    private static final Logger LOG = Logger.getInstance(ClaudeTerminalLocator.class);

    private ClaudeTerminalLocator() {}

    /** The connector to write to, and the title of its tab (for messages). */
    public record Found(TtyConnector connector, String title) {}

    @Nullable
    public static Found find(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (toolWindow == null) {
            LOG.info("Ask Claude: no Terminal tool window");
            return null;
        }

        // The selected tab first, so that with several Claude sessions the visible one wins.
        List<Content> contents = new ArrayList<>(List.of(toolWindow.getContentManager().getContents()));
        Content selected = toolWindow.getContentManager().getSelectedContent();
        if (selected != null && contents.remove(selected)) contents.add(0, selected);

        for (Content content : contents) {
            TerminalWidget widget = TerminalToolWindowManager.findWidgetByContent(content);
            if (widget == null) {
                LOG.info("Ask Claude: tab '" + content.getTabName() + "' has no terminal widget");
                continue;
            }
            TtyConnector connector = unwrap(widget);
            if (!(connector instanceof ProcessTtyConnector process)) {
                LOG.info("Ask Claude: tab '" + content.getTabName() + "' has no process connector ("
                        + (connector == null ? "null" : connector.getClass().getName()) + ")");
                continue;
            }
            boolean claude = isClaudeRunning(process.getProcess());
            LOG.info("Ask Claude: tab '" + content.getTabName() + "' runs claude: " + claude);
            if (claude) return new Found(connector, content.getTabName());
        }
        return null;
    }

    @Nullable
    private static TtyConnector unwrap(TerminalWidget widget) {
        JBTerminalWidget jediTerm = JBTerminalWidget.asJediTermWidget(widget);
        TtyConnector connector = jediTerm != null ? jediTerm.getProcessTtyConnector() : widget.getTtyConnector();
        if (connector instanceof ProxyTtyConnector proxy) connector = proxy.getConnector();
        return connector;
    }

    private static boolean isClaudeRunning(@Nullable Process shell) {
        if (shell == null || !shell.isAlive()) return false;
        ProcessHandle handle = shell.toHandle();
        return isClaude(handle) || handle.descendants().anyMatch(ClaudeTerminalLocator::isClaude);
    }

    private static boolean isClaude(ProcessHandle handle) {
        ProcessHandle.Info info = handle.info();
        return ClaudeProcessMatcher.isClaude(info.command().orElse(null), info.arguments().orElse(null));
    }
}
