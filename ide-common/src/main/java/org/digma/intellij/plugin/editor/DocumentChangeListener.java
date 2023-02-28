package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to documentChanged events and updates DocumentInfoService and the current context.
 * This class is only used from the current package by EditorEventsHandler so it and its methods are
 * 'package' access.
 */
class DocumentChangeListener {

    private static final Logger LOGGER = Logger.getInstance(DocumentChangeListener.class);

    private final Project project;
    private final DocumentInfoService documentInfoService;
    private final CaretContextService caretContextService;

    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    DocumentChangeListener(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
        caretContextService = project.getService(CaretContextService.class);
    }

    void maybeAddDocumentListener(@NotNull Editor editor) {

        if (editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing document listener for {} because it is disposed", editor);
            return;
        }

        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        //probably should never happen
        if (psiFile == null) {
            return;
        }

        VirtualFile virtualFile = psiFile.getVirtualFile();

        //this is a check if this document already has a document listener
        if (disposables.containsKey(virtualFile)) {
            return;
        }

        addDocumentListener(editor, virtualFile);
    }

    private void addDocumentListener(@NotNull Editor editor, @NotNull VirtualFile virtualFile) {

        Document document = editor.getDocument();

        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(virtualFile, parentDisposable);
        Log.log(LOGGER::debug, "adding document listener for file:{}", virtualFile);

        document.addDocumentListener(new DocumentListener() {

            private final Alarm documentChangeAlarm = AlarmFactory.getInstance().create();

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {

                if (project.isDisposed()) {
                    return;
                }

                documentChangeAlarm.cancelAllRequests();
                documentChangeAlarm.addRequest(() ->
                {
                    //this code is always executed in smart mode because the document listener is installed only in smart mode
                    PsiFile changedPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.getDocument());
                    //probably should never happen
                    if (changedPsiFile == null) {
                        return;
                    }

                    ReadAction.nonBlocking(new RunnableCallable(() -> {
                        try {
                            Log.log(LOGGER::debug, "Processing documentChanged event for {}", changedPsiFile.getVirtualFile());
                            processDocumentChanged(changedPsiFile);
                        } catch (PsiInvalidElementAccessException e) {
                            Log.debugWithException(LOGGER, e, "exception while processing documentChanged event for file: {}, {}", changedPsiFile.getVirtualFile(), e.getMessage());
                        }
                    })).inSmartMode(project).withDocumentsCommitted(project).finishOnUiThread(ModalityState.defaultModalityState(), unused -> {

                        var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                        if (selectedTextEditor != null) {
                            PsiFile selectedPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument());
                            //if the selected editor is still the file that was changed in this event then call contextChanged
                            // otherwise do nothing
                            if (selectedPsiFile != null && selectedPsiFile.equals(changedPsiFile)) {
                                LanguageService languageService = LanguageServiceLocator.getInstance(project).locate(selectedPsiFile.getLanguage());
                                MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, selectedPsiFile, selectedTextEditor.getCaretModel().getOffset());
                                caretContextService.contextChanged(methodUnderCaret);
                            }
                        }

                    }).submit(NonUrgentExecutor.getInstance());

                }, 500);

            }
        }, parentDisposable);
    }


    private void processDocumentChanged(@NotNull PsiFile psiFile) {

        if (project.isDisposed()) {
            return;
        }

        LanguageService languageService = LanguageServiceLocator.getInstance(project).locate(psiFile.getLanguage());

        //todo: try to improve.
        // we call buildDocumentInfo for every document change event. and call
        // documentInfoService.addCodeObjects that will actually reload all insights
        // and update the current context.
        // maybe we can improve it and only do that if something relevant was changed
        // by checking the code block that was changed.
        DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile);

        documentInfoService.addCodeObjects(psiFile, documentInfo);
    }




    void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
