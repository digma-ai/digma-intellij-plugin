package org.digma.intellij.plugin.idea.psi.discovery.endpoint;

import com.intellij.openapi.project.Project;

public class JaxrsJakartaFramework extends AbstractJaxrsFramework {

    public JaxrsJakartaFramework(Project project) {
        super(project);
    }

    @Override
    String getJaxRsPackageName() {
        return "jakarta.ws.rs";
    }
}
