package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.posthog.ActivityMonitor;

import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    public static final String DIGMA = "Digma: ";
    public static final String DIGMA_PROJECT = "Digma: Project:";

    public static void log(Consumer<String> consumer, Project project, String format, Object... args) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String format, Object... args) {
        consumer.accept(DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void test(Consumer<String> consumer, String format, Object... args) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String callingMethodName = stackTraceElements[2].getMethodName();
        String lineNumber = String.valueOf(stackTraceElements[2].getLineNumber());
        String threadName = Thread.currentThread().getName();
        String header = String.format("%s:%s - %s", callingMethodName, lineNumber, threadName);
        consumer.accept(header + " - " + String.format(format.replace("{}", "%s"), args));
    }

    public static void debugWithException(Logger logger,Throwable e, String format, Object... args) {
        logger.debug(DIGMA + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void debugWithException(Logger logger, Project project,Throwable e, String format, Object... args) {
        logger.debug(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void warnWithException(Logger logger,Throwable e, String format, Object... args) {
        logger.warn(DIGMA + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void warnWithException(Logger logger, Project project,Throwable e, String format, Object... args) {
        logger.warn(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args),e);
    }


    public static void log(Consumer<String> consumer,Project project, String msg) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + msg);
    }

    public static void log(Consumer<String> consumer, String msg) {
        consumer.accept(DIGMA + msg);
    }

    public static void error(Logger logger,Project project, Exception exception, String format, Object... args) {
        var msg = String.format(format.replace("{}", "%s"), args);
        error(logger, exception, DIGMA_PROJECT + project.getName() + ": " + msg);
        ActivityMonitor.getInstance(project).registerError(exception, msg);
    }
    public static void error(Logger logger, Exception exception, String format, Object... args) {
        error(logger, exception, DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void error(Logger logger, Exception exception, String msg) {
        logger.error(DIGMA + msg, exception);
    }


}
