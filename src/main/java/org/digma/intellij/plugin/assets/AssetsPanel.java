package org.digma.intellij.plugin.assets;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

import static org.digma.intellij.plugin.ui.list.ListCommonKt.listBackground;

public class AssetsPanel extends JPanel {


    public AssetsPanel(Project project) {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty());

        var component = AssetsService.getInstance(project).getComponent();
        if (component == null) {
            component = new JLabel("JCef is not supported");
        }
        add(component);
        setBackground(listBackground());
    }

    @Override
    public Insets getInsets() {
        return JBUI.emptyInsets();
    }
}
