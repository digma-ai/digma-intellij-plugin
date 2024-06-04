package org.digma.intellij.plugin.common;

import org.digma.intellij.plugin.analytics.*;
import org.jetbrains.annotations.*;

import javax.net.ssl.SSLException;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.HttpTimeoutException;

public class ExceptionUtils {

    @Nullable
    public static <T extends Throwable> T findCause(@NotNull Class<T> toFind, @NotNull Throwable throwable) {

        Throwable cause = throwable;
        while (cause != null && !toFind.isAssignableFrom(cause.getClass())) {
            cause = cause.getCause();
        }

        return (T) cause;
    }

    @Nullable
    public static <T> T find(@NotNull Exception e, @NotNull Class<T> javaClass) {

        var ex = e.getCause();
        while (ex != null && !(javaClass.equals(ex.getClass()))) {
            ex = ex.getCause();
        }

        if (ex != null) {
            return javaClass.cast(ex);
        }

        return null;
    }


    public static Throwable findFirstRealExceptionCause(@NotNull Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof InvocationTargetException ||
                cause instanceof UndeclaredThrowableException) {

            if (cause instanceof UndeclaredThrowableException undeclaredThrowableException) {
                cause = undeclaredThrowableException.getUndeclaredThrowable();
            } else if (cause instanceof InvocationTargetException invocationTargetException) {
                cause = invocationTargetException.getTargetException();
            }

            if (cause instanceof AnalyticsProviderException && cause.getCause() != null) {
                cause = cause.getCause();
            }
        }

        return cause;
    }

    public static Class<? extends Throwable> findFirstRealExceptionCauseType(@NotNull Throwable throwable) {
        return findFirstRealExceptionCause(throwable).getClass();
    }


    public static String getFirstRealExceptionCauseTypeName(@NotNull Throwable throwable) {
        return findFirstRealExceptionCauseType(throwable).getName();
    }


    public static boolean isAnyConnectionException(@NotNull Throwable e) {
        return isConnectionException(e) || isSslConnectionException(e);
    }

    public static boolean isConnectionException(@NotNull Throwable e) {

        var ex = e.getCause();
        while (ex != null && !(isConnectionUnavailableException(ex))) {
            ex = ex.getCause();
        }
        return ex != null;
    }

    public static boolean isConnectionUnavailableException(@NotNull Throwable exception) {

        //InterruptedIOException is thrown when the connection is dropped , for example by iptables

        //SocketTimeoutException and HttpTimeoutException are not considered connection unavailable.
        //but their derived classed may be. so compare equals and not instanceof
        if (SocketTimeoutException.class.equals(exception.getClass()) ||
                HttpTimeoutException.class.equals(exception.getClass())) {
            return false;
        }

        return exception instanceof SocketException ||
                exception instanceof UnknownHostException ||
                exception instanceof HttpTimeoutException ||
                exception instanceof InterruptedIOException;

    }


    public static boolean isSslConnectionException(@NotNull Throwable e) {

        var ex = e.getCause();
        while (ex != null && !(ex instanceof SSLException)) {
            ex = ex.getCause();
        }
        return ex != null;
    }

    public static String getSslExceptionMessage(@NotNull Throwable e) {
        var ex = e.getCause();
        while (ex != null && !(ex instanceof SSLException)) {
            ex = ex.getCause();
        }
        if (ex != null) {
            return ex.getMessage();
        }

        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }


    public static String getConnectExceptionMessage(@NotNull Throwable e) {
        var ex = e.getCause();
        while (ex != null && !(isConnectionUnavailableException(ex))) {
            ex = ex.getCause();
        }
        if (ex != null) {
            return ex.getMessage();
        }

        return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
    }


    public static boolean isEOFException(@NotNull Throwable e) {
        var ex = e.getCause();
        while (ex != null && !(ex instanceof EOFException)) {
            ex = ex.getCause();
        }
        return ex != null;
    }


    @Nullable
    public static String getNonEmptyMessage(@NotNull Throwable exception) {

        if (exception instanceof AnalyticsServiceException e) {
            return e.getMeaningfulMessage();
        }

        var exc = exception;
        var exceptionMessage = exception.getMessage();
        while ((exceptionMessage == null || exceptionMessage.isEmpty())
                && exc != null) {

            exc = exc.getCause();
            if (exc != null) {
                if (exc instanceof AnalyticsServiceException e) {
                    return e.getMeaningfulMessage();
                } else {
                    exceptionMessage = exc.getMessage();
                }
            }
        }

        if (exceptionMessage == null) {
            return exception.toString();
        }

        return exceptionMessage;
    }


}
