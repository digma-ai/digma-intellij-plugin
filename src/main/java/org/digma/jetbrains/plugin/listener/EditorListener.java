package org.digma.jetbrains.plugin.listener;

import com.intellij.openapi.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import org.digma.jetbrains.plugin.log.*;
import org.digma.jetbrains.plugin.service.*;
import org.jetbrains.annotations.*;


/**
 * Listens to FileEditorManager events and CaretEvents and updates view respectively.
 */
public class EditorListener implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorListener.class);

    private final Project project;
    private final EditorInteractionService editorInteractionService;


    public EditorListener(Project project) {
        this.project = project;
        this.editorInteractionService = EditorInteractionService.getInstance(project);
        this.editorInteractionService.registerFileEditorManagerListenerToStop(this);
    }


    //todo: fileOpened sometimes sends the wrong VirtualFile: https://intellij-support.jetbrains.com/hc/en-us/community/posts/4792428181650-wrong-file-in-FileEditorManagerListener-fileOpened
    @Override
    public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileOpened: editor:{},file:{}", fileEditorManager.getSelectedEditor(), file);
        addCaretListener(fileEditorManager);
    }


    @Override
    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileClosed: editor:{},file:{}", fileEditorManager.getSelectedEditor(), file);
        if (fileEditorManager.getSelectedEditor() != null) {
            Log.log(LOGGER::debug, "fileClosed: disposing listener for editor:{},file:{}", fileEditorManager.getSelectedEditor(), file);
            editorInteractionService.dispose(fileEditorManager.getSelectedEditor().getFile());
        }
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {
        FileEditorManager fileEditorManager = editorManagerEvent.getManager();
        Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());
        addCaretListener(fileEditorManager);
        Editor editor = fileEditorManager.getSelectedTextEditor();
        FileEditor fileEditor = fileEditorManager.getSelectedEditor();
        if (editor != null && fileEditor != null && fileEditor.getFile() != null) {
            Log.log(LOGGER::debug, "selectionChanged: updating with file:{}", fileEditor.getFile());
            updateCurrentElement(editor.getCaretModel().getOffset(), fileEditor.getFile());
        }
    }


    //todo: don't install listener on non code files. check language.
    private void addCaretListener(FileEditorManager fileEditorManager) {

        FileEditor fileEditor = fileEditorManager.getSelectedEditor();
        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor == null || fileEditor == null || !fileEditor.isValid()) {
            Log.log(LOGGER::debug, "not installing listener for {} because its is either null or not valid", editor);
            return;
        }

        VirtualFile file = fileEditor.getFile();
        if (editorInteractionService.containsDisposable(file)) {
            Log.log(LOGGER::debug, "editor:{} already has caret listener", editor);
            return;
        }

        Disposable parentDisposable = Disposer.newDisposable();
        editorInteractionService.registerDisposable(file, parentDisposable);
        Log.log(LOGGER::debug, "adding caret listener for editor:{},file:{}", editor, file);
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent caretEvent) {
                Log.log(LOGGER::debug, "caretPositionChanged for editor:{},file:{}", editor, file);
                if (caretEvent.getCaret() != null){
                    int caretOffset = caretEvent.getCaret().getOffset();
                    updateCurrentElement(caretOffset, file);
                }
            }
        }, parentDisposable);
    }


    private void updateCurrentElement(int caretOffset, @NotNull VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            if (psiElement != null) {
                Log.log(LOGGER::debug, "got psi element {}", psiElement);
                editorInteractionService.updateViewContent(psiElement);
            }
        }
    }


}
