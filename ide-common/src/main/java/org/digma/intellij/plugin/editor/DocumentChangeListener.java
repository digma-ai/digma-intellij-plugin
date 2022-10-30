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
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DocumentChangeListener {

    private static final Logger LOGGER = Logger.getInstance(DocumentChangeListener.class);

    private final Project project;
    private final EditorEventsHandler editorEventsHandler;

    private final DocumentInfoService documentInfoService;
    private final CaretContextService caretContextService;

    private final Alarm documentChangeAlarm;

    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    public DocumentChangeListener(Project project, EditorEventsHandler editorEventsHandler) {
        this.project = project;
        this.editorEventsHandler = editorEventsHandler;
        documentInfoService = project.getService(DocumentInfoService.class);
        caretContextService = project.getService(CaretContextService.class);
        documentChangeAlarm = AlarmFactory.getInstance().create();
    }

    public void maybeAddDocumentListener(Editor editor, PsiFile psiFile, LanguageService languageService) {
        if (disposables.containsKey(psiFile.getVirtualFile())) {
            return;
        }

        addDocumentListener(editor, psiFile, languageService);
    }

    private void addDocumentListener(@NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull LanguageService languageService) {

        if (editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing document listener for {} because it is disposed", editor);
            return;
        }

        Document document = editor.getDocument();

        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(psiFile.getVirtualFile(), parentDisposable);
        Log.log(LOGGER::debug, "adding document listener for file:{}", psiFile.getVirtualFile());

        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {

                documentChangeAlarm.cancelAllRequests();
                documentChangeAlarm.addRequest(() -> {
                    processDocumentChanged(editor, psiFile, languageService);
                }, 300);

            }
        }, parentDisposable);
    }


    private void processDocumentChanged(@NotNull Editor editor, @NotNull PsiFile psiFile, @NotNull LanguageService languageService) {
        PsiDocumentManager.getInstance(project).performLaterWhenAllCommitted(() -> {
            DocumentInfo documentInfo;
            try {
                Map<Integer, DocumentInfo> documentInfoMap =
                        FileBasedIndex.getInstance().getFileData(DocumentInfoIndex.DOCUMENT_INFO_INDEX_ID, psiFile.getVirtualFile(), project);
                //there is only one DocumentInfo per file in the index.
                //all relevant files must be indexed, so if we are here then DocumentInfo must be found in the index is ready,
                // or we have a mistake somewhere else. java interfaces,enums and annotations are indexed but the DocumentInfo
                // object is empty of methods, that's because currently we have no way to exclude those types from indexing.
                documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);
            } catch (IndexNotReadyException e) {
                //IndexNotReadyException will be thrown on dumb mode, when indexing is still in progress.
                documentInfo = languageService.buildDocumentInfo(psiFile);
            }

            if (documentInfo == null) {
                Log.log(LOGGER::error, "Could not find DocumentInfo for file {}", psiFile.getVirtualFile());
                throw new DocumentInfoIndexNotFoundException("Could not find DocumentInfo index for " + psiFile.getVirtualFile());
            }
            Log.log(LOGGER::debug, "Found DocumentInfo index for {},'{}'", psiFile.getVirtualFile(), documentInfo);

            MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, editor.getCaretModel().getOffset());

            update(psiFile, documentInfo, methodUnderCaret);

        });
    }


    private void update(@NotNull PsiFile psiFile, DocumentInfo documentInfo, MethodUnderCaret methodUnderCaret) {
        Backgroundable.ensureBackground(project, "Document changed", () -> {
            //documentInfoService will update the discovery code objects, refresh backend data and call some event
            //so that code lens will be installed.
            //contextChanged will update the UI and must run after documentInfoService.addCodeObjects is finished
            documentInfoService.addCodeObjects(psiFile, documentInfo);
            caretContextService.contextChanged(methodUnderCaret);
        });
    }


    public void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
