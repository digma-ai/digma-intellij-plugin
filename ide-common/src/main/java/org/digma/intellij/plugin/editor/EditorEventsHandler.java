package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
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

import java.util.Map;

public class EditorEventsHandler implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandler.class);

    private EditorListener editorListener;
    private final Project project;
    private final CaretContextService caretContextService;

    private final DocumentInfoService documentInfoService;

    private final LanguageServiceLocator languageServiceLocator;

    private final CaretListener caretListener;

    private final ProjectFileIndex projectFileIndex;

    private boolean initialized = false;


    public EditorEventsHandler(Project project) {
        this.project = project;
        caretContextService = project.getService(CaretContextService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        caretListener = new CaretListener(project, this);
        projectFileIndex = ProjectFileIndex.getInstance(project);
    }

//
//    @Override
//    public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file, @NotNull List<FileEditorWithProvider> editorsWithProviders) {
//        FileEditorManagerListener.super.fileOpenedSync(source, file, editorsWithProviders);
//    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        //todo: check source.runWhenLoaded to execute the code when the editor is fully loaded
        //todo: consider dumb mode on startup
        //todo: check if file is actually a source file and not library source and not test file with ProjectFileIndex


//        if (source.getSelectedTextEditor() == null){
//            return;
//        }

        if (source.getSelectedTextEditor() == null || !isRelevantFile(file)) {
            caretContextService.contextEmptyNonSupportedFile(file.getPath());
            return;
        }

        source.runWhenLoaded(source.getSelectedTextEditor(), () -> {

            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return;
            }
            //this is actually a test if this is a supported file type
            LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
            if (!languageService.isIndexedLanguage()) {
                return;
            }

            DocumentInfo documentInfo;
            try {
                Map<Integer, DocumentInfo> documentInfoMap = FileBasedIndex.getInstance().getFileData(DocumentInfoIndex.DOCUMENT_INFO_INDEX_ID, file, project);
                //there is only one DocumentInfo per file in the index
                documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);
            } catch (IndexNotReadyException e) {
                //IndexNotReadyException will be thrown on dumb mode, when indexing is still in process.
                documentInfo = languageService.buildDocumentInfo(psiFile);
            }

            if (documentInfo == null) {
                Log.log(LOGGER::error, "Could not find DocumentInfo for file {}", file);
                return;
            }
            Log.log(LOGGER::debug, "Found document for {},{}", file, documentInfo);

            Editor editor = source.getSelectedTextEditor();
            MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, editor.getCaretModel().getOffset());
            FileEditor fileEditor = source.getSelectedEditor();

            DocumentInfo finalDocumentInfo = documentInfo;
            Backgroundable.ensureBackground(project, "File opened", () -> {
                //documentInfoService will add the discovery code objects, load backend data and call some event
                //so that code lens will be installed
                documentInfoService.addCodeObjects(psiFile, finalDocumentInfo);
                if (fileEditor.getFile().equals(file)) {
                    caretContextService.contextChanged(methodUnderCaret);
                }
            });
        });


//        Backgroundable.ensureBackground(project, "Document opened", new Runnable() {
//            @Override
//            public void run() {
//                Map<Integer, DocumentInfo> documentInfoMap = FileBasedIndex.getInstance().getFileData(DocumentInfoIndex.DOCUMENT_INFO_INDEX_ID,file,project);
//                //there is only one DocumentInfo per file in the index
//                DocumentInfo documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);
//                //if a class has no methods then there is no DocumentInfo in the index
//                if (documentInfo == null) {
//                    Log.log(LOGGER::error, "Could not find document for file {}", file);
//                    return;
//                }
//                Log.log(LOGGER::debug, "Found document for {},{}", file, documentInfo);
//                //documentInfoService will add the discovery code objects, load backend data and call some event
//                //so that code lens will be installed
//                documentInfoService.addCodeObjects(psiFile, documentInfo);
//            }
//        });
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileClosed: file:{}", file);
        caretListener.removeCaretListener(file);
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        //don't need to test if it's a supported file, if document info exists it will be removed.
        documentInfoService.removeDocumentInfo(psiFile);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {
        FileEditorManager fileEditorManager = editorManagerEvent.getManager();
        Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());

        var newFile = editorManagerEvent.getNewFile();
        var editor = fileEditorManager.getSelectedTextEditor();

        //ignore non supported files. newFile may be null when the last editor is closed.
        if (newFile != null && editor != null && isRelevantFile(newFile)) {
            Log.log(LOGGER::debug, "selectionChanged: updating with file:{}", newFile);
            caretListener.maybeAddCaretListener(editor, newFile);
            updateCurrentContext(editor.getCaretModel().getOffset(), newFile);
        } else if (newFile != null) {
            caretContextService.contextEmptyNonSupportedFile(newFile.getPath());
        } else {
            caretContextService.contextEmpty();
        }
    }

    /**
     * This event will be called every time the tool window is shows after it was hidden. but we want to initialize
     * our listeners only once, and we check it with initialized flag.
     * There is no need to synchronize access to initialized flag, showing the tool window can't happen by multiple
     * threads simultaneously
     */
//    @Override
//    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
//        if (PluginId.TOOL_WINDOW_ID.equals(toolWindow.getId()) && !initialized) {
////            start();
//            initialized = true;
//        }
//    }


//    @Override
//    public void dispose() {
//        Log.log(LOGGER::debug, "disposing..");
//        editorListener.stop();
//    }


//    public void start() {
//        Log.log(LOGGER::debug, "starting..");
//        editorListener = new EditorListener(project,caretContextService, this);
//        editorListener.start();
//    }


    boolean isRelevantFile(VirtualFile file) {
        if (projectFileIndex.isInLibrary(file) ||
                projectFileIndex.isInTestSourceContent(file) ||
                !isSupportedFile(file) ||
                DocumentInfoIndex.namesToExclude.contains(file.getName())) {
            return false;
        }
        return true;
    }


    boolean isSupportedFile(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        return languageService.isIntellijPlatformPluginLanguage();
    }


//    void emptySelection() {
//        caretContextService.contextEmpty();
//    }

    void updateCurrentContext(int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        updateCurrentContext(caretOffset, psiFile);
    }

    void updateCurrentContext(int caretOffset, PsiFile psiFile) {
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
        caretContextService.contextChanged(methodUnderCaret);
    }
}
