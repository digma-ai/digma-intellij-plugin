package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DumbAwareNotifier {
    private static final Logger LOGGER = Logger.getInstance(DumbAwareNotifier.class);
    private final List<Runnable> runnableList;

    public DumbAwareNotifier() {
        this.runnableList = new ArrayList<>();
    }

    public static DumbAwareNotifier getInstance(Project project) {
        Log.test(LOGGER, "Getting instance of {}", DumbAwareNotifier.class.getSimpleName());
        DumbAwareNotifier service = project.getService(DumbAwareNotifier.class);
        Log.test(LOGGER, "Returning {}", DumbAwareNotifier.class.getSimpleName());
        return service;
    }

    public void whenSmart(@NotNull Runnable runnable) {
        runnableList.add(runnable);
    }

    void trigger() {
        Log.log(LOGGER::debug, "runActivity event is triggered");
        for (Runnable runnable : runnableList) {
            runnable.run();
        }
    }
}

class DumbAwareStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        DumbAwareNotifier.getInstance(project).trigger();
    }
}
