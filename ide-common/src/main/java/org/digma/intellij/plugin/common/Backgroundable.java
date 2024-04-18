package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.FutureResult;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class Backgroundable {

    private static final Logger LOGGER = Logger.getInstance(Backgroundable.class);

    private Backgroundable() {
    }


    //it's better to use ensureBackgroundWithoutReadAccess
    public static void ensureBackground(Project project, String name, Runnable task) {

        Log.log(LOGGER::trace, "Request to call task '{}'", name);

        if (EDT.isEdt()) {
            Log.log(LOGGER::trace, "Executing task '{}' in background thread", name);
            new Task.Backgroundable(project, name) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    runWithErrorReporting(project, name, task);
                }
            }.queue();
        } else {
            Log.log(LOGGER::trace, "Executing task '{}' in current thread", name);
            runWithErrorReporting(project, name, task);
        }
    }


    public static void ensureBackgroundWithoutReadAccess(Project project, String name, Runnable task) {

        Log.log(LOGGER::trace, "Request to call task '{}'", name);

        if (EDT.isEdt() || ReadActions.isReadAccessAllowed()) {
            Log.log(LOGGER::trace, "Executing task '{}' in background thread", name);
            new Task.Backgroundable(project, name) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    runWithErrorReporting(project, name, task);
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
                runWithErrorReporting(project, name, task);
            }
        }.queue();

    }

    //it's better to use ensurePooledThreadWithoutReadAccess
    public static void ensurePooledThread(@NotNull Runnable action) {
        if (EDT.isEdt()) {
            executeOnPooledThread(action);
        } else {
            runWithErrorReporting(action);
        }
    }

    public static void ensurePooledThreadWithoutReadAccess(@NotNull Runnable action) {
        if (EDT.isEdt() || ReadActions.isReadAccessAllowed()) {
            executeOnPooledThread(action);
        } else {
            runWithErrorReporting(action);
        }
    }

    public static <T> Future<T> ensurePooledThreadWithoutReadAccess(@NotNull Callable<T> action) {
        if (EDT.isEdt() || ReadActions.isReadAccessAllowed()) {
            return executeOnPooledThread(action);
        } else {
            return runWithErrorReporting(action);
        }
    }


    //can use the future to wait for the thread to finish if necessary
    public static Future<?> executeOnPooledThread(@NotNull Runnable action) {
        return ApplicationManager.getApplication().executeOnPooledThread(() -> runWithErrorReporting(action));
    }


    //when calling this method make sure to catch errors when calling future.get
    public static <T> Future<T> executeOnPooledThread(@NotNull Callable<T> action) {
        return ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                return action.call();
            } catch (Throwable e) {
                Log.warnWithException(LOGGER, e, "Exception in action");
                ErrorReporter.getInstance().reportError("executeOnPooledThread", e);
                throw e;
            }
        });
    }


    private static void runWithErrorReporting(Project project, String name, Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in task {}", name);
            ErrorReporter.getInstance().reportError(project, "Backgroundable.runWithErrorReporting(Project,Runnable)" + name, e);
        }
    }

    private static void runWithErrorReporting(Runnable task) {
        try {
            task.run();
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "Exception in action");
            ErrorReporter.getInstance().reportError("Backgroundable.runWithErrorReporting(Runnable)", e);
        }
    }

    private static <T> Future<T> runWithErrorReporting(@NotNull Callable<T> action) {

        try {
            return new FutureResult<>(action.call());
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in action");
            ErrorReporter.getInstance().reportError("Backgroundable.runWithErrorReporting(Callable)", e);
            return new FutureResult<>();
        }
    }

}
