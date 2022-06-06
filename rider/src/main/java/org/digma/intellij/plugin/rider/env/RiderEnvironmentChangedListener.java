package org.digma.intellij.plugin.rider.env;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.ui.CaretContextService;

public class RiderEnvironmentChangedListener implements EnvironmentChanged {


    private final Project project;
    private final CodeObjectHost codeObjectHost;
    private final CaretContextService caretContextService;
    private final DocumentInfoService documentInfoService;

    public RiderEnvironmentChangedListener(Project project) {
        this.project = project;
        codeObjectHost = project.getService(CodeObjectHost.class);
        caretContextService = project.getService(CaretContextService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
        project.getMessageBus().connect().subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }

    @Override
    public void environmentChanged(String newEnv) {
        caretContextService.contextEmpty();
        documentInfoService.environmentChanged(newEnv);
        codeObjectHost.environmentChanged();
    }
}

