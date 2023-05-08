package org.digma.intellij.plugin.htmleditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DigmaHTMLEditorProvider {

    private final Project project;

    public DigmaHTMLEditorProvider(Project project) {
        this.project = project;
    }

    public static DigmaHTMLEditorProvider getInstance(Project project){
        return project.getService(DigmaHTMLEditorProvider.class);
    }

    public static void openEditor(@NotNull Project project, @NotNull String title,@NotNull  String htmlContent){
        getInstance(project).openEditor( title, htmlContent);
    }


    public void openEditor(@NotNull String title,@NotNull  String htmlContent){
        var file = new DigmaHtmlEditorFile(title, DigmaHtmlFileType.INSTANCE, htmlContent);
        file.putUserData(HTMLEditorProvider.Companion.getAFFINITY_KEY(), "");
        FileEditorManager.getInstance(project).openFile(file, true);
    }
}
