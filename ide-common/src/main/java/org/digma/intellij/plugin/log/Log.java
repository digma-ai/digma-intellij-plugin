package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.ui.ActivityMonitor;

import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    //todo: change all logging to print the current project
    public static void log(Consumer<String> consumer, Project project, String format, Object... args) {
        log(consumer, "Digma: Project:" + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String format, Object... args) {
        log(consumer, "Digma: " + String.format(format.replace("{}", "%s"), args));
    }

    public static void debugWithException(Logger logger,Throwable e, String format, Object... args) {
        logger.debug("Digma: " + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void warnWithException(Logger logger,Throwable e, String format, Object... args) {
        logger.warn("Digma: " + String.format(format.replace("{}", "%s"), args),e);
    }


    public static void log(Consumer<String> consumer,Project project, String msg) {
        consumer.accept("Digma: Project:" + project.getName() + ": " + msg);
    }

    public static void log(Consumer<String> consumer, String msg) {
        consumer.accept("Digma: " + msg);
    }

    public static void error(Logger logger,Project project, Exception exception, String format, Object... args) {
        var msg = String.format(format.replace("{}", "%s"), args);
        error(logger, exception, "Digma: Project:" + project.getName() + ": " + msg);
        ActivityMonitor.getInstance(project).RegisterError(exception, msg);
    }
    public static void error(Logger logger, Exception exception, String format, Object... args) {
        error(logger, exception, "Digma: " + String.format(format.replace("{}", "%s"), args));
    }

    public static void error(Logger logger, Exception exception, String msg) {
        logger.error("Digma: " + msg, exception);
    }



}
