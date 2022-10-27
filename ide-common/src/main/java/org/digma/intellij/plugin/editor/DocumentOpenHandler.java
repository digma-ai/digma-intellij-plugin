package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.indexing.FileBasedIndex;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.index.DocumentInfoIndex;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Listens to document open events , loads DocumentInfo from the index and store it in
 * DocumentInfoService, and install code lens on the opened file.
 * Also listens to file closed events and remove DocumentInfo from DocumentInfoService.
 */
public class DocumentOpenHandler implements FileEditorManagerListener {

    private final Logger LOGGER = Logger.getInstance(DocumentOpenHandler.class);

    private final Project project;

    private final DocumentInfoService documentInfoService;
    private final LanguageServiceLocator languageServiceLocator;

    public DocumentOpenHandler(Project project) {
        this.project = project;
        documentInfoService = project.getService(DocumentInfoService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);

//        PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
//            @Override
//            public void beforeChildAddition(@NotNull PsiTreeChangeEvent event) {
//                super.beforeChildAddition(event);
//            }
//
//            @Override
//            public void beforeChildRemoval(@NotNull PsiTreeChangeEvent event) {
//                super.beforeChildRemoval(event);
//            }
//
//            @Override
//            public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {
//                super.beforeChildReplacement(event);
//            }
//
//            @Override
//            public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {
//                super.beforeChildMovement(event);
//            }
//
//            @Override
//            public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {
//                super.beforeChildrenChange(event);
//            }
//
//            @Override
//            public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {
//                super.beforePropertyChange(event);
//            }
//
//            @Override
//            public void childAdded(@NotNull PsiTreeChangeEvent event) {
//                super.childAdded(event);
//            }
//
//            @Override
//            public void childRemoved(@NotNull PsiTreeChangeEvent event) {
//                super.childRemoved(event);
//            }
//
//            @Override
//            public void childReplaced(@NotNull PsiTreeChangeEvent event) {
//                super.childReplaced(event);
//            }
//
//            @Override
//            public void childMoved(@NotNull PsiTreeChangeEvent event) {
//                super.childMoved(event);
//            }
//
//            @Override
//            public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
//                super.childrenChanged(event);
//            }
//
//            @Override
//            public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
//                super.propertyChanged(event);
//            }
//        });
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

        //todo: check source.runWhenLoaded to execute the code when the editor is fully loaded
        //todo: consider dumb mode on startup
        //todo: check if file is actually a source file and not library source and not test file with ProjectFileIndex

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        //this is actually a test if this is a supported file type
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        if (!languageService.isIndexedLanguage()) {
            return;
        }

        Backgroundable.ensureBackground(project, "Document opened", new Runnable() {
            @Override
            public void run() {
                Map<Integer, DocumentInfo> documentInfoMap = FileBasedIndex.getInstance().getFileData(DocumentInfoIndex.DOCUMENT_INFO_INDEX_ID, file, project);
                //there is only one DocumentInfo per file in the index
                DocumentInfo documentInfo = documentInfoMap.values().stream().findFirst().orElse(null);
                //if a class has no methods then there is no DocumentInfo in the index
                if (documentInfo == null) {
                    Log.log(LOGGER::error, "Could not find document for file {}", file);
                    return;
                }
                Log.log(LOGGER::debug, "Found document for {},{}", file, documentInfo);
                //documentInfoService will add the discovery code objects, load backend data and call some event
                //so that code lens will be installed
                documentInfoService.addCodeObjects(psiFile, documentInfo);
            }
        });
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        //don't need to test if it's a supported file, if document info exists it will be removed.
        documentInfoService.removeDocumentInfo(psiFile);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        FileEditorManagerListener.super.selectionChanged(event);
    }
}
