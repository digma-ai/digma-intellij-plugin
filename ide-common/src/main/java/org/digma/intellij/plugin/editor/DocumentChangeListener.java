package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import com.intellij.util.Alarm.ThreadToUse;
import com.intellij.util.AlarmFactory;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to documentChanged events and updates DocumentInfoService and the current context.
 */
class DocumentChangeListener {

    private static final Logger LOGGER = Logger.getInstance(DocumentChangeListener.class);

    private final Project project;
    private final DocumentInfoService documentInfoService;
    private final CurrentContextUpdater currentContextUpdater;
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    DocumentChangeListener(Project project, CurrentContextUpdater currentContextUpdater) {
        this.project = project;
        documentInfoService = DocumentInfoService.getInstance(project);
        this.currentContextUpdater = currentContextUpdater;
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

            private final Alarm documentChangeAlarm = AlarmFactory.getInstance().create(ThreadToUse.POOLED_THREAD,parentDisposable);

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {

                if (project.isDisposed()) {
                    return;
                }

                //must be executed on EDT
                PsiFile changedPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.getDocument());
                if (changedPsiFile == null){
                    Log.log(LOGGER::debug, "changedPsiFile is null for {}", event.getDocument());
                    return;
                }
                var fileEditor =  FileEditorManager.getInstance(project).getSelectedEditor(changedPsiFile.getVirtualFile());

                documentChangeAlarm.cancelAllRequests();
                documentChangeAlarm.addRequest(() -> {

                    try {
                        Log.log(LOGGER::debug, "got documentChanged alarm for {}", event.getDocument());
                        Log.log(LOGGER::debug, "Processing documentChanged event for {}", changedPsiFile.getVirtualFile());
                        processDocumentChanged(changedPsiFile,fileEditor);
                    } catch (Exception e) {
                        Log.warnWithException(LOGGER, e, "exception while processing documentChanged event for file: {}, {}", event.getDocument(), e.getMessage());
                    }

                    EDT.ensureEDT(() -> {
                        //caret event is not always fired while editing, but the document may change, and a caret
                        // event will fire only when the caret moves but not while editing.
                        // if the document changes and no caret event is fired the UI will not be updated.
                        // so calling here currentContextUpdater after document change will update the UI.
                        var editor1 = EditorUtils.getSelectedTextEditorForFile(changedPsiFile.getVirtualFile(), FileEditorManager.getInstance(project));
                        if (editor1 != null){
                            int caretOffset = editor1.logicalPositionToOffset(editor1.getCaretModel().getLogicalPosition());
                            var file = FileDocumentManager.getInstance().getFile(editor1.getDocument());
                            currentContextUpdater.clearLatestMethod();
                            currentContextUpdater.addRequest(editor1,caretOffset,file);
                        }
                    });
                },2000);

            }
        }, parentDisposable);
    }


    private void processDocumentChanged(@NotNull PsiFile psiFile, FileEditor fileEditor) {

        if (project.isDisposed()) {
            return;
        }

        EDT.assertNonDispatchThread();

        LanguageService languageService = LanguageServiceLocator.getInstance(project).locate(psiFile.getLanguage());

        //todo: try to improve.
        // see : https://github.com/digma-ai/digma-intellij-plugin/issues/343

        DumbService.getInstance(project).waitForSmartMode();

        if (fileEditor.isValid()) {

            DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile, fileEditor);

            Log.log(LOGGER::debug, "got DocumentInfo for {}", psiFile.getVirtualFile());

            documentInfoService.addCodeObjects(psiFile, documentInfo);
            Log.log(LOGGER::debug, "documentInfoService updated with DocumentInfo for {}", psiFile.getVirtualFile());
        }

    }


    void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
