package org.digma.intellij.plugin.insights;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.digma.intellij.plugin.ui.service.InsightsService;

import javax.swing.*;
import java.awt.*;

import static org.digma.intellij.plugin.ui.list.ListCommonKt.listBackground;

public class InsightsReactPanel extends JPanel {


    public InsightsReactPanel(Project project) {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());
        add(InsightsService.getInstance(project).getComponent());
        setBackground(listBackground());
    }

}
