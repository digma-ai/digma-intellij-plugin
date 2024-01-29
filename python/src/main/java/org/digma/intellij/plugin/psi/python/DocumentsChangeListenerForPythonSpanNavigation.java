package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import org.digma.intellij.plugin.common.VfsUtilsKt;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * a listener for document change that updates span navigation map.
 * installs a document listener when a document is opened in the editor and updates the span navigation
 * when a document changes. waits for quite period before updating span navigation so to not call span navigation
 * processing too many times.
 */
public class DocumentsChangeListenerForPythonSpanNavigation implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(DocumentsChangeListenerForPythonSpanNavigation.class);

    private final Project project;

    private final PythonLanguageService pythonLanguageService;

    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    public DocumentsChangeListenerForPythonSpanNavigation(@NotNull Project project) {
        this.project = project;
        pythonLanguageService = project.getService(PythonLanguageService.class);
    }


    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        try {

            if (project.isDisposed() || !VfsUtilsKt.isValidVirtualFile(file)) {
                return;
            }

            if (!pythonLanguageService.isRelevant(file)) {
                return;
            }

            if (disposables.containsKey(file)) {
                return;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (PsiUtils.isValidPsiFile(psiFile)) {
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                if (document != null) {
                    Disposable parentDisposable = Disposer.newDisposable();
                    disposables.put(file, parentDisposable);
                    Log.log(LOGGER::debug, "adding document listener for file:{}", file);
                    document.addDocumentListener(new DocumentListener() {

                        private final Alarm documentChangeAlarm = AlarmFactory.getInstance().create();

                        @Override
                        public void documentChanged(@NotNull DocumentEvent event) {

                            try {

                                if (project.isDisposed()) {
                                    return;
                                }

                                documentChangeAlarm.cancelAllRequests();
                                documentChangeAlarm.addRequest(() -> {
                                    try {
                                        PythonSpanNavigationProvider pythonSpanNavigationProvider = PythonSpanNavigationProvider.getInstance(project);
                                        pythonSpanNavigationProvider.documentChanged(event.getDocument());
                                    }catch (Exception e){
                                        Log.warnWithException(LOGGER,e,"Exception in documentChanged");
                                        ErrorReporter.getInstance().reportError(project,"DocumentsChangeListenerForPythonSpanNavigation.DocumentListener.documentChanged",e);
                                    }
                                }, 5000);

                            } catch (Throwable e) {
                                Log.warnWithException(LOGGER, e, "Exception in documentChanged");
                                ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForPythonSpanNavigation.DocumentListener.documentChanged", e);
                            }

                        }
                    }, parentDisposable);
                }

            }

        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in fileOpened");
            ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForPythonSpanNavigation.fileOpened", e);
        }

    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }


}
