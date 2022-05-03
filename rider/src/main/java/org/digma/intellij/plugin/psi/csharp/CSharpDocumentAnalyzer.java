package org.digma.intellij.plugin.psi.csharp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.DocumentAnalyzer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.model.MethodInfo;
import org.digma.rider.protocol.CodeObjectAnalysisHost;

import java.util.List;

public class CSharpDocumentAnalyzer implements DocumentAnalyzer {

    private final CodeObjectAnalysisHost codeObjectAnalysisHost;
    private final DocumentInfoService documentInfoService;

    public CSharpDocumentAnalyzer(Project project) {
        this.codeObjectAnalysisHost = project.getService(CodeObjectAnalysisHost.class);
        this.documentInfoService = project.getService(DocumentInfoService.class);
    }

    @Override
    public void fileOpened(PsiFile psiFile) {
        List<MethodInfo> methodInfos = codeObjectAnalysisHost.getMethodsForFile(psiFile);
        documentInfoService.addMethodInfos(psiFile,methodInfos);
    }
}
