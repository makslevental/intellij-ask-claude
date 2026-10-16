package dev.makslevental.claudeask;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.regex.PatternSyntaxException;

/** The settings page: how to recognise the Claude Code process. */
public final class AskClaudeConfigurable implements Configurable {
    private JBTextField pattern;

    @Override
    public String getDisplayName() {
        return "Ask Claude";
    }

    @Override
    public JComponent createComponent() {
        pattern = new JBTextField(30);
        pattern.getEmptyText().setText(ClaudeProcessMatcher.DEFAULT_PATTERN);

        JBLabel help = new JBLabel("<html>A terminal tab counts as running Claude Code when it has a process whose executable "
                + "file name contains a match for this regular expression (case-insensitive). So <code>claude</code> matches "
                + "<code>claude</code> and <code>claude-code-1.2.3</code>. Change it if you start Claude through a wrapper "
                + "or under another name. Installs run through <code>node</code> are recognised separately. "
                + "Default: <code>" + ClaudeProcessMatcher.DEFAULT_PATTERN + "</code></html>");
        help.setForeground(UIUtil.getContextHelpForeground());

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Claude Code process name:", pattern)
                .addComponent(help)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return pattern != null && !normalized(pattern.getText()).equals(AskClaudeSettings.getInstance().processPatternText());
    }

    @Override
    public void apply() throws ConfigurationException {
        String text = normalized(pattern.getText());
        try {
            ClaudeProcessMatcher.compileStrict(text);
        } catch (PatternSyntaxException invalid) {
            throw new ConfigurationException("Not a valid regular expression: " + invalid.getDescription());
        }
        AskClaudeSettings.getInstance().setProcessPatternText(text);
    }

    @Override
    public void reset() {
        if (pattern != null) pattern.setText(AskClaudeSettings.getInstance().processPatternText());
    }

    @Override
    public void disposeUIResources() {
        pattern = null;
    }

    private static String normalized(String text) {
        return text == null || text.isBlank() ? ClaudeProcessMatcher.DEFAULT_PATTERN : text.strip();
    }
}
