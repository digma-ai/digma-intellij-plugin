package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    //todo: check how logs come out and maybe change to include project from now on

    public static void log(Consumer<String> consumer, Project project, String format, Object... args) {
        log(consumer, "Digma: Project:" + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String format, Object... args) {
        log(consumer, "Digma: " + String.format(format.replace("{}", "%s"), args));
    }

    public static void log(Consumer<String> consumer, String msg) {
        consumer.accept(msg);
    }

    public static void error(Logger logger,Project project, Exception exception, String format, Object... args) {
        error(logger, exception,"Digma: Project:" + project.getName() + ": " + String.format(format.replace("{}", "%s"), args));
    }
    public static void error(Logger logger, Exception exception, String format, Object... args) {
        error(logger, exception, "Digma: " + String.format(format.replace("{}", "%s"), args));
    }

    public static void error(Logger logger, Exception exception, String msg) {
        logger.error(msg, exception);
    }



}
