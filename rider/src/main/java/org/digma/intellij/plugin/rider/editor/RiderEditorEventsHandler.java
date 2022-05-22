package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RiderEditorEventsHandler implements EditorEventsHandler, DocumentInfoChanged {

    private Project project;
    private CaretContextService caretContextService;

    public RiderEditorEventsHandler(Project project) {
        this.project = project;
    }

    @Override
    public void start(@NotNull Project project, CaretContextService caretContextService, LanguageService languageService) {

        //keep it for later use
        this.caretContextService = caretContextService;

        ElementUnderCaretDetector elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        elementUnderCaretDetector.start(caretContextService);

        project.getMessageBus().connect().subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,this);
    }

    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        if (caretContextService != null){
            ElementUnderCaretDetector elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
            elementUnderCaretDetector.maybeNotifyElementUnderCaret(psiFile,caretContextService);
        }

    }
}
