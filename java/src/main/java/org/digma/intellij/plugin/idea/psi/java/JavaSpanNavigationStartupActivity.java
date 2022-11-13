package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

public class JavaSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        JavaSpanNavigationProvider javaSpanNavigationProvider = project.getService(JavaSpanNavigationProvider.class);

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {

                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(event.getDocument());
                if (virtualFile instanceof LightVirtualFile){
                    return;
                }

                javaSpanNavigationProvider.documentChanged(event.getDocument());
            }
        },javaSpanNavigationProvider);


        javaSpanNavigationProvider.build();

    }
}
