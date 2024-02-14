package org.digma.intellij.plugin.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.*;
import org.digma.intellij.plugin.errorreporting.*;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.progress.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.*;

public class BuildDocumentInfoProcessContext extends ProcessContext {

    private static final Logger LOGGER = Logger.getInstance(BuildDocumentInfoProcessContext.class);

    public BuildDocumentInfoProcessContext(@NotNull ProgressIndicator indicator) {
        super(indicator);
    }


    //this method should not be used a lot , it may throw ProcessCanceledException or other exceptions and will not retry
    public static <T> T buildDocumentInfoUnderProcessOnCurrentThreadNoRetry(Function<ProgressIndicator, T> buildFunction) {
        var indicator = new EmptyProgressIndicator();
        return ProgressManager.getInstance().runProcess(() -> buildFunction.apply(indicator), indicator);
    }


    public static void buildDocumentInfoUnderProcess(@NotNull Project project, Consumer<ProgressIndicator> buildTask) {
        DumbService.getInstance(project).waitForSmartMode();

        var workTask = new java.util.function.Consumer<ProgressIndicator>() {

            @Override
            public void accept(ProgressIndicator progressIndicator) {
                buildTask.accept(progressIndicator);
            }
        };


        var onErrorTask = new java.util.function.Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                ErrorReporter.getInstance().reportError(
                        "DocumentChangeListener.buildDocumentInfoUnderProcess.onError", throwable,
                        Collections.singletonMap(ErrorReporterKt.SEVERITY_PROP_NAME, ErrorReporterKt.SEVERITY_MEDIUM_TRY_FIX)
                );
            }
        };

        var onPCETask = new java.util.function.Consumer<ProcessCanceledException>() {
            @Override
            public void accept(ProcessCanceledException e) {
                ErrorReporter.getInstance().reportError(
                        "DocumentChangeListener.buildDocumentInfoUnderProcess.onPCE", e,
                        Collections.singletonMap(ErrorReporterKt.SEVERITY_PROP_NAME, ErrorReporterKt.SEVERITY_MEDIUM_TRY_FIX)
                );
            }
        };

        var onFinish = new java.util.function.Consumer<RetryableTask>() {

            @Override
            public void accept(RetryableTask task) {
                var hadProgressErrors = task.getError() != null;
                var hadPCE = task.getProcessCanceledException() != null;
                var success = task.isCompletedSuccessfully();

                if (success) {
                    Log.log(LOGGER::info, "buildDocumentInfoUnderProcess completed successfully");
                } else {
                    if (hadProgressErrors) {
                        Log.log(LOGGER::info, "buildDocumentInfoUnderProcess completed with errors");
                    } else if (hadPCE && task.isExhausted()) {
                        Log.log(LOGGER::info, "buildDocumentInfoUnderProcess process retry exhausted");
                    } else if (hadPCE && task.isStoppedBeforeExhausted()) {
                        Log.log(LOGGER::info, "buildDocumentInfoUnderProcess completed before exhausted");
                    } else {
                        Log.log(LOGGER::info, "buildDocumentInfoUnderProcess completed abnormally");
                    }

                    //if no success can schedule a retry in some seconds
                    //schedule(searchScopeProvider, origin, TimeUnit.MINUTES.toMillis(2L), retry + 1)
                }

                // it possible to make the ProcessContext visible here and if there were errors
                //schedule another run soon.
                //but building DocumentInfo should be a fast process and should not be retries too much,

            }
        };


        //running buildDocumentInfo is a must because different sub systems must run in progress.
        //the RetryableTask will retry only in case of ProcessCanceledException or an error that was not
        //caught.

        //even on success of the process there may be errors on the ProcessContext because
        //the different utilities catch errors and continue , but the errors are recorded on
        //the ProcessContext.

        //buildDocumentInfo process should finish quickly and can not be retried too many times.
        //only few times with a short delay.

        var task = new RetryableTask.Invisible(
                project,
                "Digma span navigation - $origin:$retry",
                workTask,
                null,
                null,
                onErrorTask,
                onPCETask,
                onFinish,
                3,
                500L);

        task.setReuseCurrentThread(true);
        task.runInBackground();

    }
}
