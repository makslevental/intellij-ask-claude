package dev.makslevental.claudeask;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/** The plugin's application-wide settings (Settings | Tools | Ask Claude). */
@State(name = "AskClaudeSettings", storages = @Storage("askClaude.xml"))
public final class AskClaudeSettings implements PersistentStateComponent<AskClaudeSettings.State> {

    /** Persisted fields; public and mutable because the XML serializer needs that. */
    public static final class State {
        /** Regular expression for the Claude Code process name. */
        public String processPattern = ClaudeProcessMatcher.DEFAULT_PATTERN;
        /** The Claude Code executable to start when no session is running; blank to find it automatically. */
        public String claudeExecutable = "";
    }

    private State state = new State();
    private String compiledFrom;
    private Pattern compiled;

    public static AskClaudeSettings getInstance() {
        return ApplicationManager.getApplication().getService(AskClaudeSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State loaded) {
        XmlSerializerUtil.copyBean(loaded, state);
    }

    public String processPatternText() {
        return state.processPattern == null || state.processPattern.isBlank()
                ? ClaudeProcessMatcher.DEFAULT_PATTERN : state.processPattern;
    }

    public void setProcessPatternText(String text) {
        state.processPattern = text == null || text.isBlank() ? ClaudeProcessMatcher.DEFAULT_PATTERN : text.strip();
    }

    public String claudeExecutableText() {
        return state.claudeExecutable == null ? "" : state.claudeExecutable.strip();
    }

    public void setClaudeExecutableText(String text) {
        state.claudeExecutable = text == null ? "" : text.strip();
    }

    /** The compiled pattern; an invalid saved value falls back to the default instead of failing. */
    public synchronized Pattern processPattern() {
        String text = processPatternText();
        if (!text.equals(compiledFrom)) {
            compiled = ClaudeProcessMatcher.compile(text);
            compiledFrom = text;
        }
        return compiled;
    }
}
