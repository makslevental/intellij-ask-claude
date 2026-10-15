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

        String pointer = pointerFor(project, editor, file);
        AskPopup.show(editor, pointer, question -> {
            String message = PromptEncoder.message(pointer, question);
            ClaudeTerminalSender.send(project, message);
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
        return SelectionPointer.build(path, startLine0 + 1, lastLine);
    }
}
