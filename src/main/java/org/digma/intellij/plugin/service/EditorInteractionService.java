package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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
                toolWindowContent.empty();
            }
        }
    }


    @Override
    public void dispose() {
        Log.log(LOGGER::debug, "disposing..");
        editorListener.stop();
    }


    public void setToolWindowContent(@NotNull ToolWindowContent toolWindowContent) {
        this.toolWindowContent = toolWindowContent;
    }

    public void start(@NotNull Project project) {
        Log.log(LOGGER::debug, "starting..");
        this.editorListener = new EditorListener(project, this);
        editorListener.start();
        psiNavigator = new PsiNavigator(project);
    }
}
