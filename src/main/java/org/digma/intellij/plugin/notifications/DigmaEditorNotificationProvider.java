package org.digma.intellij.plugin.notifications;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Function;

public class DigmaEditorNotificationProvider implements EditorNotificationProvider {

    @Override
    public @NotNull Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {

        return (Function<FileEditor, JComponent>) fileEditor -> {

            if (!fileEditor.getFile().isWritable() &&
                    fileEditor.getFile().getPath().contains("digma-stacktrace") ){
                var panel = new EditorNotificationPanel();
                panel.setBackground(JBColor.YELLOW);
                panel.setText("Digma: This is a read only stack trace document");
                return panel;
            }else if(!fileEditor.getFile().isWritable() &&
                    fileEditor.getFile() instanceof ContentRevisionVirtualFile){
                var panel = new EditorNotificationPanel();
                panel.setBackground(JBColor.YELLOW);
                panel.setText("Digma: This is a read only vcs document");
                return panel;

            }

            return null;
        };
    }
}
