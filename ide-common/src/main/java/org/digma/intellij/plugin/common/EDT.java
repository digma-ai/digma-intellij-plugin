package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.log.Log;

public class EDT {

    private static final Logger LOGGER = Logger.getInstance(EDT.class);

    public static void ensureEDT(Runnable task) {

        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeLater(task);
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



    public static void assertNonDispatchThread(){
        //noinspection UnstableApiUsage
        ApplicationManager.getApplication().assertIsNonDispatchThread();
    }



    public static boolean isEdt() {
        return ApplicationManager.getApplication().isDispatchThread();
    }
}
