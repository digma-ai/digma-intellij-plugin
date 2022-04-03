package org.digma.intellij.plugin.toolwindow;

import com.intellij.psi.PsiElement;

import javax.swing.*;

public class ToolWindowContent {
    private JTextArea mainTextArea;
    private JPanel contentPanel;


    public JComponent getContent() {
        return contentPanel;
    }

    public void update(PsiElement psiElement) {
        mainTextArea.setText(psiElement.getText());
    }

    public void empty() {
        mainTextArea.setText(null);
    }
}
