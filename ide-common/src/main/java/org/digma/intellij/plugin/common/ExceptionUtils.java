package org.digma.intellij.plugin.common;

import com.google.common.base.Throwables;
import org.digma.intellij.plugin.analytics.*;
import org.jetbrains.annotations.*;

import javax.net.ssl.SSLException;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.http.*;
import java.util.List;

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


    @Nullable
    public static <T extends Throwable> T findAssignableCause(@NotNull Class<T> toFind, @NotNull Throwable throwable) {

        Throwable cause = throwable;
        while (cause != null && !toFind.isAssignableFrom(cause.getClass())) {
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


    @NotNull
    public static Throwable findRootCause(@NotNull Throwable throwable) {

        try {
            return Throwables.getRootCause(throwable);
        } catch (Throwable e) {
            return throwable;
        }

        //this code finds root cause without throwing exception in loop like guava does
//        Throwable cause = throwable;
//        Throwable latestNonNullCause = null;
//        while (cause != null && cause != latestNonNullCause) {
//            latestNonNullCause = cause;
//            cause = cause.getCause();
//        }
//
//        return latestNonNullCause;
    }


    @NotNull
    public static Class<? extends Throwable> findRootCauseType(@NotNull Throwable throwable) {
        var realCause = findRootCause(throwable);
        return realCause.getClass();
    }


    @NotNull
    public static String findRootCauseTypeName(@NotNull Throwable throwable) {
        return findRootCauseType(throwable).getName();
    }


    @Nullable
    public static Throwable findConnectException(@NotNull Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null && !isConnectionUnavailableException(cause)) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Nullable
    public static Throwable findSslException(@NotNull Throwable throwable) {
        return findAssignableCause(SSLException.class, throwable);
    }


    @Nullable
    public static Throwable findAuthenticationException(@NotNull Throwable throwable) {
        return findCause(AuthenticationException.class, throwable);
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


        //this is a real timeout
        if (exception instanceof SocketTimeoutException socketTimeoutException && socketTimeoutException.bytesTransferred > 0) {
            return false;
        }
        if (exception instanceof InterruptedIOException interruptedIOException && interruptedIOException.bytesTransferred > 0) {
            return false;
        }

        if (HttpTimeoutException.class.equals(exception.getClass()) ||
                isIOExceptionTimeout(exception)) {
            return false;
        }

        //this includes any SocketTimeoutException, it's derived from InterruptedIOException
        return is404PageNotFound(exception) ||
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

    //this is considered no connection
    private static boolean isSocketTimeoutExceptionConnectTimeout(@NotNull Throwable exception) {
        return SocketTimeoutException.class.equals(exception.getClass()) &&
                exception.getMessage() != null &&
                exception.getMessage().trim().toLowerCase().contains("connect timed out");
    }


    private static boolean isIOExceptionTimeout(@NotNull Throwable exception) {
        return IOException.class.equals(exception.getClass()) &&
                exception.getMessage() != null &&
                exception.getMessage().trim().toLowerCase().contains("timeout");

    }

    private static boolean is404PageNotFound(@NotNull Throwable exception) {
        return exception.getMessage() != null &&
                exception.getMessage().trim().toLowerCase().contains("404 page not found");

    }


    public static boolean isSslConnectionException(@NotNull Throwable e) {
        var cause = findAssignableCause(SSLException.class, e);
        return cause != null;
    }

    @Nullable
    public static String getSslExceptionMessage(@NotNull Throwable e) {
        var cause = findAssignableCause(SSLException.class, e);
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

        var realCause = findRootCause(exception);
        if (realCause.getMessage() != null) {
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
