package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** "Ask Claude...": opens the prompt box for the current selection (or caret line) and sends the result. */
public final class AskClaudeAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(AskClaudeAction.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = e.getProject() != null
                && e.getData(CommonDataKeys.EDITOR) != null
                && e.getData(CommonDataKeys.VIRTUAL_FILE) != null;
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || editor == null || file == null) return;

        List<TerminalTab> tabs;
        try {
            tabs = ClaudeTerminalLocator.tabs(project);
        } catch (LinkageError missing) {
            // The Terminal plugin's classes are absent or incompatible with this IDE build.
            Notices.show(project, "Ask Claude needs the Terminal plugin (" + missing + ").", NotificationType.ERROR);
            return;
        }

        String pointer = pointerFor(project, editor, file);
        SelectionModel selection = editor.getSelectionModel();
        String selectedText = selection.hasSelection() ? selection.getSelectedText() : null;
        String language = file.getExtension();
        ModalityState modality = ModalityState.stateForComponent(editor.getComponent());

        // Walking the process trees can be slow, so it runs off the UI thread; the popup opens when it is done.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<TerminalTarget> targets;
            try {
                targets = ClaudeTerminalLocator.claudeTargets(tabs);
            } catch (RuntimeException failed) {
                LOG.warn("Ask Claude: could not inspect the terminals", failed);
                Notices.show(project, "Could not inspect the terminals: " + failed.getMessage(), NotificationType.ERROR);
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (editor.isDisposed()) return;
                showPopup(project, editor, pointer, targets, selectedText, language);
            }, modality, project.getDisposed());
        });
    }

    private static void showPopup(Project project, Editor editor, String pointer, List<TerminalTarget> targets,
                                  String selectedText, String language) {
        if (targets.isEmpty()) {
            // Say so before the user has typed anything.
            Notices.show(project, "No Claude Code is running in the IDE's Terminal tool window. Start `claude` there first.",
                    NotificationType.WARNING);
            return;
        }
        List<Long> ids = targets.stream().map(TerminalTarget::id).toList();
        List<Boolean> selected = targets.stream().map(TerminalTarget::isSelected).toList();
        int start = TargetChoice.defaultIndex(ids, selected, LastChoice.get(project));
        boolean hasText = selectedText != null && !selectedText.isBlank();
        AskPopup.show(editor, pointer, targets, start, hasText, submission -> {
            String message = submission.includeSelection()
                    ? PromptEncoder.message(pointer, submission.question(), selectedText, language)
                    : PromptEncoder.message(pointer, submission.question());
            ClaudeTerminalSender.send(project, submission.target(), message);
        });
    }

    private static String pointerFor(Project project, Editor editor, VirtualFile file) {
        SelectionModel selection = editor.getSelectionModel();
        Document document = editor.getDocument();
        boolean hasSelection = selection.hasSelection();
        int start = hasSelection ? selection.getSelectionStart() : editor.getCaretModel().getOffset();
        int end = hasSelection ? selection.getSelectionEnd() : start;
        int startLine0 = document.getLineNumber(start);
        int endLine0 = document.getLineNumber(end);
        int endColumn = end - document.getLineStartOffset(endLine0);
        int lastLine = SelectionPointer.lastLine(startLine0, endLine0, endColumn, hasSelection);
        String path = SelectionPointer.displayPath(file.getPath(), project.getBasePath());
        String pointer = SelectionPointer.build(path, startLine0 + 1, lastLine);
        if (!hasSelection) return pointer;

        // Part of a line selected: the mention can only name whole lines, so add the exact span as plain text.
        int startColumn = start - document.getLineStartOffset(startLine0);
        String before = document.getText(new TextRange(document.getLineStartOffset(startLine0), start));
        // A selection that ends at column 0 of a later line stops at the end of the line before it, like the mention.
        boolean endsAtLineStart = endColumn == 0 && endLine0 > startLine0;
        String after = endsAtLineStart ? "" : document.getText(new TextRange(end, document.getLineEndOffset(endLine0)));
        if (!SelectionPointer.isPartial(before, after)) return pointer;
        int spanEndLine0 = endsAtLineStart ? endLine0 - 1 : endLine0;
        int spanEndColumn = endsAtLineStart
                ? document.getLineEndOffset(spanEndLine0) - document.getLineStartOffset(spanEndLine0)
                : endColumn;
        return pointer + " " + SelectionPointer.span(startLine0 + 1, startColumn + 1, spanEndLine0 + 1, spanEndColumn + 1);
    }
}
