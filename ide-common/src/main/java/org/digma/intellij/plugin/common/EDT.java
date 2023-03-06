package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;

public class EDT {

    public static void ensureEDT(Runnable task) {

        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            ApplicationManager.getApplication().invokeLater(task);
        }
    }

}
