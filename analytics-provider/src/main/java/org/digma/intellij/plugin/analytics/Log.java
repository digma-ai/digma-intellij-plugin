package org.digma.intellij.plugin.analytics;

import java.util.logging.Logger;

public class Log {

    public static void test(Logger logger, String format, Object... args) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String callingMethodName = stackTraceElements[2].getMethodName();
        String threadName = Thread.currentThread().getName();
        String header = callingMethodName + " - " + threadName;
        logger.warning(header + " - " + String.format(format.replace("{}", "%s"), args));
    }

}
