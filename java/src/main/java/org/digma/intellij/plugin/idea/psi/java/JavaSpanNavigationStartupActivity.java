package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class JavaSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        var javaSpanNavigationProvider = project.getService(JavaSpanNavigationProvider.class);
        javaSpanNavigationProvider.buildSpanNavigation();

        var javaEndpointNavigationProvider = project.getService(JavaEndpointNavigationProvider.class);
        javaEndpointNavigationProvider.buildEndpointNavigation();
    }

}
