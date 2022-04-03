package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.digma.intellij.plugin.listener.EditorListener;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.PsiNavigator;
import org.digma.intellij.plugin.toolwindow.ToolWindowContent;
import org.jetbrains.annotations.NotNull;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done in the EDT.
 */
public class EditorInteractionService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorInteractionService.class);

    private EditorListener editorListener;
    private ToolWindowContent toolWindowContent;
    private PsiNavigator psiNavigator;
    private Project project;

    public static EditorInteractionService getInstance(Project project) {
        return project.getService(EditorInteractionService.class);
    }

    public void updateViewContent(PsiElement psiElement) {
        if (toolWindowContent != null) {
            if (psiNavigator.isInMethod(psiElement)) {
                Log.log(LOGGER::debug, "got psi method for element {}", psiElement);
                toolWindowContent.update(psiNavigator.getMethod(psiElement));
            } else {
                Log.log(LOGGER::debug, "psi element {} is not in a method , emptying tool window", psiElement);
                clearViewContent();
            }
        }
    }

    public void clearViewContent() {
        if (toolWindowContent != null) {
            toolWindowContent.empty();
        }
    }

    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
        editorListener.stop();
    }


    public void start(@NotNull Project project, ToolWindowContent toolWindowContent) {
        Log.log(LOGGER::debug, "starting..");
        this.project = project;
        psiNavigator = new PsiNavigator(project);
        this.toolWindowContent = toolWindowContent;
        editorListener = new EditorListener(project, this);
        editorListener.start();
    }

    public boolean isSupportedFile(VirtualFile newFile) {
        return psiNavigator.isSupportedFile(project, newFile);
    }
}
