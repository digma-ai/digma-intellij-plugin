package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;

public class EDT {

    private static final Logger LOGGER = Logger.getInstance(EDT.class);

    public static void ensureEDT(Runnable task) {

        if (ApplicationManager.getApplication().isDispatchThread()) {
            runWithErrorReporting(task);
        } else {
            ApplicationManager.getApplication().invokeLater(() -> runWithErrorReporting(task));
        }
    }

    private static void runWithErrorReporting(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "Exception in EDT task");
            ErrorReporter.getInstance().reportError("EDT.runWithErrorReporting", e);
        }
    }

    public static void assertEDT(String message) {
        if (ApplicationManager.getApplication().isDispatchThread()){
            return;
        }
        //log an error here, intellij will pop up an error message. usually we don't want an error message
        // but this should be caught in development time.
        Log.log(LOGGER::error,message);
    }



    public static void assertIsDispatchThread(){
        ApplicationManager.getApplication().assertIsDispatchThread();
    }

    public static void assertNonDispatchThread(){
        //noinspection UnstableApiUsage
        ApplicationManager.getApplication().assertIsNonDispatchThread();
    }



    public static boolean isEdt() {
        return ApplicationManager.getApplication().isDispatchThread();
    }
}
