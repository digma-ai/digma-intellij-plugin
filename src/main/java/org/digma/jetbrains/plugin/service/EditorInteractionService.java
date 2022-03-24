package org.digma.jetbrains.plugin.service;

import com.intellij.openapi.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import org.digma.jetbrains.plugin.listener.*;
import org.digma.jetbrains.plugin.log.*;
import org.digma.jetbrains.plugin.psi.*;
import org.digma.jetbrains.plugin.toolwindow.*;

import java.util.*;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done in the EDT.
 */
public class EditorInteractionService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorInteractionService.class);

    /**
     * EditorListener registers CaretListeners on editors, those listeners need to be removed from
     * the editor on editor close and on ide shutdown. the recommanded jetbrains way is to register
     * listeners with a parent disposable. EditorListener registers a parent disposable for each
     * installed listener, on fileClosed the parent disposable should be disposed so that the listener
     * is removed,EditorInteractionService keeps those parent disposables and disposes them when asked to
     * or on its own dispose call.
     */
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();

    private boolean disposed = false;
    private EditorListener editorListener;
    private ToolWindowContent toolWindowContent;
    private Project project;

    public static EditorInteractionService getInstance(Project project) {
        return project.getService(EditorInteractionService.class);
    }

    public void setToolWindowContent(ToolWindowContent toolWindowContent) {
        this.toolWindowContent = toolWindowContent;
    }

    public void updateViewContent(PsiElement psiElement) {
        if (toolWindowContent != null) {
            if (PsiNavigator.isMethod(psiElement)) {
                Log.log(LOGGER::debug, "got psi method for element {}", psiElement);
                toolWindowContent.update(PsiNavigator.getMethod(psiElement));
            }else{
                Log.log(LOGGER::debug, "psi element {} is not a method , emptying tool window", psiElement);
                toolWindowContent.empty();
            }
        }
    }


    public void registerDisposable(VirtualFile file, Disposable parentDisposable) {
        disposables.put(file, parentDisposable);
    }

    public boolean containsDisposable(VirtualFile file) {
        return disposables.containsKey(file);
    }

    public void dispose(VirtualFile file){
        Disposable disposable = disposables.remove(file);
        if (disposable != null) {
            Log.log(LOGGER::debug,"disposing for file:{}",file);
            Disposer.dispose(disposable);
        }
    }


    @Override
    public void dispose() {
        Log.log(LOGGER::debug,"disposing..");
        disposed = true;
        disposables.values().forEach(Disposer::dispose);
        //todo: maybe change to messaging service because removeFileEditorManagerListener is deprected
        FileEditorManager.getInstance(project).removeFileEditorManagerListener(editorListener);
    }


    public void registerFileEditorManagerListenerToStop(EditorListener editorListener) {
        this.editorListener = editorListener;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
