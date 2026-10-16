package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabBuilder;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import kotlin.Unit;
import kotlinx.coroutines.Deferred;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.session.TerminalStartupOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * The reworked terminal (the default in recent IDEs): tabs are {@link TerminalView}s, not widgets. Text goes in
 * through the IDE's own send-text builder, which does the bracketed paste and the Enter.
 */
final class ReworkedTerminals {
    private static final Logger LOG = Logger.getInstance(ReworkedTerminals.class);

    private ReworkedTerminals() {}

    /** Every reworked tab, in tab order. */
    static List<TerminalTab> tabs(Project project, @Nullable Content selected) {
        List<TerminalTab> all = new ArrayList<>();
        for (TerminalToolWindowTab tab : TerminalToolWindowTabsManager.getInstance(project).getTabs()) {
            all.add(tab(tab, processId(tab.getView()), tab.getContent() == selected));
        }
        return all;
    }

    /**
     * Opens a new tab that runs {@code command} itself (no shell) in {@code directory}, and shows the Terminal tool
     * window on it without taking focus. Once the process has started, its tab becomes the last-used session, so
     * later questions go to it.
     */
    static void openTab(Project project, List<String> command, @Nullable String directory, String tabName) {
        TerminalToolWindowTabBuilder builder = TerminalToolWindowTabsManager.getInstance(project).createTabBuilder()
                .shellCommand(command)
                .tabName(tabName)
                .requestFocus(false)
                .deferSessionStartUntilUiShown(false);
        if (directory != null) builder.workingDirectory(directory);
        TerminalToolWindowTab tab = builder.createTab();

        ContentManager contents = tab.getContent().getManager();
        if (contents != null) contents.setSelectedContent(tab.getContent(), false);
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal");
        if (toolWindow != null) toolWindow.show(); // show, unlike activate, leaves focus in the editor

        Deferred<TerminalStartupOptions> options = tab.getView().getStartupOptionsDeferred();
        options.invokeOnCompletion(failure -> {
            if (failure == null) {
                Long pid = options.getCompleted().getPid();
                if (pid != null) LastChoice.set(project, pid);
            }
            return Unit.INSTANCE;
        });
    }

    /** The shell's process id, or -1 while the session has not started or cannot be read. */
    private static long processId(TerminalView view) {
        try {
            Deferred<TerminalStartupOptions> options = view.getStartupOptionsDeferred();
            if (!options.isCompleted()) return -1;
            Long pid = options.getCompleted().getPid(); // null when the shell is not a local process
            return pid == null ? -1 : pid;
        } catch (RuntimeException | LinkageError e) {
            LOG.info("Ask Claude: could not read a terminal's startup options: " + e);
            return -1;
        }
    }

    private static TerminalTab tab(TerminalToolWindowTab tab, long pid, boolean isSelected) {
        TerminalView view = tab.getView();
        String title = tab.getContent().getTabName();
        // Without a process id, the IDE's tab object is the stable key.
        long id = pid > 0 ? pid : System.identityHashCode(tab);
        return new TerminalTab() {
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
            public long processId() {
                return pid;
            }

            @Override
            public boolean mayGuessFromTitle() {
                return true;
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
