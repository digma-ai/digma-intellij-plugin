package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.model.discovery.EndpointFramework;

public class JaxrsJakartaFramework extends AbstractJaxrsFramework {

    public JaxrsJakartaFramework(Project project) {
        super(project);
    }

    @Override
    String getJaxRsPackageName() {
        return "jakarta.ws.rs";
    }

    @Override
    protected EndpointFramework getFramework() {
        return EndpointFramework.JaxrsJakarta;
    }
}
