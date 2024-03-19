package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

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
        if (ApplicationManager.getApplication().isDispatchThread()) {
            EDTAccessException edtAccessException = new EDTAccessException("Must not run on EDT");
            LOGGER.error("Must not run on EDT", edtAccessException);
            ErrorReporter.getInstance().reportInternalFatalError("assertNonDispatchThread", edtAccessException, Collections.emptyMap());
            throw edtAccessException;
        }
    }



    public static boolean isEdt() {
        return ApplicationManager.getApplication().isDispatchThread();
    }

    public static void invokeAndWait(@NotNull Runnable task) {
        ApplicationManager.getApplication().invokeAndWait(task);
    }


    public static class EDTAccessException extends RuntimeException {
        public EDTAccessException(String message) {
            super(message);
        }
    }

}
