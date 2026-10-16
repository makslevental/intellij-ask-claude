package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.ui.content.Content;
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

    /**
     * Every reworked tab that runs Claude, in tab order. When no tab's process could be read, a single tab titled
     * like "Claude" is returned instead, marked as unverified.
     */
    static List<TerminalTarget> findAll(Project project, @Nullable Content selected) {
        List<TerminalToolWindowTab> tabs = new ArrayList<>(TerminalToolWindowTabsManager.getInstance(project).getTabs());
        List<TerminalTarget> found = new ArrayList<>();
        List<TerminalToolWindowTab> titled = new ArrayList<>();
        boolean anyUnreadable = false;
        for (TerminalToolWindowTab tab : tabs) {
            String title = tab.getContent().getTabName();
            long pid = processId(tab.getView());
            boolean claude = pid > 0 && ClaudeProcesses.isClaudeRunning(pid);
            LOG.info("Ask Claude: reworked tab '" + title + "' pid " + (pid > 0 ? pid : "unknown") + " runs claude: " + claude);
            if (pid <= 0) anyUnreadable = true;
            if (claude) {
                found.add(target(tab, pid, true, tab.getContent() == selected));
            } else if (pid <= 0 && title != null && title.toLowerCase().contains("claude")) {
                titled.add(tab);
            }
        }
        // Process ids are not always readable. Only when none matched, and exactly one unreadable tab is titled like
        // Claude, offer that one, labelled as a guess.
        if (found.isEmpty() && anyUnreadable && titled.size() == 1) {
            LOG.info("Ask Claude: no process matched; offering the one tab titled like Claude");
            TerminalToolWindowTab tab = titled.get(0);
            found.add(target(tab, System.identityHashCode(tab), false, tab.getContent() == selected));
        }
        return found;
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

    private static TerminalTarget target(TerminalToolWindowTab tab, long id, boolean verified, boolean isSelected) {
        TerminalView view = tab.getView();
        String title = tab.getContent().getTabName();
        return new TerminalTarget() {
            @Override
            public long id() {
                return id;
            }

            @Override
            public String title() {
                return title;
            }

            @Override
            public String directory() {
                try {
                    java.nio.file.Path directory = view.getWorkingDirectoryFlow().getValue();
                    return directory == null ? null : directory.toString();
                } catch (RuntimeException e) {
                    return null;
                }
            }

            @Override
            public boolean isVerified() {
                return verified;
            }

            @Override
            public boolean isSelected() {
                return isSelected;
            }

            @Override
            public boolean send(String message) {
                return view.createSendTextBuilder().useBracketedPasteMode().shouldExecute().trySend(message);
            }
        };
    }
}
