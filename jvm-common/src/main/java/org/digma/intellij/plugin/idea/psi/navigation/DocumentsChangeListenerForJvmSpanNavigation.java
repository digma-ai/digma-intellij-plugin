package org.digma.intellij.plugin.idea.psi.navigation;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.*;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.idea.navigation.*;
import org.digma.intellij.plugin.idea.psi.JvmLanguageService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * a listener for document change that updates span navigation map.
 * installs a document listener when a document is opened in the editor and updates the span navigation
 * when a document changes. waits for quite period before updating span navigation so to not call span navigation
 * processing too many times.
 */
public class DocumentsChangeListenerForJvmSpanNavigation implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(DocumentsChangeListenerForJvmSpanNavigation.class);

    private final Project project;

    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    public DocumentsChangeListenerForJvmSpanNavigation(@NotNull Project project) {
        this.project = project;
    }


    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        try {

            if (!ProjectUtilsKt.isProjectValid(project) || !VfsUtilsKt.isValidVirtualFile(file)) {
                return;
            }

            //that means a listener is already installed
            if (disposables.containsKey(file)) {
                return;
            }

            var languageService = LanguageService.findLanguageServiceByFile(project, file);

            //only jvm languages are supported here
            if (!JvmLanguageService.class.isAssignableFrom(languageService.getClass())) {
                return;
            }


            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (PsiUtils.isValidPsiFile(psiFile) && languageService.isRelevant(file)) {
                Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                if (document != null) {
                    Disposable parentDisposable = Disposer.newDisposable();
                    disposables.put(file, parentDisposable);
                    Log.log(LOGGER::debug, "adding document listener for file:{}", file);
                    document.addDocumentListener(new DocumentListener() {

                        private final Alarm documentChangeAlarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.POOLED_THREAD, JvmSpanNavigationProvider.getInstance(project));

                        @Override
                        public void documentChanged(@NotNull DocumentEvent event) {

                            try {

                                if (!ProjectUtilsKt.isProjectValid(project)) {
                                    return;
                                }

                                documentChangeAlarm.cancelAllRequests();
                                documentChangeAlarm.addRequest(() -> {
                                    try {
                                        JvmSpanNavigationProvider.getInstance(project).documentChanged(event.getDocument());
                                        JvmEndpointNavigationProvider.getInstance(project).documentChanged(event.getDocument());
                                    } catch (Throwable e) {
                                        Log.warnWithException(LOGGER, e, "Exception in documentChanged");
                                        ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForJvmSpanNavigation.DocumentListener.documentChanged", e);
                                    }
                                }, 5000);


                            } catch (Throwable e) {
                                Log.warnWithException(LOGGER, e, "Exception in documentChanged");
                                ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForJvmSpanNavigation.DocumentListener.documentChanged", e);
                            }
                        }

                    }, parentDisposable);
                }

            }
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in fileOpened");
            ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForJvmSpanNavigation.fileOpened", e);
        }

    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            var disposable = disposables.remove(file);
            if (disposable != null) {
                Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
                Disposer.dispose(disposable);
            }
        } catch (Throwable e) {
            ErrorReporter.getInstance().reportError(project, "DocumentsChangeListenerForJvmSpanNavigation.fileClosed", e);
        }
    }


}
