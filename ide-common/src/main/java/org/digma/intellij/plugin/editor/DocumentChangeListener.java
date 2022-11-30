package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import com.intellij.util.indexing.FileBasedIndex;
import org.digma.intellij.plugin.common.Backgroundable;
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
                    //this code is always executed in smart mode because the listener is installed only in smart mode
                    PsiFile fileToQuery = PsiDocumentManager.getInstance(project).getPsiFile(event.getDocument());
                    //probably should never happen
                    if (fileToQuery == null) {
                        return;
                    }
                    try {

                        processDocumentChanged(editor, fileToQuery);
                    } catch (PsiInvalidElementAccessException e) {
                        Log.debugWithException(LOGGER, e, "exception while processing file: {}, {}", fileToQuery.getVirtualFile(), e.getMessage());
                    }

                }, 500);

            }
        }, parentDisposable);
    }


    private void processDocumentChanged(@NotNull Editor editor, @NotNull PsiFile psiFile) {

        if (project.isDisposed()) {
            return;
        }


        PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {

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

                //usually we should find the document info in the index. on extreme cases, maybe is the index is corrupted
                // the document info will not be found, try again to build it
                if (documentInfo == null) {
                    documentInfo = languageService.buildDocumentInfo(psiFile);
                }

            } catch (IndexNotReadyException e) {
                //IndexNotReadyException will be thrown on dumb mode, when indexing is still in progress.
                //usually it should not happen because the listener is installed only in smart mode.
                documentInfo = languageService.buildDocumentInfo(psiFile);
            }

            if (documentInfo == null) {
                Log.log(LOGGER::error, "Could not find DocumentInfo for file {}", psiFile.getVirtualFile());
                throw new DocumentInfoIndexNotFoundException("Could not find DocumentInfo index for " + psiFile.getVirtualFile());
            }
            Log.log(LOGGER::debug, "Found DocumentInfo index for {},'{}'", psiFile.getVirtualFile(), documentInfo);

            MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, editor.getCaretModel().getOffset());

            update(languageService, psiFile, documentInfo, methodUnderCaret);

        });
    }


    private void update(@NotNull LanguageService languageService, @NotNull PsiFile psiFile, DocumentInfo documentInfo, MethodUnderCaret methodUnderCaret) {
        Backgroundable.ensureBackground(project, "Document changed", () -> {
            //documentInfoService will update the discovery code objects, refresh backend data and call some event
            //so that code lens will be installed.
            //contextChanged will update the UI and must run after documentInfoService.addCodeObjects is finished
            //enrichDocumentInfo is meant mainly to discover spans. the DocumentInfoIndex can
            // not discover spans because there is no reference resolving during file based index.
            languageService.enrichDocumentInfo(documentInfo, psiFile);
            documentInfoService.addCodeObjects(psiFile, documentInfo);
            caretContextService.contextChanged(methodUnderCaret);
        });
    }


    void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
