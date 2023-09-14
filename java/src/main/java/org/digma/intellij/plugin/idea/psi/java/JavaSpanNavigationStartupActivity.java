package org.digma.intellij.plugin.idea.psi.java;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.jetbrains.annotations.NotNull;

public class JavaSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        Backgroundable.executeOnPooledThread(() -> {
            try {
                var javaSpanNavigationProvider = JavaSpanNavigationProvider.getInstance(project);
                javaSpanNavigationProvider.buildSpanNavigation();

                var javaEndpointNavigationProvider = JavaEndpointNavigationProvider.getInstance(project);
                javaEndpointNavigationProvider.buildEndpointNavigation();
            } catch (Exception e) {
                ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationStartupActivity.runActivity", e);
            }
        });
    }

}
