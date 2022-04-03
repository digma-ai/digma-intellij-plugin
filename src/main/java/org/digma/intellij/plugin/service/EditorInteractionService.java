package org.digma.intellij.plugin.service;

import com.intellij.openapi.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import org.digma.intellij.plugin.listener.*;
import org.digma.intellij.plugin.log.*;
import org.digma.intellij.plugin.psi.*;
import org.digma.intellij.plugin.toolwindow.*;

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
            }else{
                Log.log(LOGGER::debug, "psi element {} is not in a method , emptying tool window", psiElement);
                toolWindowContent.empty();
            }
        }
    }




    @Override
    public void dispose() {
        Log.log(LOGGER::debug,"disposing..");
        editorListener.stop();
    }


    public void init(ToolWindowContent toolWindowContent, Project project) {
        this.toolWindowContent = toolWindowContent;
        this.editorListener = new EditorListener(project,this);
        editorListener.start();
        psiNavigator = new PsiNavigator(project);
    }
}
