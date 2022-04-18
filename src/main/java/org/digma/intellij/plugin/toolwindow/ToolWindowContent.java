package org.digma.intellij.plugin.toolwindow;

import javax.swing.*;

public class ToolWindowContent {
    private JTextArea mainTextArea;
    private JPanel contentPanel;


    public JComponent getContent() {
        return contentPanel;
    }

    public void update(String methodInfo) {
        mainTextArea.setText(methodInfo);
    }

    public void empty() {
        mainTextArea.setText(null);
    }
}
