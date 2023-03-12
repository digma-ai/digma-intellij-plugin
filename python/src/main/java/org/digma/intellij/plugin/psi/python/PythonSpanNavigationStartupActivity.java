package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public class PythonSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        PythonSpanNavigationProvider pythonSpanNavigationProvider = project.getService(PythonSpanNavigationProvider.class);

        pythonSpanNavigationProvider.buildSpanNavigation();

    }
}
