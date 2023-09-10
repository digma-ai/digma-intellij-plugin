package org.digma.intellij.plugin.psi.python;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.common.Backgroundable;
import org.jetbrains.annotations.NotNull;

public class PythonSpanNavigationStartupActivity implements StartupActivity {


    @Override
    public void runActivity(@NotNull Project project) {

        Backgroundable.executeOnPooledThread(() -> {
            PythonSpanNavigationProvider pythonSpanNavigationProvider = PythonSpanNavigationProvider.getInstance(project);
            pythonSpanNavigationProvider.buildSpanNavigation();
        });

    }
}
