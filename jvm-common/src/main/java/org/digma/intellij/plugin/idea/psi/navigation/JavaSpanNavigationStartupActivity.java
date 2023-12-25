package org.digma.intellij.plugin.idea.psi.navigation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

public class JavaSpanNavigationStartupActivity implements StartupActivity {

    private Logger logger = Logger.getInstance(JavaSpanNavigationStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {

        Backgroundable.executeOnPooledThread(() -> {
            try {
                Log.log(logger::info, "starting navigation mapping");
                var javaSpanNavigationProvider = JavaSpanNavigationProvider.getInstance(project);
                javaSpanNavigationProvider.buildSpanNavigation();

                var javaEndpointNavigationProvider = JavaEndpointNavigationProvider.getInstance(project);
                javaEndpointNavigationProvider.buildEndpointNavigation();
                Log.log(logger::info, "navigation mapping completed successfully");
            } catch (Exception e) {
                Log.warnWithException(logger, e, "error in navigation mapping {}", e);
                ErrorReporter.getInstance().reportError(project, "JavaSpanNavigationStartupActivity.runActivity", e);
            }
        });
    }

}
