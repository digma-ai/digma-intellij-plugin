package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.errorreporting.*;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    public static final String DIGMA = "Digma: ";
    public static final String DIGMA_PROJECT = "Digma: Project:";


    private static final FrequentErrorDetector FREQUENT_ERROR_DETECTOR = new FrequentErrorDetector(Duration.ofMinutes(10));



    public static void log(Consumer<String> consumer, Project project, String format, Object... args) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String format, Object... args) {
        consumer.accept(DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void debugWithException(Logger logger,Throwable e, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }

        logger.debug(DIGMA + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void debugWithException(Logger logger, Project project,Throwable e, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }
        logger.debug(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void warnWithException(Logger logger,Throwable e, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }
        logger.warn(DIGMA + String.format(format.replace("{}", "%s"), args),e);
    }

    public static void warnWithException(Logger logger, Project project,Throwable e, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }
        logger.warn(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args),e);
    }


    public static void log(Consumer<String> consumer,Project project, String msg) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + msg);
    }

    public static void log(Consumer<String> consumer, String msg) {
        consumer.accept(DIGMA + msg);
    }

    public static void error(Logger logger,Project project, Exception exception, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, exception)) {
            return;
        }

        var msg = String.format(format.replace("{}", "%s"), args);
        error(logger, exception, DIGMA_PROJECT + project.getName() + ": " + msg);
        ErrorReporter.getInstance().reportError(project, "Log.error", exception);
    }
    public static void error(Logger logger, Exception exception, String format, Object... args) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, exception)) {
            return;
        }
        error(logger, exception, DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void error(Logger logger, Exception exception, String msg) {
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(msg, exception)) {
            return;
        }
        logger.error(DIGMA + msg, exception);
    }

    public static void error(Logger logger, String msg) {
        logger.error(DIGMA + msg);
    }



}
