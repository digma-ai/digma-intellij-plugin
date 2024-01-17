package org.digma.intellij.plugin.htmleditor;

import com.intellij.openapi.fileEditor.impl.HTMLEditorProvider;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.common.EDT;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is a replacement to jetbrains com.intellij.openapi.fileEditor.impl.HTMLEditorProvider.
 * why? we need to be able to identify when we open a browser in the editor, one example is the histogram.
 * the easiest way to identify if an editor opens our browser is to supply a virtual file that we can identify
 * as ours. But it's impossible to supply our own virtual file for web preview because of jetbrains
 * HTMLEditorProvider.openEditor implementation without providing the whole provider interfaces which is too much
 * for such a need. it was possible until version 2023.1 but has changed in 2023.1.1 to code that is impossible
 * to inherit. so the way we do it is by collecting titles of files that we open and when necessary to check if a
 * file is ours we check against the virtual file name.
 * see org.digma.intellij.plugin.editor.EditorEventsHandler#isFileNotChangingContext(com.intellij.openapi.vfs.VirtualFile)
 */
public class DigmaHTMLEditorProvider {


    private final LinkedHashMap<String,String> ourTitles = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 50; // it will never be more than the allowed open files in intellij which is usually a low number
        }
    };

    private final Project project;

    public DigmaHTMLEditorProvider(Project project) {
        this.project = project;
    }

    public static DigmaHTMLEditorProvider getInstance(Project project){
        return project.getService(DigmaHTMLEditorProvider.class);
    }

    public static void openEditor(@NotNull Project project, @NotNull String title,@NotNull  String htmlContent){
        EDT.ensureEDT(() -> getInstance(project).openEditor(title, htmlContent));
    }

    public static void openEditorWithUrl(@NotNull Project project, @NotNull String title, @NotNull String url) {
        EDT.ensureEDT(() -> getInstance(project).openEditorWithUrl(title, url));
    }

    private void openEditorWithUrl(@NotNull String title, @NotNull String url) {
        ourTitles.put(title, title);
        HTMLEditorProvider.openEditor(project, title, url, "<html>Timeout</html>");
    }


    private void openEditor(@NotNull String title, @NotNull String htmlContent) {
        ourTitles.put(title, title);
        HTMLEditorProvider.openEditor(project, title, htmlContent);
    }


    public boolean isOurTitle(String name){
        return ourTitles.containsKey(name);
    }
}
