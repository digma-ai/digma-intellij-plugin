package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

public class RiderEditorEventsHandler implements EditorEventsHandler, DocumentInfoChanged {

    private Logger LOGGER = Logger.getInstance(RiderEditorEventsHandler.class);

    private Project project;
    private CaretContextService caretContextService;

    private final LanguageService languageService;

    public RiderEditorEventsHandler(Project project) {
        this.project = project;
        languageService = project.getService(LanguageService.class);
    }

    @Override
    public void start(@NotNull Project project, CaretContextService caretContextService, LanguageService languageService) {

        //keep it for later use
        this.caretContextService = caretContextService;

        ElementUnderCaretDetector elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        elementUnderCaretDetector.start(caretContextService);

        project.getMessageBus().connect().subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,this);


        //todo: keep this code as example until we are sure we don't need it.
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            //resharper does not send an event on non-supported files,and when a non-supported file is opened our context is still shown
            // in the tool window.
            //sometimes resharper will send a caret event when a source file looses focus, so in ElementUnderCaretHost, if there
            //is a caret event for a non-visible source file the context is cleared and sends an event to the fronend, this has the same
            // effect as the listener here. but that doesn't always happen so we still need this listener here.

            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditorManager fileEditorManager = event.getManager();
                Log.log(LOGGER::info, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                        event.getNewFile(), event.getOldFile());

                var newFile = event.getNewFile();
                if (newFile == null){
                    return;
                }

                if (!languageService.isSupportedFile(project,newFile)){
                    Log.log(LOGGER::info, "Non supported file opened, clearing context. {}", newFile);
                    caretContextService.contextEmpty();
                    elementUnderCaretDetector.emptyModel();
                }
            }
        });
    }

    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        if (caretContextService != null){
            Log.log(LOGGER::info, "Got documentInfoChanged event for {}", PsiUtils.psiFileToDocumentProtocolKey(psiFile));
            ElementUnderCaretDetector elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
            elementUnderCaretDetector.maybeNotifyElementUnderCaret(psiFile,caretContextService);
        }

    }
}
