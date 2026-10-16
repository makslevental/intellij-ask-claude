package dev.makslevental.claudeask;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** "Ask Claude...": opens the prompt box for the current selection (or caret line) and sends the result. */
public final class AskClaudeAction extends AnAction implements DumbAware {

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

        List<TerminalTarget> targets;
        try {
            targets = ClaudeTerminalLocator.findAll(project);
        } catch (LinkageError missing) {
            // The Terminal plugin's classes are absent or incompatible with this IDE build.
            Notices.show(project, "Ask Claude needs the Terminal plugin (" + missing + ").", NotificationType.ERROR);
            return;
        }
        if (targets.isEmpty()) {
            // Say so before the user has typed anything.
            Notices.show(project, "No Claude Code is running in the IDE's Terminal tool window. Start `claude` there first.",
                    NotificationType.WARNING);
            return;
        }

        String pointer = pointerFor(project, editor, file);
        List<Long> ids = targets.stream().map(TerminalTarget::id).toList();
        List<Boolean> selected = targets.stream().map(TerminalTarget::isSelected).toList();
        int start = TargetChoice.defaultIndex(ids, selected, LastChoice.get(project));
        SelectionModel selection = editor.getSelectionModel();
        String selectedText = selection.hasSelection() ? selection.getSelectedText() : null;
        boolean hasText = selectedText != null && !selectedText.isBlank();
        String language = file.getExtension();
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
        String path = SelectionPointer.displayPath(file.getPath(), project.getBasePath());
        if (hasSelection) {
            int startColumn = start - document.getLineStartOffset(startLine0);
            return SelectionPointer.buildWithColumns(path, startLine0 + 1, startColumn + 1, endLine0 + 1, endColumn + 1);
        }
        return SelectionPointer.build(path, startLine0 + 1, startLine0 + 1);
    }
}
