package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * This is the main listener for file open , it will cache a selectionChanged on FileEditorManager and do
 * the necessary actions when file is opened.
 * This listener is installed only when necessary,for example on Idea,Pycharm. usually it will not be installed on Rider
 * unless python plugin is installed on Rider.
 **/
public class EditorEventsHandler implements FileEditorManagerListener {

    //todo:
    // start installing caret listener only after the tool window is opened

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);

    private final Project project;
    private final CaretContextService caretContextService;
    private final DocumentInfoService documentInfoService;
    private final LanguageServiceLocator languageServiceLocator;
    private final CaretListener caretListener;
    private final DocumentChangeListener documentChangeListener;
    private final ProjectFileIndex projectFileIndex;
    private final Alarm contextChangeAlarmAfterFileClosed;


    public EditorEventsHandler(Project project) {
        this.project = project;
        caretContextService = project.getService(CaretContextService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        caretListener = new CaretListener(project);
        documentChangeListener = new DocumentChangeListener(project);
        projectFileIndex = ProjectFileIndex.getInstance(project);
        contextChangeAlarmAfterFileClosed = AlarmFactory.getInstance().create();
    }


    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {

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
        caretListener.cancelAllCaretPositionChangedRequests();

        //see comment in fileClosed before calling updateContextAfterFileClosed
        // if we're here then we can cancel contextChangeAlarmAfterFileClosed
        contextChangeAlarmAfterFileClosed.cancelAllRequests();


        var newFile = editorManagerEvent.getNewFile();

        //ignore non supported files. newFile may be null when the last editor is closed.
        //A relevant file is a source file that is supported by one of the language services. and also a language
        // where the language plugin is intellij platform plugin. this is mainly meant to distinguish from C# on Rider,
        // this listener should not handle C# files.
        // usually this listener will not be installed on Rider unless another language plugin is installed on Rider,
        // for example the python plugin can be installed on rider.

        //this code also follows file opened events. there is a dedicated file opened event but its invoked after
        // selectionChanged and that causes some problems in managing the events. using selectionChanged to decide when
        // a new file is opened proves to be more reliable.

        if (newFile != null && isRelevantFile(newFile)) {

            PsiFile psiFile = PsiManager.getInstance(project).findFile(newFile);
            var selectedTextEditor = fileEditorManager.getSelectedTextEditor();

            if (psiFile != null && selectedTextEditor != null) {
                LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                var documentInfo = maybeSupportedFileOpened(languageService, psiFile);
                MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, selectedTextEditor.getCaretModel().getOffset());
                caretListener.maybeAddCaretListener(selectedTextEditor, newFile);
                documentChangeListener.maybeAddDocumentListener(selectedTextEditor, psiFile, languageService);

                //if documentInfo is not null then its a new file that was opened, in that case the documentInfo needs
                // to be added to documentInfoService and then call contextChanged , otherwise only call contextChanged.
                if (documentInfo != null) {
                    Backgroundable.ensureBackground(project, "File opened", () -> {
                        //documentInfoService will add the discovery code objects, load backend data and call some event
                        //so that code lens will be installed.
                        //if it's a new file that was opened then contextChanged must run after
                        // documentInfoService.addCodeObjects is finished
                        //enrichDocumentInfo is meant mainly to discover spans. the DocumentInfoIndex can
                        // not discover spans because there is no reference resolving during file based index.
                        languageService.enrichDocumentInfo(documentInfo,psiFile);
                        documentInfoService.addCodeObjects(psiFile, documentInfo);
                        caretContextService.contextChanged(methodUnderCaret);
                    });
                } else {
                    caretContextService.contextChanged(methodUnderCaret);
                }

            } else {
                caretContextService.contextEmptyNonSupportedFile(newFile.getPath());
            }

        } else if (newFile != null) {
            caretContextService.contextEmptyNonSupportedFile(newFile.getPath());
        } else {
            caretContextService.contextEmpty();
        }
    }


    @Nullable
    private DocumentInfo maybeSupportedFileOpened(@NotNull LanguageService languageService, @NotNull PsiFile psiFile) {

        //if documentInfoService already contains this PsiFile then this document was already opened before
        if (documentInfoService.contains(psiFile)) {
            return null;
        }

        //this is actually a test if this is a supported file type
        if (!languageService.isIndexedLanguage()) {
            return null;
        }

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
            documentInfo = languageService.buildDocumentInfo(psiFile);
        }

        if (documentInfo == null) {
            Log.log(LOGGER::error, "Could not find DocumentInfo for file {}", psiFile.getVirtualFile());
            throw new DocumentInfoIndexNotFoundException("Could not find DocumentInfo index for " + psiFile.getVirtualFile());
        }
        Log.log(LOGGER::debug, "Found DocumentInfo index for {},'{}'", psiFile.getVirtualFile(), documentInfo);

        return documentInfo;

    }


    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        var selectedEditor = source.getSelectedEditor();
        Log.log(LOGGER::debug, "fileClosed: file:{}, selected editor: {}", file, selectedEditor);
        caretListener.removeCaretListener(file);
        documentChangeListener.removeDocumentListener(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        //don't need to test if it's a supported file, if document info exists it will be removed.
        documentInfoService.removeDocumentInfo(psiFile);

        //sometimes, can't say why, when a tab is closed and another tab becomes visible, selectionChanged is not called
        // until the tab is clicked. fileClosed is always called. most of the time it works ok, but sometimes not.
        // in that case our plugin context keeps showing the closed tab info.
        // updating the context here with the selected editor file when a file is closed solves it. but then if
        // selectionChanged is called, which happens most of the time, then we will update the context twice.
        // so updateContextAfterFileClosed will add the contextChanged request in an Alarm with a delay of 200 millis,
        // if selectionChanged is called right after that it will cancel the Alarm and hopefully we don't update twice.
        // worst case is that sometimes there will be a delay of 200 millis in updating the context, which as said usually
        // it works ok.
        updateContextAfterFileClosed(selectedEditor, source);
    }


    private void updateContextAfterFileClosed(FileEditor selectedEditor, @NotNull FileEditorManager fileEditorManager) {
        contextChangeAlarmAfterFileClosed.cancelAllRequests();
        if (selectedEditor != null) {
            var selectedFile = selectedEditor.getFile();
            if (isRelevantFile(selectedFile)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
                var selectedTextEditor = fileEditorManager.getSelectedTextEditor();
                if (psiFile != null && selectedTextEditor != null) {
                    LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
                    MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, selectedTextEditor.getCaretModel().getOffset());
                    contextChangeAlarmAfterFileClosed.addRequest(() -> caretContextService.contextChanged(methodUnderCaret), 200);
                } else {
                    contextChangeAlarmAfterFileClosed.addRequest(() -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200);
                }
            } else {
                contextChangeAlarmAfterFileClosed.addRequest(() -> caretContextService.contextEmptyNonSupportedFile(selectedFile.getPath()), 200);
            }
        } else {
            contextChangeAlarmAfterFileClosed.addRequest(caretContextService::contextEmpty, 200);
        }
    }


    private boolean isRelevantFile(VirtualFile file) {
        //if file is not writable it is not supported even if it's a language we support, usually when we open vcs files.
        return file.isWritable() &&
                isSupportedFile(file) &&
                !projectFileIndex.isInLibrary(file) &&
                !projectFileIndex.isInTestSourceContent(file) &&
                !DocumentInfoIndex.namesToExclude.contains(file.getName());
    }


    private boolean isSupportedFile(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        return languageService.isIntellijPlatformPluginLanguage();
    }


}
