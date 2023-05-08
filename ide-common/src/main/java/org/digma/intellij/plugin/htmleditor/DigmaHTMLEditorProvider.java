package org.digma.intellij.plugin.htmleditor;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

/**
 * This class is a replacement to jetbrains com.intellij.openapi.fileEditor.impl.HTMLEditorProvider.
 * why? we need to be able to identify when we open a browser in the editor, one example is the histogram.
 * the easiest way to identify if an editor opens our browser is to supply a virtual file that we can identify
 * as ours. so the method openEditor open a digma file, and we can identify it in various places. for example in
 * org.digma.intellij.plugin.editor.EditorEventsHandler#selectionChanged
 */
public class DigmaHTMLEditorProvider {

    private static final Key<String> AFFINITY_KEY = Key.create("html.editor.affinity.key");

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
        file.putUserData(AFFINITY_KEY, "");
        FileEditorManager.getInstance(project).openFile(file, true);
    }
}
