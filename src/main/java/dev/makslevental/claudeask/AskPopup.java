package dev.makslevental.claudeask;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * The small prompt box shown next to the selection. Enter sends, Shift+Enter inserts a newline, Escape closes.
 * The popup takes keyboard focus while it is open (it has to, to be typed into) and hands it back to the editor
 * when it closes.
 */
public final class AskPopup {
    private AskPopup() {}

    public static void show(Editor editor, String pointer, Consumer<String> onSend) {
        JBTextArea input = new JBTextArea(3, 56);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.getEmptyText().setText("Ask Claude about this code (Enter to send, Shift+Enter for a new line)");

        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(6)));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(new JBLabel(pointer), BorderLayout.NORTH);
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
        input.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = input.getText().strip();
                if (text.isEmpty()) return;
                popup.closeOk(null);
                onSend.accept(text);
            }
        });
        input.getActionMap().put("newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.insert("\n", input.getCaretPosition());
            }
        });

        popup.showInBestPositionFor(editor);
    }
}
