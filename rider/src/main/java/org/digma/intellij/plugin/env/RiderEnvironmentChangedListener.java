package org.digma.intellij.plugin.env;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.rider.protocol.CodeObjectHost;

public class RiderEnvironmentChangedListener implements EnvironmentChanged {


    private final Project project;
    private final CodeObjectHost codeObjectHost;

    public RiderEnvironmentChangedListener(Project project) {
        this.project = project;
        codeObjectHost = project.getService(CodeObjectHost.class);
        project.getMessageBus().connect().subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }

    @Override
    public void environmentChanged(String newEnv) {
        codeObjectHost.environmentChanged();
    }
}

