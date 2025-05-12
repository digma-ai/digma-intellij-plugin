package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.document.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.process.ProcessManager;
import org.digma.intellij.plugin.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * This is the main listener for file open, it will cache a selectionChanged on FileEditorManager and do
 * the necessary actions when a file is opened.
 * This listener is installed only when necessary, for example, on Idea, Pycharm. Usually it will not be installed on Rider
 * unless the python plugin is installed on Rider.
 **/
public class EditorEventsHandler implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);

    private final Project project;
    private final DocumentInfoService documentInfoService;
    private final LanguageServiceLocator languageServiceLocator;
    private final DocumentChangeListener documentChangeListener;

    private boolean startupEnsured = false;

    public EditorEventsHandler(Project project) {
        this.project = project;
        languageServiceLocator = LanguageServiceLocator.getInstance(project);
        documentInfoService = DocumentInfoService.getInstance(project);
        documentChangeListener = new DocumentChangeListener(project);
    }


    private void ensureStartupOnEdt(Project project) {
        if (startupEnsured) {
            return;
        }

        LanguageService.ensureStartupOnEDTForAll(project);

        startupEnsured = true;
    }


    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {
        try {

            if (!ProjectUtilsKt.isProjectValid(project)) {
                return;
            }

            selectionChangedImpl(editorManagerEvent);
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in selectionChanged");
            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.selectionChanged", e);
        }
    }

    private void selectionChangedImpl(@NotNull FileEditorManagerEvent editorManagerEvent) {

        //This will make sure that all registered language services complete startup before they can be used.
        // Usually there is nothing to do, but rider, for example, needs to load the protocol models on EDT.
        // In most cases this method will return immediately. Rider has a ServicesStartup StartupActivity that
        // will already do it, this call is here in case this event is fired before all StartupActivity completed.
        ensureStartupOnEdt(project);


        if (editorManagerEvent.getNewEditor() == null || !editorManagerEvent.getNewEditor().isValid()) {
            return;
        }

        FileEditorManager fileEditorManager = editorManagerEvent.getManager();

        Log.log(LOGGER::trace, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());

        var newFile = editorManagerEvent.getNewFile();

        if (newFile != null && isRelevantFile(newFile)) {

            Log.log(LOGGER::trace, "handling new open file:{}", newFile);

            var newEditor = editorManagerEvent.getNewEditor();

            Backgroundable.executeOnPooledThread(() -> {

                if (!VfsUtilsKt.isValidVirtualFile(newFile)) {
                    return;
                }

                var psiFileCachedValue = PsiUtils.getPsiFileCachedValue(project, newFile);

                var psiFile = psiFileCachedValue.getValue();
                if (!PsiUtils.isValidPsiFile(psiFile)) {
                    Log.log(LOGGER::trace, "Psi file invalid for :{}", newFile);
                    return;
                }

                if (!documentInfoService.contains(psiFile)) {

                    DumbService.getInstance(project).waitForSmartMode();

                    if (newEditor.isValid()) {

                        psiFile = psiFileCachedValue.getValue();
                        if (!PsiUtils.isValidPsiFile(psiFile)) {
                            Log.log(LOGGER::trace, "Psi file invalid for :{}", newFile);
                            return;
                        }

                        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                        Log.log(LOGGER::trace, "Found language service {} for :{}", languageService, newFile);


                        var processName = "EditorEventsHandler.buildDocumentInfo";
                        var context = new BuildDocumentInfoProcessContext(processName);
                        Runnable runnable = () -> {
                            DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFileCachedValue, newEditor, context);
                            Log.log(LOGGER::trace, "got DocumentInfo for :{}", newFile);
                            //get the value again, maybe it was invalidated
                            var upToDatePsiFile = psiFileCachedValue.getValue();
                            if (PsiUtils.isValidPsiFile(upToDatePsiFile)) {
                                documentInfoService.addCodeObjects(upToDatePsiFile, documentInfo);
                                Log.log(LOGGER::trace, "documentInfoService updated with DocumentInfo for :{}", newFile);
                            }
                        };

                        var processResult = project.getService(ProcessManager.class).runTaskUnderProcess(runnable, context, true, 2, false);
                        Log.log(LOGGER::trace, "buildDocumentInfo completed {}", processResult);
                        context.logErrors(LOGGER, project, false);

                    }
                } else {
                    Log.log(LOGGER::trace, "documentInfoService already contains :{}", newFile);
                }

                if (!newEditor.isValid()) {
                    return;
                }


                EDT.ensureEDT(() -> {
                    Log.log(LOGGER::trace, "finishing on ui thread for :{}", newFile);

                    Editor selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(newFile, fileEditorManager);

                    if (selectedTextEditor != null) {
                        Log.log(LOGGER::trace, "Found selected editor for :{}", newFile);
                        PsiFile selectedPsiFile = SlowOperationsUtilsKt.allowSlowOperation(() ->
                                PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument()));

                        if (PsiUtils.isValidPsiFile(selectedPsiFile) && isRelevantFile(selectedPsiFile.getVirtualFile())) {
                            Log.log(LOGGER::trace, "Found relevant psi file for :{}", newFile);
                            documentChangeListener.maybeAddDocumentListener(selectedTextEditor);
                        }
                    } else {
                        Log.log(LOGGER::trace, "No selected editor for :{}", newFile);
                    }
                });

            });
        }
    }


    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            if (file.isValid()) {
                fileClosedImpl(file);
            } else {
                Log.log(LOGGER::warn, "got invalid file in EditorEventsHandler.fileClosed");
            }
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in fileClosed");
            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.fileClosed", e);
        }
    }


    private void fileClosedImpl(@NotNull VirtualFile file) {

        Log.log(LOGGER::trace, "fileClosed: file:{}", file);

        if (!VfsUtilsKt.isValidVirtualFile(file)) {
            return;
        }

        PsiFile psiFile = SlowOperationsUtilsKt.allowSlowOperation(() -> PsiManager.getInstance(project).findFile(file));

        if (PsiUtils.isValidPsiFile(psiFile) && !FileUtils.isVcsFile(file) && isRelevantFile(file)) {
            Log.log(LOGGER::trace, "found psi file for fileClosed {}", file);
            documentInfoService.removeDocumentInfo(psiFile);
            Log.log(LOGGER::trace, "psi file is relevant for fileClosed {}", file);
            documentChangeListener.removeDocumentListener(file);
        }
    }


    private boolean isRelevantFile(VirtualFile file) {

        if (!VfsUtilsKt.isValidVirtualFile(file)) {
            return false;
        }

        return PsiAccessUtilsKt.runInReadAccessWithResult(() -> {
            if (file.isDirectory() || !VfsUtilsKt.isValidVirtualFile(file)) {
                return false;
            }

            PsiFile psiFile = SlowOperationsUtilsKt.allowSlowOperation(() -> PsiManager.getInstance(project).findFile(file));

            if (!PsiUtils.isValidPsiFile(psiFile)) {
                return false;
            }
            LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());

            return !FileUtils.isLightVirtualFileBase(file) && languageService.isRelevant(file);
        });
    }


}
