package org.digma.intellij.plugin.troubleshooting;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.digma.intellij.plugin.ui.panels.DisposablePanel;

import java.awt.*;

import static org.digma.intellij.plugin.ui.list.ListCommonKt.listBackground;

public class TroubleshootingPanel extends DisposablePanel {


    private final Project project;

    public TroubleshootingPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(TroubleshootingService.getInstance(project).getComponent());
        setBackground(listBackground());
    }

    @Override
    public void dispose() {
        TroubleshootingService.getInstance(project).disposeBrowser();
    }
}
