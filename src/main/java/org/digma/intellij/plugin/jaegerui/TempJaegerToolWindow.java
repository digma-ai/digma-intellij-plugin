package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TempJaegerToolWindow implements ToolWindowFactory {


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        if (!JBCefApp.isSupported()) {
            return;
        }

        var component = JaegerUIService.getInstance(project).getBrowserComponent();
        var browserPanel = new JPanel();
        browserPanel.setLayout(new BorderLayout());
        browserPanel.add(component, BorderLayout.CENTER);
        var content = ContentFactory.getInstance().createContent(browserPanel,null,false);
        toolWindow.getContentManager().addContent(content);
    }
}
