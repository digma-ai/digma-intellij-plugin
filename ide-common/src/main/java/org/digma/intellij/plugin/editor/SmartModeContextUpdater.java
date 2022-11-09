package org.digma.intellij.plugin.editor;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;


/**
 * because we do span discovery only in smart mode there may be some documents that are opened in dumb mode
 * and their summaries don't include spans.
 * when documents are opened in dumb mode they will be enriched with spans only in smart mode.
 * this StartupActivity will refresh all loaded documents and call contextChanged for the selected editor
 * so that everything will refresh and show all insights etc.
 */
public class SmartModeContextUpdater implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {

        //todo: not really working because it runs before the span enrichments are finished
        // try to find a way to wait for all ReadAction.nonBlocking.inSmartMode

        DocumentInfoService.getInstance(project).refreshAll();

        var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null){
            var virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (virtualFile != null) {
                var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile != null){
                    LanguageService languageService = LanguageServiceLocator.getInstance(project).locate(psiFile.getLanguage());
                    var offset = editor.getCaretModel().getOffset();
                    var methodUnderCaret = languageService.detectMethodUnderCaret(project,psiFile,offset);
                    CaretContextService caretContextService = project.getService(CaretContextService.class);
                    caretContextService.contextChanged(methodUnderCaret);
                }
            }
        }
    }
}
