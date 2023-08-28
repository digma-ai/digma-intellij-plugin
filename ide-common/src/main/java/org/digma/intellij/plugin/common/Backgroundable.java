package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Backgroundable {

    private static final Logger LOGGER = Logger.getInstance(Backgroundable.class);

    private Backgroundable() {
    }


    public static void ensureBackground(Project project, String name, Runnable task) {

        Log.log(LOGGER::trace, "Request to call task '{}'", name);

        if (EDT.isEdt()) {
            Log.log(LOGGER::trace, "Executing task '{}' in background thread", name);
            new Task.Backgroundable(project, name) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    runWithErrorReporting(project,name,task);
                }
            }.queue();
        } else {
            Log.log(LOGGER::trace, "Executing task '{}' in current thread", name);
            runWithErrorReporting(project, name, task);
        }
    }




    public static void runInNewBackgroundThread(Project project, String name, Runnable task) {

        new Task.Backgroundable(project, name) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                runWithErrorReporting(project,name,task);
            }
        }.queue();

    }

    public static void ensurePooledThread(@NotNull Runnable action){
        if (EDT.isEdt()) {
            executeOnPooledThread(action);
        } else {
            runWithErrorReporting(action);
        }
    }


    public static Future<?> executeOnPooledThread(@NotNull Runnable action) {
        return ApplicationManager.getApplication().executeOnPooledThread(() -> runWithErrorReporting(action));
    }

    public static <T> Future<T> executeOnPooledThread(@NotNull Callable<T> action) {
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                return action.call();
            }catch (Exception e){
                Log.warnWithException(LOGGER, e, "Exception in action");
                ErrorReporter.getInstance().reportError(null, "executeOnPooledThread", e);
                throw e;
            }
        });
    }


    private static void runWithErrorReporting(Project project, String name, Runnable task) {
        try{
            task.run();
        }catch (Exception e){
            Log.warnWithException(LOGGER, e, "Exception in task {}",name);
            ErrorReporter.getInstance().reportError(project, "Exception in task "+name, e);
        }
    }

    private static void runWithErrorReporting(Runnable task) {
        try{
            task.run();
        }catch (Exception e){
            Log.warnWithException(LOGGER, e, "Exception in action");
            ErrorReporter.getInstance().reportError(null, "Backgroundable.runWithErrorReporting", e);
        }
    }


}
