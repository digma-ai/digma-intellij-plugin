package org.digma.intellij.plugin.document;

import org.jetbrains.annotations.NotNull;

public class CodeObjectsUtil {
    public static String createSpanId(@NotNull String instrumentationLibrary, @NotNull String name) {
        return instrumentationLibrary + "$_$" + name;
    }
}
