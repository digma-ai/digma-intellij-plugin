package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.*;
import com.intellij.util.Alarm.ThreadToUse;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.*;

import java.util.*;

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

                try {

                    if (!ProjectUtilsKt.isProjectValid(project)) {
                        return;
                    }


                    var virtualFile = FileDocumentManager.getInstance().getFile(event.getDocument());

                    if (virtualFile == null || !VfsUtilsKt.isValidVirtualFile(virtualFile)) {
                        return;
                    }

                    var psiFileCachedValue = PsiUtils.getPsiFileCachedValue(project, virtualFile);

                    @Nullable
                    var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);

                    documentChangeAlarm.cancelAllRequests();
                    documentChangeAlarm.addRequest(() -> {

                        try {
                            Log.log(LOGGER::debug, "got documentChanged alarm for {}", event.getDocument());
                            Log.log(LOGGER::debug, "Processing documentChanged event for {}", virtualFile);
                            processDocumentChanged(psiFileCachedValue, fileEditor);
                        } catch (Exception e) {
                            Log.warnWithException(LOGGER, e, "exception while processing documentChanged event for file: {}, {}", event.getDocument(), e.getMessage());
                            ErrorReporter.getInstance().reportError(project, "DocumentChangeListener.documentChanged", e);
                        }

                        EDT.ensureEDT(() -> {
                            //caret event is not always fired while editing, but the document may change, and a caret
                            // event will fire only when the caret moves but not while editing.
                            // if the document changes and no caret event is fired the UI will not be updated.
                            // so calling here currentContextUpdater after document change will update the UI.
                            var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                            if (selectedTextEditor != null) {
                                int caretOffset = selectedTextEditor.logicalPositionToOffset(selectedTextEditor.getCaretModel().getLogicalPosition());
                                var file = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
                                currentContextUpdater.clearLatestMethod();
                                currentContextUpdater.addRequest(selectedTextEditor, caretOffset, file);
                            }
                        });
                    }, 2000);
                } catch (Exception e) {
                    Log.warnWithException(LOGGER, e, "exception DocumentChangeListener.documentChanged");
                    ErrorReporter.getInstance().reportError(project, "DocumentChangeListener.documentChanged", e);
                }

            }
        }, parentDisposable);
    }


    private void processDocumentChanged(@NotNull PsiFileCachedValueWithUri psiFileCachedValue, @Nullable FileEditor fileEditor) {

        if (!ProjectUtilsKt.isProjectValid(project) || !PsiUtils.isValidPsiFile(psiFileCachedValue.getValue())) {
            return;
        }

        EDT.assertNonDispatchThread();

        LanguageService languageService = LanguageServiceLocator.getInstance(project).locate(psiFileCachedValue.getValue().getLanguage());

        BuildDocumentInfoProcessContext.buildDocumentInfoUnderProcess(project, psiFileCachedValue.getValue().getName(), progressIndicator -> {
            var context = new BuildDocumentInfoProcessContext(progressIndicator);
            DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFileCachedValue, fileEditor, context);
            Log.log(LOGGER::debug, "got DocumentInfo for {}", psiFileCachedValue.getValue().getVirtualFile());
            documentInfoService.addCodeObjects(psiFileCachedValue.getValue(), documentInfo);
            Log.log(LOGGER::debug, "documentInfoService updated with DocumentInfo for {}", psiFileCachedValue.getValue().getVirtualFile());
        });
    }


    void removeDocumentListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }
}
