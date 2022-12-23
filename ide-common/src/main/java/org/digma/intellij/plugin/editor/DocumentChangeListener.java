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
import com.intellij.openapi.project.IndexNotReadyException;
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
import com.intellij.util.indexing.FileBasedIndex;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.index.DocumentInfoIndex;
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

        DocumentInfo documentInfo;
        try {
            Map<Integer, DocumentInfo> documentInfoMap =
                    FileBasedIndex.getInstance().getFileData(DocumentInfoIndex.DOCUMENT_INFO_INDEX_ID, psiFile.getVirtualFile(), project);
            //there is only one DocumentInfo per file in the index.
            //all relevant files must be indexed, so if we are here then DocumentInfo must be found in the index is ready,
            // or we have a mistake somewhere else. java interfaces,enums and annotations are indexed but the DocumentInfo
            // object is empty of methods, that's because currently we have no way to exclude those types from indexing.
            documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);

            //usually we should find the document info in the index. on extreme cases, maybe if the index is corrupted
            // the document info will not be found, try again to build it
            if (documentInfo == null) {
                documentInfo = languageService.buildDocumentInfo(psiFile);
            }

        } catch (IndexNotReadyException e) {
            //IndexNotReadyException will be thrown on dumb mode, when indexing is still in progress.
            //usually it should not happen because the document listener is installed only in smart mode.
            documentInfo = languageService.buildDocumentInfo(psiFile);
        }

        if (documentInfo == null) {
            Log.log(LOGGER::error, "Could not find DocumentInfo for file {}", psiFile.getVirtualFile());
            throw new DocumentInfoIndexNotFoundException("Could not find DocumentInfo index for " + psiFile.getVirtualFile());
        }
        Log.log(LOGGER::debug, "Found DocumentInfo index for {},'{}'", psiFile.getVirtualFile(), documentInfo);

        languageService.enrichDocumentInfo(documentInfo, psiFile);
        documentInfoService.addCodeObjects(psiFile, documentInfo);
    }


    void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
