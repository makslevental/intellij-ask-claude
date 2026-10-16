package dev.makslevental.claudeask;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * The small prompt box shown next to the selection. Enter sends, Shift+Enter or Option/Alt+Enter inserts a newline,
 * Escape closes.
 * When several Claude Code terminals are running, a "Send to" list names each one (Alt+Up/Down changes it).
 * The popup takes keyboard focus while it is open (it has to, to be typed into) and hands it back to the editor
 * when it closes.
 */
public final class AskPopup {
    private AskPopup() {}

    /** What the user chose: the question, the terminal to send it to, and whether to attach the selected code. */
    public record Submission(String question, TerminalTarget target, boolean includeSelection) {}

    public static void show(Editor editor, String pointer, List<TerminalTarget> targets, int defaultIndex,
                            boolean hasSelectedText, Consumer<Submission> onSend) {
        JBTextArea input = new JBTextArea(3, 56);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.getEmptyText().setText("Ask Claude about this code (Enter sends; Shift+Enter or Option+Enter adds a line)");

        JPanel top = new JPanel(new GridLayout(0, 1, 0, JBUI.scale(4)));
        top.add(new JBLabel(pointer));
        ComboBox<String> chooser = null;
        if (targets.size() > 1) {
            chooser = new ComboBox<>(targets.stream().map(TerminalTarget::label).toArray(String[]::new));
            chooser.setSelectedIndex(defaultIndex);
            JPanel row = new JPanel(new BorderLayout(JBUI.scale(6), 0));
            row.add(new JBLabel("Send to:"), BorderLayout.WEST);
            row.add(chooser, BorderLayout.CENTER);
            top.add(row);
            top.add(new JBLabel("Alt+Up / Alt+Down changes the session"));
        }
        final ComboBox<String> sessions = chooser;

        JBCheckBox include = new JBCheckBox("Include the selected text", LastChoice.includeText());
        if (hasSelectedText) top.add(include);

        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JBScrollPane(input), BorderLayout.CENTER);

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, input)
                .setTitle("Ask Claude")
                .setRequestFocus(true)
                .setFocusable(true)
                .setMovable(true)
                .setResizable(true)
                .setCancelOnClickOutside(true)
                .setCancelKeyEnabled(true)
                .createPopup();

        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "newline");
        // Option+Enter on a Mac (Alt+Enter elsewhere).
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), "newline");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "previousSession");
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "nextSession");
        input.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = input.getText().strip();
                if (text.isEmpty()) return;
                int index = sessions == null ? 0 : Math.max(0, sessions.getSelectedIndex());
                boolean attach = hasSelectedText && include.isSelected();
                if (hasSelectedText) LastChoice.setIncludeText(include.isSelected());
                popup.closeOk(null);
                onSend.accept(new Submission(text, targets.get(index), attach));
            }
        });
        input.getActionMap().put("newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.insert("\n", input.getCaretPosition());
            }
        });
        input.getActionMap().put("previousSession", moveSelection(sessions, -1));
        input.getActionMap().put("nextSession", moveSelection(sessions, +1));

        popup.showInBestPositionFor(editor);
    }

    private static AbstractAction moveSelection(ComboBox<String> sessions, int delta) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sessions == null) return;
                int n = sessions.getItemCount();
                sessions.setSelectedIndex(Math.floorMod(sessions.getSelectedIndex() + delta, n));
            }
        };
    }
}
