package dev.makslevental.claudeask;

import com.intellij.openapi.diagnostic.Logger;
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

/** The classic (jediterm) terminal: tabs that have a {@link TerminalWidget}. Written to like a pty. */
final class ClassicTerminals {
    private static final Logger LOG = Logger.getInstance(ClassicTerminals.class);
    /** Pause between the paste and Enter, so Claude Code has taken the whole paste before Enter arrives. */
    private static final long ENTER_DELAY_MS = 150;

    private ClassicTerminals() {}

    /** Every classic tab with a live shell, in tab order. */
    static List<TerminalTab> tabs(List<Content> contents, @Nullable Content selected) {
        List<TerminalTab> found = new ArrayList<>();
        for (Content content : contents) {
            TerminalWidget widget = TerminalToolWindowManager.findWidgetByContent(content);
            if (widget == null) continue; // a tab of the reworked terminal, handled elsewhere
            TtyConnector connector = unwrap(widget);
            if (!(connector instanceof ProcessTtyConnector process)) {
                LOG.info("Ask Claude: classic tab '" + content.getTabName() + "' has no process connector");
                continue;
            }
            Process shell = process.getProcess();
            if (shell == null || !shell.isAlive()) {
                LOG.info("Ask Claude: classic tab '" + content.getTabName() + "' has no live shell");
                continue;
            }
            long id = shell.pid();
            String title = content.getTabName();
            boolean isSelected = content == selected;
            found.add(new TerminalTab() {
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
                    return null; // the classic widget does not expose it reliably
                }

                @Override
                public long processId() {
                    return id;
                }

                @Override
                public boolean mayGuessFromTitle() {
                    return false; // the process is always readable here
                }

                @Override
                public boolean isSelected() {
                    return isSelected;
                }

                @Override
                public boolean send(String message) throws Exception {
                    connector.write(PromptEncoder.paste(message, true));
                    Thread.sleep(ENTER_DELAY_MS);
                    connector.write(PromptEncoder.ENTER);
                    return true;
                }
            });
        }
        return found;
    }

    @Nullable
    private static TtyConnector unwrap(TerminalWidget widget) {
        JBTerminalWidget jediTerm = JBTerminalWidget.asJediTermWidget(widget);
        TtyConnector connector = jediTerm != null ? jediTerm.getProcessTtyConnector() : widget.getTtyConnector();
        if (connector instanceof ProxyTtyConnector proxy) connector = proxy.getConnector();
        return connector;
    }
}
