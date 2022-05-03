package org.digma.intellij.plugin.document;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.jetbrains.annotations.NotNull;

public class EditorManagerFileOpenedListener implements FileEditorManagerListener {

    private final Project project;
    private final DocumentAnalyzer documentAnalyzer;
    private final LanguageService languageService;

    public EditorManagerFileOpenedListener(Project project) {
        this.project = project;
        documentAnalyzer = project.getService(DocumentAnalyzer.class);
        languageService = project.getService(LanguageService.class);
    }

    private static final Logger LOGGER = Logger.getInstance(EditorManagerFileOpenedListener.class);

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        source.runWhenLoaded(source.getSelectedTextEditor(), new Runnable() {
            @Override
            public void run() {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                Log.log(LOGGER::info, "EditorManagerFileOpenedListener.fileOpened: file:{}, psi-file {}", file,psiFile);
                if (languageService.isSupportedFile(project,psiFile)) {
                    documentAnalyzer.fileOpened(psiFile);
                }
            }
        });
    }
}
