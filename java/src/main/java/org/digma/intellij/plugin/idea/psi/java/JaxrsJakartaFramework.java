package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.project.Project;

public class JaxrsJakartaFramework extends AbsJaxrsFramework {

    public JaxrsJakartaFramework(Project project) {
        super(project);
    }

    @Override
    String getJaxRsPackageName() {
        return "jakarta.ws.rs";
    }
}
