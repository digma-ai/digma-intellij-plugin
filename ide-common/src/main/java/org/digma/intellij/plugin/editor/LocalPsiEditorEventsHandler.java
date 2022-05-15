package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

public abstract class LocalPsiEditorEventsHandler implements EditorEventsHandler, Disposable {

    private static final Logger LOGGER = Logger.getInstance(LocalPsiEditorEventsHandler.class);

    private EditorListener editorListener;
    private Project project;
    private LanguageService languageService;
    private CaretContextService caretContextService;


    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
        editorListener.stop();
    }


    public void start(@NotNull Project project, CaretContextService caretContextService, LanguageService languageService) {
        Log.log(LOGGER::debug, "starting..");
        this.project = project;
        this.languageService = languageService;
        this.caretContextService = caretContextService;
        editorListener = new EditorListener(project, this);
        editorListener.start();
    }

    boolean isSupportedFile(VirtualFile newFile) {
        return languageService.isSupportedFile(project, newFile);
    }


    void emptySelection() {
        caretContextService.contextEmpty();
    }

    void updateCurrentElement(int caretOffset, VirtualFile file) {
        MethodUnderCaret methodUnderCaret = PsiUtils.detectMethodUnderCaret(project,languageService,caretOffset,file);
        caretContextService.contextChanged(methodUnderCaret);
    }
}
