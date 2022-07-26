package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RiderDocumentInfoConsumer extends LifetimedProjectComponent implements DocumentInfoChanged , Disposable {

    private static final Logger LOGGER = Logger.getInstance(RiderDocumentInfoConsumer.class);

    private final CodeObjectHost codeObjectHost;
    private final CodeLensProvider codeLensProvider;
    private final MessageBusConnection messageBusConnection;


    public RiderDocumentInfoConsumer(@NotNull Project project) {
        super(project);
        codeObjectHost = project.getService(CodeObjectHost.class);
        codeLensProvider = project.getService(CodeLensProvider.class);
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,this);
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
        super.dispose();
        messageBusConnection.dispose();
    }
}
