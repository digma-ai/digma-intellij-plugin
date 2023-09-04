package org.digma.intellij.plugin.common;

import org.jetbrains.annotations.Nullable;

public class ExceptionUtils {

    @Nullable
    public static <T extends Throwable> T findCause(Class<T> toFind, Throwable throwable) {

        Throwable cause = throwable;
        while (cause != null && !toFind.isAssignableFrom(cause.getClass())) {
            cause = cause.getCause();
        }

        return (T) cause;
    }


}
