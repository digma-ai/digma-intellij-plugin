package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.common.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class AnalyticsServiceException extends Exception {

    public AnalyticsServiceException() {
        super();
    }

    public AnalyticsServiceException(String message) {
        super(message);
    }

    public AnalyticsServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalyticsServiceException(Throwable cause) {
        super(cause);
    }

    public AnalyticsServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @NotNull
    public String getMeaningfulMessage() {


        AnalyticsProviderException analyticsProviderException = ExceptionUtils.findCause(AnalyticsProviderException.class, this);

        if (analyticsProviderException != null) {
            return analyticsProviderException.getMessage();
        }

        InvocationTargetException invocationTargetException = ExceptionUtils.findCause(InvocationTargetException.class, this);
        if (invocationTargetException != null) {
            if (invocationTargetException.getCause() != null && invocationTargetException.getCause().getMessage() != null) {
                return invocationTargetException.getCause().getMessage();
            } else if (invocationTargetException.getMessage() != null) {
                return invocationTargetException.getMessage();
            }
        }

        return getMessage();
    }
}
