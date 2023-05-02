package org.digma.intellij.plugin.document;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class CodeObjectsUtil {

    public static String createSpanId(@NotNull String instrumentationLibrary, @NotNull String name) {
        return instrumentationLibrary + "$_$" + name;
    }

    public static String createSpanIdWithType(@NotNull String instrumentationLibrary, @NotNull String name) {
        return "span:"+createSpanId(instrumentationLibrary,name);
    }

    public static String createMethodCodeObjectId(@NotNull String className, @NotNull String methodName) {
        return className + "$_$" + methodName;
    }

    public static String createMethodCodeObjectIdWithType(@NotNull String className, @NotNull String methodName) {
        return "method:"+createMethodCodeObjectId(className,methodName);
    }

    public static List<String> addSpanTypeToIds(@NotNull Collection<String> spanIds) {
        return spanIds.stream().map(id -> "span:"+id).toList();
    }

    public static List<String> addMethodTypeToIds(@NotNull Collection<String> methodIds) {
        return methodIds.stream().map(id -> "method:"+id).toList();
    }




}
