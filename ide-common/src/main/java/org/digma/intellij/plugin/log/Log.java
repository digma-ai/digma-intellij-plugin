package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException;
import org.digma.intellij.plugin.common.FrequencyDetector;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    public static final String DIGMA = "Digma: ";
    public static final String DIGMA_PROJECT = "Digma: Project:";
    public static final String API_LOGGER_NAME = "api.digma.org";


    private static final FrequencyDetector FREQUENT_ERROR_DETECTOR = new FrequencyDetector(Duration.ofMinutes(60));


    public static void log(Consumer<String> consumer, Project project, String format, Object... args) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }

    public static void logWithThreadName(Consumer<String> consumer, String format, Object... args) {
        consumer.accept(Thread.currentThread().getName() + ": " + DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String format, Object... args) {
        consumer.accept(DIGMA + String.format(format.replace("{}", "%s"), args));
    }

    public static void debugWithException(Logger logger, Throwable e, String format, Object... args) {

        if (!logger.isDebugEnabled()) {
            return;
        }
        //always write the message so we see it in the log,check frequency only for the exception
        logger.debug(DIGMA + String.format(format.replace("{}", "%s"), args));
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }
        logger.debug(e);
    }

    public static void debugWithException(Logger logger, Project project, Throwable e, String format, Object... args) {

        if (!logger.isDebugEnabled()) {
            return;
        }
        //always write the message so we see it in the log,check frequency only for the exception
        logger.debug(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
            return;
        }
        logger.debug(e);
    }


    public static void warnWithException(Logger logger, Throwable e, String format, Object... args) {

        //don't log NoSelectedEnvironmentException in warn level
        if (e instanceof NoSelectedEnvironmentException) {
            debugWithException(logger, e, format, args);
        } else {
            //always write the message so we see it in the log,check frequency only for the exception
            logger.warn(DIGMA + String.format(format.replace("{}", "%s"), args));
            if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
                return;
            }
            logger.warn(e);
        }
    }

    public static void warnWithException(Logger logger, Project project, Throwable e, String format, Object... args) {

        //don't log NoSelectedEnvironmentException in warn level
        if (e instanceof NoSelectedEnvironmentException) {
            debugWithException(logger, project, e, format, args);
        } else {
            //always write the message so we see it in the log,check frequency only for the exception
            logger.warn(DIGMA_PROJECT + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
            if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, e)) {
                return;
            }
            logger.warn(e);
        }
    }


    public static void log(Consumer<String> consumer, Project project, String msg) {
        consumer.accept(DIGMA_PROJECT + project.getName() + ": " + msg);
    }

    public static void log(Consumer<String> consumer, String msg) {
        consumer.accept(DIGMA + msg);
    }


    // **********  New API

    public static void info(Logger logger, Project project, String format, Object... args) {
        logger.info(DIGMA_PROJECT + project.getName() + ": " + format(format, args));
    }

    public static void info(Logger logger, String format, Object... args) {
        logger.info(DIGMA + ": " + format(format, args));
    }



    public static void warn(Logger logger, Project project, String format, Object... args) {
        logger.warn(DIGMA_PROJECT + project.getName() + ": " + format(format, args));
    }

    public static void warn(Logger logger, String format, Object... args) {
        logger.warn(DIGMA + ": " + format(format, args));
    }


    public static void trace(Logger logger, Project project, String format, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(DIGMA_PROJECT + project.getName() + ": " + format(format, args));
        }
    }

    public static void trace(Logger logger, String format, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(DIGMA + ": " + format(format, args));
        }
    }

    public static void debug(Logger logger, Project project, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(DIGMA_PROJECT + project.getName() + ": " + format(format, args));
        }
    }

    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(DIGMA + ": " + format(format, args));
        }
    }


    private static String format(String format, Object... args) {
        return String.format(format.replace("{}", "%s"), args);
    }


    //Note: We should never log error in intellij because logging error will popup a red error to the user.

//    private static void error(Logger logger, Project project, Exception exception, String format, Object... args) {
//
//        if (exception instanceof NoSelectedEnvironmentException) {
//            debugWithException(logger, exception, format, args);
//        } else {
//
//            if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, exception)) {
//                return;
//            }
//
//            var msg = String.format(format.replace("{}", "%s"), args);
//            error(logger, exception, DIGMA_PROJECT + project.getName() + ": " + msg);
//            ErrorReporter.getInstance().reportError(project, "Log.error", exception);
//        }
//    }
//
//    private static void error(Logger logger, Exception exception, String format, Object... args) {
//        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(format, exception)) {
//            return;
//        }
//        error(logger, exception, DIGMA + String.format(format.replace("{}", "%s"), args));
//    }

//    private static void error(Logger logger, Exception exception, String msg) {
//        if (FREQUENT_ERROR_DETECTOR.isTooFrequentException(msg, exception)) {
//            return;
//        }
//        logger.error(DIGMA + msg, exception);
//    }

//    private static void error(Logger logger, String msg) {
//        logger.error(DIGMA + msg);
//    }


}
