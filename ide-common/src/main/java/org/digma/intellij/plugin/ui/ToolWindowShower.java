package org.digma.intellij.plugin.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

public class ToolWindowShower {

    private static final Logger LOGGER = Logger.getInstance(ToolWindowShower.class);
    private Content mainContent;
    private Content installationWizardContent;


    public static ToolWindowShower getInstance(@NotNull Project project) {
        return project.getService(ToolWindowShower.class);
    }

    private final Project project;

    private ToolWindow toolWindow;

    public ToolWindowShower(Project project) {
        this.project = project;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    public void setMainContent(Content mainContent) {
        this.mainContent = mainContent;
    }

    public void setInstallationWizardContent(Content installationWizardContent) {
        this.installationWizardContent = installationWizardContent;
    }

    public void displayMainSidePaneWindowPanel(){
        setToolWindowContent(mainContent);
        showToolWindow();
    }

    public void displayInstallationWizard(){
        setToolWindowContent(installationWizardContent);
        showToolWindow();
    }

    public void setToolWindowContent(Content newContent){
        Content currentContent = toolWindow.getContentManager().getContent(0);
        if(currentContent != null){
            toolWindow.getContentManager().removeContent(currentContent, true);
        }

        toolWindow.getContentManager().addContent(newContent);
    }

    public void showToolWindow() {

        Log.log(LOGGER::debug, "showToolWindow invoked");

        if (toolWindow != null) {
            Log.log(LOGGER::debug, "Got reference to tool window, showing..");
            show(toolWindow);
        } else {
            Log.log(LOGGER::debug, "Don't have reference to tool window, showing with ToolWindowManager..");
            ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(PluginId.TOOL_WINDOW_ID);
            if (tw != null) {
                Log.log(LOGGER::debug, "Got tool window from ToolWindowManager");
                show(tw);
            } else {
                Log.log(LOGGER::debug, "Could not find tool window");
            }
        }
    }

//    public void showToolWindowById(String id) {
//        Log.log(LOGGER::debug, "Don't have reference to tool window with id {}, showing with ToolWindowManager..", id);
//        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
//        if (tw != null) {
//            Log.log(LOGGER::debug, "Got tool window {} from ToolWindowManager",id);
//            show(tw);
//        } else {
//            Log.log(LOGGER::debug, "Could not find tool window {}",id);
//        }
//    }

    private void show(ToolWindow toolWindow) {
        if (toolWindow.isVisible()) {
            Log.log(LOGGER::debug, "Tool window is already visible");
        } else {
            Log.log(LOGGER::debug, "Calling toolWindow.show");
            toolWindow.show();
        }
    }
}
