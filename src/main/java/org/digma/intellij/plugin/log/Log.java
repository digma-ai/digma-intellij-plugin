package org.digma.intellij.plugin.log;

import java.util.function.*;

/**
 * A simple logging facility that emulates log4j/slf4j parameter substitution.
 * Jetbrains platform logger does not support parameter substitution.
 */
public class Log {

    public static void log(Consumer<String> consumer, String format, Object...args){
        consumer.accept(String.format(format.replace("{}","%s"),args));
    }

}
