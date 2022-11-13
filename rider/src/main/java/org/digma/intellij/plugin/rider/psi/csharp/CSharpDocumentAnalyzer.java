package org.digma.intellij.plugin.rider.psi.csharp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.document.DocumentInfoNotFoundException;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

//todo: its not an Analyzer anymore, its more a 'document open handler', so change the name
public class CSharpDocumentAnalyzer extends LifetimedProjectComponent {

    private final Logger LOGGER = Logger.getInstance(CSharpDocumentAnalyzer.class);

    private final CodeObjectHost codeObjectHost;
    private final DocumentInfoService documentInfoService;

    public CSharpDocumentAnalyzer(Project project) {
        super(project);
        this.codeObjectHost = project.getService(CodeObjectHost.class);
        this.documentInfoService = project.getService(DocumentInfoService.class);
    }

    public void analyzeDocument(@NotNull PsiFile psiFile) {

        Backgroundable.ensureBackground(getProject(), "Digma: analyzeDocument", () -> {
            DocumentInfo documentInfo = codeObjectHost.getDocument(psiFile);
            if (documentInfo == null) {
                Log.log(LOGGER::error, "Could not find document for psi file {}", psiFile.getVirtualFile());
                throw new DocumentInfoNotFoundException("Could not find document for psi file " + psiFile.getVirtualFile());
            }
            Log.log(LOGGER::debug, "Found document for {},{}", psiFile.getVirtualFile(), documentInfo);
            documentInfoService.addCodeObjects(psiFile, documentInfo);
        });
    }


}
