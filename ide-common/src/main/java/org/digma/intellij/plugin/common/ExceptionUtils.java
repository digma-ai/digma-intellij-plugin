package org.digma.intellij.plugin.common;

import org.digma.intellij.plugin.analytics.AnalyticsProviderException;
import org.jetbrains.annotations.*;

import javax.net.ssl.SSLException;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.util.*;

public class ExceptionUtils {

    @Nullable
    public static <T extends Throwable> T findCause(@NotNull Class<T> toFind, @NotNull Throwable throwable) {

        Throwable cause = throwable;
        while (cause != null && !toFind.equals(cause.getClass())) {
            cause = cause.getCause();
        }

        if (cause != null) {
            return toFind.cast(cause);
        }

        return null;
    }


    //find the first exception that is not InvocationTargetException and not UndeclaredThrowableException
    @Nullable
    public static Throwable findFirstNonWrapperException(@NotNull Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof InvocationTargetException || cause instanceof UndeclaredThrowableException) {
            cause = cause.getCause();
        }

        return cause;
    }

    @Nullable
    public static Throwable findFirstRealExceptionCause(@NotNull Throwable throwable) {
        Throwable cause = throwable;
        while (cause instanceof InvocationTargetException || cause instanceof UndeclaredThrowableException) {

            cause = cause.getCause();
            //special treatment for AnalyticsProviderException, AnalyticsProviderException.cause may be null
            if (cause instanceof AnalyticsProviderException analyticsProviderException && analyticsProviderException.getCause() != null) {
                cause = analyticsProviderException.getCause();
            }
        }

        return cause;
    }

    @NotNull
    public static Class<? extends Throwable> findFirstRealExceptionCauseType(@NotNull Throwable throwable) {
        var realCause = findFirstRealExceptionCause(throwable);
        return Objects.requireNonNullElse(realCause, throwable).getClass();
    }


    @NotNull
    public static String findFirstRealExceptionCauseTypeName(@NotNull Throwable throwable) {
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
        if ((SocketTimeoutException.class.equals(exception.getClass()) && !isConnectTimeout(exception)) ||
                HttpTimeoutException.class.equals(exception.getClass()) ||
                isIOExceptionTimeout(exception)) {
            return false;
        }

        return isConnectTimeout(exception) ||
                isConnectionIssueErrorCode(exception) ||
                exception instanceof SocketException ||
                exception instanceof UnknownHostException ||
                exception instanceof HttpConnectTimeoutException ||
                exception instanceof InterruptedIOException;

    }


    private static boolean isConnectionIssueErrorCode(@NotNull Throwable exception) {
        var analyticsProviderException = findCause(AnalyticsProviderException.class, exception);

        if (analyticsProviderException != null) {
            var connectionIssueCodes = List.of(503, 504, 502);
            return connectionIssueCodes.contains(analyticsProviderException.getResponseCode());
        }

        return false;
    }

    private static boolean isConnectTimeout(@NotNull Throwable exception) {
        return SocketTimeoutException.class.equals(exception.getClass()) && "Connect timed out".equalsIgnoreCase(exception.getMessage());
    }


    private static boolean isIOExceptionTimeout(@NotNull Throwable exception) {
        return IOException.class.equals(exception.getClass()) &&
                exception.getMessage() != null &&
                exception.getMessage().toLowerCase().contains("timeout");

    }


    public static boolean isSslConnectionException(@NotNull Throwable e) {
        var cause = findCause(SSLException.class, e);
        return cause != null;
    }

    @Nullable
    public static String getSslExceptionMessage(@NotNull Throwable e) {
        var cause = findCause(SSLException.class, e);
        if (cause != null) {
            return cause.getMessage();
        }

        return null;
    }


    @Nullable
    public static String getConnectExceptionMessage(@NotNull Throwable e) {
        var ex = e.getCause();
        while (ex != null && !(isConnectionUnavailableException(ex))) {
            ex = ex.getCause();
        }
        if (ex != null) {
            return ex.getMessage();
        }

        return null;
    }


    public static boolean isEOFException(@NotNull Throwable e) {
        return findCause(EOFException.class, e) != null;
    }


    @NotNull
    public static String getNonEmptyMessage(@NotNull Throwable exception) {

        var realCause = findFirstRealExceptionCause(exception);
        if (realCause != null && realCause.getMessage() != null) {
            return realCause.getMessage();
        }

        var exceptionMessage = exception.getMessage();
        var cause = exception;
        while (cause != null && exceptionMessage == null) {
            exceptionMessage = cause.getMessage();
            cause = cause.getCause();
        }

        if (exceptionMessage == null) {
            exceptionMessage = exception.toString();
        }

        return exceptionMessage;
    }


}
