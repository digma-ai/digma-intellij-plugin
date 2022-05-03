package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.model.CodeLens;
import org.digma.rider.protocol.CodeObjectAnalysisHost;

import java.util.List;

public class RiderDocumentInfoConsumer implements DocumentInfoChanged , Disposable {

    private final Project project;

    private final CodeObjectAnalysisHost codeObjectAnalysisHost;
    private final DocumentInfoService documentInfoService;
    private final CodeLensProvider codeLensProvider;


    public RiderDocumentInfoConsumer(Project project) {
        this.project = project;
        codeObjectAnalysisHost = project.getService(CodeObjectAnalysisHost.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        codeLensProvider = project.getService(CodeLensProvider.class);

        project.getMessageBus().connect().subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGE_TOPIC,this);
    }


    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        List<CodeLens> codeLens = codeLensProvider.provideCodeLens(psiFile);
        codeObjectAnalysisHost.installCodeLens(psiFile,codeLens);
    }

    @Override
    public void dispose() {
        //maybe disconnect message bus
    }
}
