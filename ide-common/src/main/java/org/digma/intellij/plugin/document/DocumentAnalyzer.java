package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;

public interface DocumentAnalyzer {
    void analyzeDocument(PsiFile psiFile);

}
