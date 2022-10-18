package org.digma.intellij.plugin.rider.env;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.document.CodeLensProvider;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.lens.CodeLens;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;

import java.util.List;

public class RiderEnvironmentChangedListener extends LifetimedProjectComponent implements EnvironmentChanged {

    private final Logger LOGGER = Logger.getInstance(RiderEnvironmentChangedListener.class);

    private final CodeObjectHost codeObjectHost;
    private final ElementUnderCaretDetector elementUnderCaretDetector;
    private final DocumentInfoService documentInfoService;
    private final CodeLensProvider codeLensProvider;

    private final MessageBusConnection messageBusConnection;

    public RiderEnvironmentChangedListener(Project project) {
        super(project);
        codeObjectHost = project.getService(CodeObjectHost.class);
        elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        codeLensProvider = project.getService(CodeLensProvider.class);
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }

    @Override
    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged {}", newEnv);

        //this code is orchestration of what needs to be done on environmentChanged.
        //must be run in background thread to not freeze the UI.
        //when fired from the environment object it runs on background.

        //call document service to refresh all its document info from the backend
        documentInfoService.environmentChanged(newEnv);

        //install new code lens for all open documents
        documentInfoService.allKeys().forEach(psiFile -> {
            Log.log(LOGGER::debug, "Requesting code lens for {}", psiFile.getVirtualFile());
            List<CodeLens> codeLens = codeLensProvider.provideCodeLens(psiFile);
            Log.log(LOGGER::debug, "Got codeLens for {}: {}", psiFile.getVirtualFile(), codeLens);
            codeObjectHost.installCodeLens(psiFile, codeLens);
        });

        //trigger a refresh of element under current, after environment change it will cause a contextChange
        //and the UI will refresh with the new environment
        elementUnderCaretDetector.refresh();
    }

    @Override
    public void environmentsListChanged(List<String> newEnvironments) {
        //do nothing
    }

    @Override
    public void dispose() {
        super.dispose();
        messageBusConnection.dispose();
    }
}

