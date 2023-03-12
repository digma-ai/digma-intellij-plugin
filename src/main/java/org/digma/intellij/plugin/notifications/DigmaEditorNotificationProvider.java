package org.digma.intellij.plugin.notifications;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.digma.intellij.plugin.service.EditorService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Function;

public class DigmaEditorNotificationProvider implements EditorNotificationProvider {

    @Override
    public @NotNull Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {

        return (Function<FileEditor, JComponent>) fileEditor -> {

            if (!fileEditor.getFile().isWritable() &&
                    fileEditor.getFile().getPath().contains(EditorService.STACKTRACE_PREFIX) ){
                var panel = new EditorNotificationPanel();
                panel.icon(AllIcons.General.Warning);
                panel.setText("Digma: This is a read only stack trace document");
                return panel;
            }else if(!fileEditor.getFile().isWritable() &&
                    fileEditor.getFile().getPath().contains(EditorService.VCS_PREFIX)){
                var panel = new EditorNotificationPanel();
                panel.icon(AllIcons.General.Warning);
                panel.setText("Digma: This is a read only vcs document");
                return panel;

            }

            return null;
        };
    }
}
