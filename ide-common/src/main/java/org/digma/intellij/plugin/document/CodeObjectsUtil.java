package org.digma.intellij.plugin.document;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;


public class CodeObjectsUtil {

    public static String createSpanId(@NotNull String instrumentationLibrary, @NotNull String name) {
        return instrumentationLibrary + "$_$" + name;
    }

    public static String createSpanIdWithType(@NotNull String instrumentationLibrary, @NotNull String name) {
        return "span:" + createSpanId(instrumentationLibrary, name);
    }

    public static String createMethodCodeObjectId(@NotNull String className, @NotNull String methodName) {
        return className + "$_$" + methodName;
    }

    public static String createMethodCodeObjectIdWithType(@NotNull String className, @NotNull String methodName) {
        return "method:" + createMethodCodeObjectId(className, methodName);
    }

    public static String addSpanTypeToId(@NotNull String spanId) {
        return spanId.startsWith("span:") ? spanId : "span:" + spanId;
    }

    public static List<String> addSpanTypeToIds(@NotNull Collection<String> spanIds) {
        return spanIds.stream().map(id -> id.startsWith("span:") ? id : "span:" + id).toList();
    }

    public static String addMethodTypeToId(@NotNull String methodId) {
        return methodId.startsWith("method:") ? methodId : "method:" + methodId;
    }

    public static List<String> addMethodTypeToIds(@NotNull Collection<String> methodIds) {
        return methodIds.stream().map(id -> id.startsWith("method:") ? id : "method:" + id).toList();
    }


    public static String stripSpanPrefix(@NotNull String spanCodeObjectId) {
        return stripPrefix(spanCodeObjectId);
    }

    public static String stripMethodPrefix(@NotNull String methodCodeObjectId) {
        return stripPrefix(methodCodeObjectId);
    }

    public static String stripEndpointPrefix(@NotNull String endpointCodeObjectId) {
        return stripPrefix(endpointCodeObjectId);
    }


    private static String stripPrefix(@NotNull String codeObjectId) {
        if (codeObjectId.startsWith("method:")) {
            return codeObjectId.substring("method:".length());
        } else if (codeObjectId.startsWith("span:")) {
            return codeObjectId.substring("span:".length());
        } else if (codeObjectId.startsWith("endpoint:")) {
            return codeObjectId.substring("endpoint:".length());
        }
        return codeObjectId;
    }


    @NotNull
    public static String getShortNameForDisplay(@NotNull String methodId) {
        var lastIndexOfDot = methodId.lastIndexOf('.');
        var shortPart = methodId;
        if (lastIndexOfDot >= 0) {
            shortPart = methodId.substring(lastIndexOfDot + 1);
        }
        return shortPart.replace("$_$", ".");
    }

    @NotNull
    public static Pair<String, String> getMethodClassAndName(@NotNull String methodId) {

        methodId = stripMethodPrefix(methodId);

        if (!methodId.contains("$_$")) {
            return new Pair<>("", methodId);
        }

        var className = methodId.substring(0, methodId.indexOf("$_$"));
        var methodName = methodId.substring(methodId.indexOf("$_$") + 3);

        return new Pair<>(methodName, className);
    }
}
