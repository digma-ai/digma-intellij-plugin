package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.jetbrains.annotations.NotNull;

public class PythonSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        Backgroundable.executeOnPooledThread(() -> {
            try {
                PythonSpanNavigationProvider pythonSpanNavigationProvider = PythonSpanNavigationProvider.getInstance(project);
                pythonSpanNavigationProvider.buildSpanNavigation();
            } catch (Exception e) {
                ErrorReporter.getInstance().reportError("PythonSpanNavigationStartupActivity.runActivity", e);
            }
        });

    }
}
