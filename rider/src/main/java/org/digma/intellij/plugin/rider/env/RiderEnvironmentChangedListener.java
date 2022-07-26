package org.digma.intellij.plugin.rider.env;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.protocol.DocumentCodeObjectsListener;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;

public class RiderEnvironmentChangedListener extends LifetimedProjectComponent implements EnvironmentChanged {

    private final Logger LOGGER = Logger.getInstance(RiderEnvironmentChangedListener.class);

    private final CodeObjectHost codeObjectHost;
    private final DocumentCodeObjectsListener documentCodeObjectsListener;
    private final CaretContextService caretContextService;
    private final DocumentInfoService documentInfoService;
    private final SummaryViewService summaryViewService;
    private final MessageBusConnection messageBusConnection;

    public RiderEnvironmentChangedListener(Project project) {
        super(project);
        codeObjectHost = project.getService(CodeObjectHost.class);
        documentCodeObjectsListener = project.getService(DocumentCodeObjectsListener.class);
        caretContextService = project.getService(CaretContextService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        summaryViewService = project.getService(SummaryViewService.class);
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }

    @Override
    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged {}", newEnv);

        //empty the context
        caretContextService.contextEmpty();
        //call document service to clean its maps
        documentInfoService.environmentChanged(newEnv);
        //codeObjectHost should mainly clear code lens
        codeObjectHost.environmentChanged();
        //documentCodeObjectsListener will fire documentCodeObjectsChanged for each documents
        //in the protocol, that will cause a refresh of the code objects,summaries etc. and will eventually
        //trigger a MethodUnderCaret event
        documentCodeObjectsListener.environmentChanged();
        // summary tab affected only by env change
        summaryViewService.environmentChanged();

        //todo: maybe trigger again MethodUnderCaret event
    }

    @Override
    public void dispose() {
        super.dispose();
        messageBusConnection.dispose();
    }
}

