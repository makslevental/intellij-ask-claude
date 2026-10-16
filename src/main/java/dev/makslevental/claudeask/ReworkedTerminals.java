package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import kotlinx.coroutines.Deferred;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.session.impl.TerminalSession;

import java.util.ArrayList;
import java.util.List;

/**
 * The reworked terminal (the default in recent IDEs): tabs are {@link TerminalView}s, not widgets. Text goes in
 * through the IDE's own send-text builder, which does the bracketed paste and the Enter.
 */
final class ReworkedTerminals {
    private static final Logger LOG = Logger.getInstance(ReworkedTerminals.class);

    private ReworkedTerminals() {}

    /** @param selectedTitle the title of the selected tab, tried first; may be null */
    @Nullable
    static TerminalTarget find(Project project, @Nullable String selectedTitle) {
        List<TerminalToolWindowTab> tabs = new ArrayList<>(TerminalToolWindowTabsManager.getInstance(project).getTabs());
        tabs.sort((a, b) -> Boolean.compare(!isSelected(b, selectedTitle), !isSelected(a, selectedTitle)));

        TerminalToolWindowTab byTitle = null;
        int titled = 0;
        for (TerminalToolWindowTab tab : tabs) {
            String title = tab.getContent().getTabName();
            long pid = processId(tab.getView());
            boolean claude = pid > 0 && ClaudeProcesses.isClaudeRunning(pid);
            LOG.info("Ask Claude: reworked tab '" + title + "' pid " + (pid > 0 ? pid : "unknown") + " runs claude: " + claude);
            if (claude) return target(tab);
            if (title != null && title.toLowerCase().contains("claude")) {
                titled++;
                byTitle = tab;
            }
        }
        // Process ids are not always readable; a single tab titled "Claude ..." is a good enough second guess.
        if (titled == 1) {
            LOG.info("Ask Claude: no process matched; using the one tab titled like Claude");
            return target(byTitle);
        }
        return null;
    }

    private static boolean isSelected(TerminalToolWindowTab tab, @Nullable String selectedTitle) {
        return selectedTitle != null && selectedTitle.equals(tab.getContent().getTabName());
    }

    /** The shell's process id, or -1 while the session has not started or cannot be read. */
    private static long processId(TerminalView view) {
        try {
            Deferred<TerminalSession> session = view.getSessionDeferred();
            if (!session.isCompleted()) return -1;
            return session.getCompleted().getProcessId();
        } catch (RuntimeException | LinkageError e) {
            LOG.info("Ask Claude: could not read a terminal session: " + e);
            return -1;
        }
    }

    private static TerminalTarget target(TerminalToolWindowTab tab) {
        TerminalView view = tab.getView();
        String title = tab.getContent().getTabName();
        return new TerminalTarget() {
            @Override
            public String title() {
                return title;
            }

            @Override
            public boolean send(String message) {
                return view.createSendTextBuilder().useBracketedPasteMode().shouldExecute().trySend(message);
            }
        };
    }
}
