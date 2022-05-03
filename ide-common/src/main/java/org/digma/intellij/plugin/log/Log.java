package org.digma.intellij.plugin.log;

import com.intellij.openapi.diagnostic.Logger;

import java.util.function.Consumer;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    private static final Logger LOGGER = Logger.getInstance(Log.class);

    public static void log(Consumer<String> consumer, String format, Object... args) {
        //Its not always clear how to change logging level in the ide, didn't find it in
        // rider for example but very easy to find in idea.
        // so to temporarily bypass all loggers uncomment this line.
        //LOGGER.info(String.format(format.replace("{}", "%s"), args));
        //todo: change the test IDE logging level to DEBUG for runIde task before it runs.
        consumer.accept("Digma:"+String.format(format.replace("{}", "%s"), args));
    }

}
