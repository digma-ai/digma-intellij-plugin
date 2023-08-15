package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.project.Project;

public class JaxrsJavaxFramework extends AbstractJaxrsFramework {

    public JaxrsJavaxFramework(Project project) {
        super(project);
    }

    @Override
    String getJaxRsPackageName() {
        return "javax.ws.rs";
    }
}
