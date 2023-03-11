package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class Backgroundable {

    private static final Logger LOGGER = Logger.getInstance(Backgroundable.class);

    private Backgroundable() {
    }


    public static void ensureBackground(Project project, String name, Runnable task) {

        Log.log(LOGGER::debug, "Request to call task '{}'", name);

        if (SwingUtilities.isEventDispatchThread()) {
            Log.log(LOGGER::debug, "Executing task '{}' in background thread", name);
            new Task.Backgroundable(project, name) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    task.run();
                }
            }.queue();
        } else {
            Log.log(LOGGER::debug, "Executing task '{}' in current thread", name);
            task.run();
        }
    }
}
