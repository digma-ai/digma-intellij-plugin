package org.digma.intellij.plugin.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

public class EditorEventsHandlerBak implements ToolWindowManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandlerBak.class);

    private EditorListener editorListener;
    private final Project project;
    private final CaretContextService caretContextService;

    private final LanguageServiceLocator languageServiceLocator;

    private boolean initialized = false;

    public EditorEventsHandlerBak(Project project) {
        this.project = project;
        caretContextService = project.getService(CaretContextService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
    }

    /**
     * This event will be called every time the tool window is shows after it was hidden. but we want to initialize
     * our listeners only once, and we check it with initialized flag.
     * There is no need to synchronize access to initialized flag, showing the tool window can't happen by multiple
     * threads simultaneously
     */
    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (PluginId.TOOL_WINDOW_ID.equals(toolWindow.getId()) && !initialized) {
            start();
            initialized = true;
        }
    }


//    @Override
//    public void dispose() {
//        Log.log(LOGGER::debug, "disposing..");
//        editorListener.stop();
//    }


    public void start() {
        Log.log(LOGGER::debug, "starting..");
//        editorListener = new EditorListener(project,caretContextService, this);
        editorListener.start();
    }

    boolean isSupportedFile(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return false;
        }
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        return languageService.isIntellijPlatformPluginLanguage();
    }


    void emptySelection() {
        caretContextService.contextEmpty();
    }

    void updateCurrentElement(int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
        caretContextService.contextChanged(methodUnderCaret);
    }
}
