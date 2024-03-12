package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.*;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.*;
import org.digma.intellij.plugin.psi.*;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.jetbrains.annotations.NotNull;

import static org.digma.intellij.plugin.common.AlarmUtilsKt.addRequestWithErrorReporting;

/**
 * This is the main listener for file open , it will cache a selectionChanged on FileEditorManager and do
 * the necessary actions when file is opened.
 * This listener is installed only when necessary,for example on Idea,Pycharm. usually it will not be installed on Rider
 * unless python plugin is installed on Rider.
 **/
public class EditorEventsHandler implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);

    private final Project project;
    private final CaretContextService caretContextService;
    private final DocumentInfoService documentInfoService;
    private final LanguageServiceLocator languageServiceLocator;
    private final CaretListener caretListener;
    private final DocumentChangeListener documentChangeListener;
    private final CurrentContextUpdater currentContextUpdater;
    private final Alarm contextChangeAlarmAfterFileClosed;

    private boolean startupEnsured = false;

    public EditorEventsHandler(Project project) {
        this.project = project;
        caretContextService = CaretContextService.getInstance(project);
        languageServiceLocator = LanguageServiceLocator.getInstance(project);
        documentInfoService = DocumentInfoService.getInstance(project);
        currentContextUpdater = project.getService(CurrentContextUpdater.class);
        caretListener = new CaretListener(project, currentContextUpdater);
        documentChangeListener = new DocumentChangeListener(project, currentContextUpdater);
        contextChangeAlarmAfterFileClosed = AlarmFactory.getInstance().create();
    }


    private void ensureStartupOnEdt(Project project) {
        if (startupEnsured) {
            return;
        }

        LanguageService.ensureStartupOnEDTForAll(project);

        startupEnsured = true;
    }


    /**
     * This is the central event that drives the plugin. if something goes wrong here the plugin will not function.
     */
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

        //this will make sure that all registered language services complete startup before they can be used.
        // usually there is nothing to do, but rider for example need to load the protocol models on EDT.
        // in most cases this method will return immediately. Rider has a ServicesStartup StartupActivity that
        // will already do it, this call is here in case this event is fired before all StartupActivity completed.
        ensureStartupOnEdt(project);


        if (editorManagerEvent.getNewEditor() == null || !editorManagerEvent.getNewEditor().isValid()) {
            return;
        }


        //this method is executed on EDT.
        //most of the code here,access to psi or to the index, needs to be executed on EDT or in Read/Write actions.
        //only the code that adds documentInfo to documentInfoService and contextChanged needs to run on background.
        //when calling contextChanged on EDT it will start a background thread when necessary.

        FileEditorManager fileEditorManager = editorManagerEvent.getManager();

        Log.log(LOGGER::trace, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());


        /*
            caretListener.cancelAllCaretPositionChangedRequests
            this comes to solve the following scenario:
            caretListener waits for a quite period before actually processing the caretPositionChanged event.
            when editor with class A is opened and clicking another class ,say B, a caretPositionChanged event is fired
            for A, but will wait for the quite period to be processed. class B is opened and our UI context changes correctly,
            but then the caretPositionChanged for A is processed and changes the context back to A.
            canceling the request solves it.
            and there should be no other effect for canceling all requests in any other scenario.
         */
        currentContextUpdater.cancelAllCaretPositionChangedRequests();

        //see comment in fileClosed before calling updateContextAfterFileClosed
        // if we're here then we can cancel contextChangeAlarmAfterFileClosed
        contextChangeAlarmAfterFileClosed.cancelAllRequests();


        var newFile = editorManagerEvent.getNewFile();

        //ignore non supported files. newFile may be null when the last editor is closed.
        //A relevant file is a source file that is supported by one of the language services.

        if (newFile != null && isRelevantFile(newFile)) {

            Log.log(LOGGER::trace, "handling new open file:{}", newFile);

            //some language services need the selected editor , for exampl  e CSharpLanguageService need to take
            // getProjectModelId from the selected editor. it may be null
            var newEditor = editorManagerEvent.getNewEditor();


            //wait for smart mode before loading document info and installing caret and document change listeners.
            //if files are opened on startup before indexes are ready there is nothing we can do to build the document
            //info or to discover spans. and anyway our tool window will not be shown on dumb mode.
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

                //psi file is non-null and valid here


                //if documentInfoService contains this file then the file was already opened before and now its only
                //selectionChanged when changing tabs
                if (!documentInfoService.contains(psiFile)) {

                    DumbService.getInstance(project).waitForSmartMode();

                    if (newEditor.isValid()) {

                        //get up-to-date file again because it's after waiting for smart mode and the file may be invalidated
                        psiFile = psiFileCachedValue.getValue();
                        if (!PsiUtils.isValidPsiFile(psiFile)) {
                            Log.log(LOGGER::trace, "Psi file invalid for :{}", newFile);
                            return;
                        }


                        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                        Log.log(LOGGER::trace, "Found language service {} for :{}", languageService, newFile);


                        BuildDocumentInfoProcessContext.buildDocumentInfoUnderProcess(project, psiFile.getName(), progressIndicator -> {
                            var context = new BuildDocumentInfoProcessContext(progressIndicator);
                            DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFileCachedValue, newEditor, context);
                            Log.log(LOGGER::trace, "got DocumentInfo for :{}", newFile);
                            //get the value again, maybe it was invalidated
                            var upToDatePsiFile = psiFileCachedValue.getValue();
                            if (PsiUtils.isValidPsiFile(upToDatePsiFile)) {
                                documentInfoService.addCodeObjects(upToDatePsiFile, documentInfo);
                                Log.log(LOGGER::trace, "documentInfoService updated with DocumentInfo for :{}", newFile);
                            }
                        });
                    }
                } else {
                    Log.log(LOGGER::trace, "documentInfoService already contains :{}", newFile);
                }

                if (!newEditor.isValid()) {
                    return;
                }


                EDT.ensureEDT(() -> {
                    Log.log(LOGGER::trace, "finishing on ui thread for :{}", newFile);

                    //get the editor where the file is opened not just the selected editor
                    Editor selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(newFile, fileEditorManager);

                    if (selectedTextEditor != null) {
                        Log.log(LOGGER::trace, "Found selected editor for :{}", newFile);
                        PsiFile selectedPsiFile = SlowOperationsUtilsKt.allowSlowOperation(() ->
                                PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument()));

                        if (PsiUtils.isValidPsiFile(selectedPsiFile) && isRelevantFile(selectedPsiFile.getVirtualFile())) {
                            Log.log(LOGGER::trace, "Found relevant psi file for :{}", newFile);
                            caretListener.maybeAddCaretListener(selectedTextEditor);
                            documentChangeListener.maybeAddDocumentListener(selectedTextEditor);

                            MainToolWindowCardsController.getInstance(project).showMainPanel();

                            int offset = selectedTextEditor.getCaretModel().getOffset();

                            Backgroundable.executeOnPooledThread(() -> {
                                LanguageService languageService1 = languageServiceLocator.locate(selectedPsiFile.getLanguage());
                                MethodUnderCaret methodUnderCaret = languageService1.detectMethodUnderCaret(project, selectedPsiFile, selectedTextEditor, offset);
                                Log.log(LOGGER::trace, "Found MethodUnderCaret for :{}, '{}'", newFile, methodUnderCaret);
                                caretContextService.contextChanged(methodUnderCaret);
                                Log.log(LOGGER::trace, "contextChanged for :{}, '{}'", newFile, methodUnderCaret);
                            });


                        } else if (selectedPsiFile != null) {
                            Log.log(LOGGER::trace, "file not supported :{}, calling contextEmptyNonSupportedFile", newFile);
                            caretContextService.contextEmptyNonSupportedFile(selectedPsiFile.getVirtualFile().getPath());
                        } else {
                            Log.log(LOGGER::trace, "calling contextEmpty for {}", newFile);
                            caretContextService.contextEmpty();
                        }
                    } else {
                        Log.log(LOGGER::trace, "No selected editor for :{}", newFile);
                    }
                });

            });


        } else if (newFile != null) {
            Log.log(LOGGER::trace, "new file is not relevant {}, calling contextEmptyNonSupportedFile", newFile);
            caretContextService.contextEmptyNonSupportedFile(newFile.getPath());
        } else {
            Log.log(LOGGER::trace, "new file is null calling contextEmpty");
            caretContextService.contextEmpty();
        }

    }


    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            if (file.isValid()) {
                fileClosedImpl(source, file);
            } else {
                Log.log(LOGGER::warn, "got invalid file in EditorEventsHandler.fileClosed");
            }
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in fileClosed");
            ErrorReporter.getInstance().reportError(project, "EditorEventsHandler.fileClosed", e);
        }
    }


    private void fileClosedImpl(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        Log.log(LOGGER::trace, "fileClosed: file:{}", file);

        if (!VfsUtilsKt.isValidVirtualFile(file)) {
            return;
        }

        PsiFile psiFile = SlowOperationsUtilsKt.allowSlowOperation(() -> PsiManager.getInstance(project).findFile(file));

        if (PsiUtils.isValidPsiFile(psiFile) && !FileUtils.isVcsFile(file) && isRelevantFile(file)) {

            Log.log(LOGGER::trace, "found psi file for fileClosed {}", file);

            documentInfoService.removeDocumentInfo(psiFile);
            Log.log(LOGGER::trace, "psi file is relevant for fileClosed {}", file);
            caretListener.removeCaretListener(file);
            documentChangeListener.removeDocumentListener(file);
        }

        if (!source.hasOpenFiles()) {
            Log.log(LOGGER::trace, "no more open editors , calling contextEmpty");
            caretContextService.contextEmpty();
            return;
        }

        //sometimes, can't say why, when a tab is closed and another tab becomes visible, selectionChanged is not called
        // until the tab is clicked. fileClosed is always called. most of the time it works ok, but sometimes not.
        // in that case our plugin context keeps showing the closed tab info.
        // updating the context here with the selected editor file when a file is closed solves it. but then if
        // selectionChanged is called, which happens most of the time, then we will update the context twice.
        // so updateContextAfterFileClosed will add the contextChanged request in an Alarm with a delay of 200 millis,
        // if selectionChanged is called right after that it will cancel the Alarm, and hopefully we don't update twice.
        // worst case is that sometimes there will be a delay of 200 millis in updating the context, which as said usually
        // it works ok.
        var selectedEditor = source.getSelectedEditor();
        if (selectedEditor != null) {
            Log.log(LOGGER::trace, "calling updateContextAfterFileClosed");
            updateContextAfterFileClosed(selectedEditor, source);
        }
    }


    //must be executed on EDT
    private void updateContextAfterFileClosed(FileEditor selectedEditor, @NotNull FileEditorManager fileEditorManager) {
        Log.log(LOGGER::trace, "updateContextAfterFileClosed called");
        contextChangeAlarmAfterFileClosed.cancelAllRequests();
        if (selectedEditor != null) {
            var selectedFile = selectedEditor.getFile();
            if (VfsUtilsKt.isValidVirtualFile(selectedFile) && isRelevantFile(selectedFile) && !FileUtils.isVcsFile(selectedFile)) {
                Log.log(LOGGER::trace, "updateContextAfterFileClosed found selected file {}", selectedFile);
                PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
                var selectedTextEditor = EditorUtils.getSelectedTextEditorForFile(selectedFile, fileEditorManager);
                if (PsiUtils.isValidPsiFile(psiFile) && selectedTextEditor != null) {
                    Log.log(LOGGER::trace, "updateContextAfterFileClosed psi file {}", psiFile.getVirtualFile());
                    //each language service may do the refresh differently, Rider is different from others.
                    LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                    Log.log(LOGGER::trace, "calling {}.refreshMethodUnderCaret for {}", languageService, psiFile.getVirtualFile());
                    addRequestWithErrorReporting(contextChangeAlarmAfterFileClosed, () -> languageService.refreshMethodUnderCaret(project, psiFile, selectedTextEditor, selectedTextEditor.getCaretModel().getOffset()), 200, "EditorEventsHandler.updateContextAfterFileClosed");
                } else {
                    Log.log(LOGGER::trace, "updateContextAfterFileClosed no psi file for {}, calling contextEmptyNonSupportedFile", selectedFile);
                    addRequestWithErrorReporting(contextChangeAlarmAfterFileClosed, () -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200, "EditorEventsHandler.updateContextAfterFileClosed");
                }
            } else {
                Log.log(LOGGER::trace, "updateContextAfterFileClosed selected file is not relevant {}, calling contextEmptyNonSupportedFile", selectedFile);
                addRequestWithErrorReporting(contextChangeAlarmAfterFileClosed, () -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200, "EditorEventsHandler.updateContextAfterFileClosed");
            }
        } else {
            Log.log(LOGGER::trace, "updateContextAfterFileClosed selected no selected editor, calling contextEmpty");
            addRequestWithErrorReporting(contextChangeAlarmAfterFileClosed, caretContextService::contextEmpty, 200, "EditorEventsHandler.updateContextAfterFileClosed");
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
