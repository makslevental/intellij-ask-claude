package dev.makslevental.claudeask;

import com.intellij.openapi.project.Project;

import java.util.Map;
import java.util.WeakHashMap;

/** Remembers, per project and until the IDE closes, which Claude terminal was sent to last. */
final class LastChoice {
    private static final Map<Project, Long> LAST = new WeakHashMap<>();

    private static boolean includeText = false;

    private LastChoice() {}

    static synchronized Long get(Project project) {
        return LAST.get(project);
    }

    static synchronized void set(Project project, long id) {
        LAST.put(project, id);
    }

    /** Whether the "Send text" box was ticked last time (it starts unticked). */
    static synchronized boolean includeText() {
        return includeText;
    }

    static synchronized void setIncludeText(boolean value) {
        includeText = value;
    }
}
