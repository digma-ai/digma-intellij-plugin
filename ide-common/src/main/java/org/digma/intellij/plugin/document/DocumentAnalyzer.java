package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;

public interface DocumentAnalyzer {
    void fileOpened(PsiFile psiFile);

}
