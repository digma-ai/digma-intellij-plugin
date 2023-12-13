package org.digma.intellij.plugin.editor;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import com.intellij.util.RunnableCallable;
import com.intellij.util.concurrency.NonUrgentExecutor;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.digma.intellij.plugin.common.FileUtils;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.jetbrains.annotations.NotNull;

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
        caretContextService = project.getService(CaretContextService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        currentContextUpdater = new CurrentContextUpdater(project);
        caretListener = new CaretListener(project,currentContextUpdater);
        documentChangeListener = new DocumentChangeListener(project,currentContextUpdater);
        contextChangeAlarmAfterFileClosed = AlarmFactory.getInstance().create();
    }


    private void ensureStartupOnEdt(Project project) {
        if (startupEnsured){
            return;
        }

        LanguageService.ensureStartupOnEDTForAll(project);

        startupEnsured = true;
    }


    /**
    This is the central event that drives the plugin. if something goes wrong here the plugin will not function.
     */
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {

        if (editorManagerEvent.getNewFile() != null && isFileNotChangingContext(editorManagerEvent.getNewFile())){
            Log.log(LOGGER::debug,"Ignoring file {} because its not changing context",editorManagerEvent.getNewFile());
            return;
        }

        //this will make sure that all registered language services complete startup before they can be used.
        // usually there is nothing to do, but rider for example need to load the protocol models on EDT.
        // in most cases this method will return immediately. Rider has a ServicesStartup StartupActivity that
        // will already do it, this call is here in case this event is fired before all StartupActivity completed.
        ensureStartupOnEdt(project);


        //this method is executed on EDT.
        //most of the code here,access to psi or to the index, needs to be executed on EDT or in Read/Write actions.
        //only the code that adds documentInfo to documentInfoService and contextChanged needs to run on background.
        //when calling contextChanged on EDT it will start a background thread when necessary.

        FileEditorManager fileEditorManager = editorManagerEvent.getManager();

        Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
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

            Log.log(LOGGER::debug, "handling new open file:{}",newFile);

            //wait for smart mode before loading document info and installing caret and document change listeners.
            //if files are opened on startup before indexes are ready there is nothing we can do to build the document
            //info or to discover spans. and anyway our tool window will not be shown on dumb mode.
            ReadAction.nonBlocking(new RunnableCallable(() -> {

                if (editorManagerEvent.getNewEditor() == null || !editorManagerEvent.getNewEditor().isValid()){
                    return;
                }

                PsiFile psiFile = PsiManager.getInstance(project).findFile(newFile);
                if (psiFile == null){
                    Log.log(LOGGER::debug, "No psi file for :{}",newFile);
                    return;
                }

                //if documentInfoService contains this file then the file was already opened before and now its only
                //selectionChanged when changing tabs
                if (documentInfoService.contains(psiFile)) {
                    Log.log(LOGGER::debug, "documentInfoService already contains :{}",newFile);
                    return;
                }

                LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                Log.log(LOGGER::debug, "Found language service {} for :{}",languageService,newFile);

                //some language services need the selected editor , for example CSharpLanguageService need to take
                // getProjectModelId from the selected editor. it may be null
                var newEditor = editorManagerEvent.getNewEditor();
                DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile,newEditor);
                Log.log(LOGGER::debug, "got DocumentInfo for :{}",newFile);

                documentInfoService.addCodeObjects(psiFile, documentInfo);
                Log.log(LOGGER::debug, "documentInfoService updated with DocumentInfo for :{}",newFile);

            })).inSmartMode(project).withDocumentsCommitted(project).finishOnUiThread(ModalityState.defaultModalityState(), unused -> {

                Log.log(LOGGER::debug, "finishing on ui thread for :{}",newFile);

                var selectedTextEditor = fileEditorManager.getSelectedTextEditor();
                if (selectedTextEditor != null) {
                    Log.log(LOGGER::debug, "Found selected editor for :{}",newFile);
                    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument());
                    if (psiFile != null && isRelevantFile(psiFile.getVirtualFile())) {
                        Log.log(LOGGER::debug, "Found relevant psi file for :{}",newFile);
                        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                        Log.log(LOGGER::debug, "Found language service {} for :{}",languageService,newFile);
                        caretListener.maybeAddCaretListener(selectedTextEditor);
                        documentChangeListener.maybeAddDocumentListener(selectedTextEditor);

                        MainToolWindowCardsController.getInstance(project).showMainPanel();
                        ReadAction.nonBlocking(new RunnableCallable(() -> {
                            MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, selectedTextEditor,selectedTextEditor.getCaretModel().getOffset());
                            Log.log(LOGGER::debug, "Found MethodUnderCaret for :{}, '{}'",newFile,methodUnderCaret);
                            caretContextService.contextChanged(methodUnderCaret);
                            Log.log(LOGGER::debug, "contextChanged for :{}, '{}'",newFile,methodUnderCaret);
                        })).inSmartMode(project).withDocumentsCommitted(project).submit(NonUrgentExecutor.getInstance());

                    }else if (psiFile != null){
                        Log.log(LOGGER::debug, "file not supported :{}, calling contextEmptyNonSupportedFile",newFile);
                        MainToolWindowCardsController.getInstance(project).showNonSupported();
                        caretContextService.contextEmptyNonSupportedFile(psiFile.getVirtualFile().getPath());
                    }else{
                        Log.log(LOGGER::debug, "calling contextEmpty for {}",newFile);
                        MainToolWindowCardsController.getInstance(project).showNoFile();
                        caretContextService.contextEmpty();
                    }
                }else{
                    Log.log(LOGGER::debug, "No selected editor for :{}",newFile);
                }

            }).submit(NonUrgentExecutor.getInstance());

        } else if (newFile != null) {
            Log.log(LOGGER::debug, "new file is not relevant {}, calling contextEmptyNonSupportedFile",newFile);
            MainToolWindowCardsController.getInstance(project).showNonSupported();
            caretContextService.contextEmptyNonSupportedFile(newFile.getPath());
        } else {
            Log.log(LOGGER::debug, "new file is null calling contextEmpty");
            MainToolWindowCardsController.getInstance(project).showNoFile();
            caretContextService.contextEmpty();
        }

    }




    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        Log.log(LOGGER::debug, "fileClosed: file:{}", file);

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null && !FileUtils.isVcsFile(file)) {

            Log.log(LOGGER::debug, "found psi file for fileClosed {}",file);
            //this PsiFile may be anything, it may be a supported language file or a non-supported file.
            // in any case try to remove from documentInfoService.

            documentInfoService.removeDocumentInfo(psiFile);

            if (isRelevantFile(file)) {
                Log.log(LOGGER::debug, "psi file is relevant for fileClosed {}",file);
                caretListener.removeCaretListener(file);
                documentChangeListener.removeDocumentListener(file);
            }

        }

        if (!source.hasOpenFiles()){
            Log.log(LOGGER::debug, "no more open editors , calling contextEmpty");
            MainToolWindowCardsController.getInstance(project).showNoFile();
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
            Log.log(LOGGER::debug, "calling updateContextAfterFileClosed");
            updateContextAfterFileClosed(selectedEditor, source);
        }
    }



    //must be executed on EDT
    private void updateContextAfterFileClosed(FileEditor selectedEditor, @NotNull FileEditorManager fileEditorManager) {
        Log.log(LOGGER::debug, "updateContextAfterFileClosed called");
        contextChangeAlarmAfterFileClosed.cancelAllRequests();
        if (selectedEditor != null) {
            var selectedFile = selectedEditor.getFile();
            if ( isRelevantFile(selectedFile) && !FileUtils.isVcsFile(selectedFile)) {
                Log.log(LOGGER::debug, "updateContextAfterFileClosed found selected file {}",selectedFile);
                PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
                var selectedTextEditor = fileEditorManager.getSelectedTextEditor();
                if (psiFile != null && selectedTextEditor != null) {
                    Log.log(LOGGER::debug, "updateContextAfterFileClosed psi file {}",psiFile.getVirtualFile());
                    //each language service may do the refresh differently, Rider is different from others.
                    LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                    Log.log(LOGGER::debug, "calling {}.refreshMethodUnderCaret for {}",languageService,psiFile.getVirtualFile());
                    contextChangeAlarmAfterFileClosed.addRequest(() -> languageService.refreshMethodUnderCaret(project, psiFile,selectedTextEditor, selectedTextEditor.getCaretModel().getOffset()), 200);
                } else {
                    if (isFileChangingContext(selectedFile)) {
                        Log.log(LOGGER::debug, "updateContextAfterFileClosed no psi file for {}, calling contextEmptyNonSupportedFile", selectedFile);
                        MainToolWindowCardsController.getInstance(project).showNonSupported();
                        contextChangeAlarmAfterFileClosed.addRequest(() -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200);
                    }
                }
            }else {
                if (isFileChangingContext(selectedFile)) {
                    Log.log(LOGGER::debug, "updateContextAfterFileClosed selected file is not relevant {}, calling contextEmptyNonSupportedFile",selectedFile);
                    MainToolWindowCardsController.getInstance(project).showNonSupported();
                    contextChangeAlarmAfterFileClosed.addRequest(() -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200);
                }
            }
        } else {
            Log.log(LOGGER::debug, "updateContextAfterFileClosed selected no selected editor, calling contextEmpty");
            MainToolWindowCardsController.getInstance(project).showNoFile();
            contextChangeAlarmAfterFileClosed.addRequest(caretContextService::contextEmpty, 200);
        }
    }



    private boolean isRelevantFile(VirtualFile file) {

        if (file.isDirectory() || !file.isValid()){
            return false;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        return !FileUtils.isLightVirtualFileBase(file) &&
                languageService.isRelevant(file);

    }


    private boolean isFileNotChangingContext(@NotNull VirtualFile file) {
        return DigmaVirtualFileMarker.class.isAssignableFrom(file.getClass());
    }

    private boolean isFileChangingContext(@NotNull VirtualFile file) {
        return !isFileNotChangingContext(file);
    }


}
