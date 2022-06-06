package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RiderDocumentInfoConsumer implements DocumentInfoChanged , Disposable {

    private static final Logger LOGGER = Logger.getInstance(RiderDocumentInfoConsumer.class);

    private final Project project;

    private final CodeObjectHost codeObjectHost;
    private final CodeLensProvider codeLensProvider;


    public RiderDocumentInfoConsumer(@NotNull Project project) {
        this.project = project;
        codeObjectHost = project.getService(CodeObjectHost.class);
        codeLensProvider = project.getService(CodeLensProvider.class);

        project.getMessageBus().connect().subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,this);
    }


    @Override
    public void documentInfoChanged(@NotNull PsiFile psiFile) {
        Log.log(LOGGER::debug, "Got documentInfoChanged for {}",psiFile.getVirtualFile());
        List<CodeLens> codeLens = codeLensProvider.provideCodeLens(psiFile);
        Log.log(LOGGER::debug, "Got codeLens for {}: {}",psiFile.getVirtualFile(),codeLens);
        codeObjectHost.installCodeLens(psiFile,codeLens);
    }

    @Override
    public void dispose() {
        //maybe disconnect message bus? should be disposed automatically when the project is closed
    }
}
