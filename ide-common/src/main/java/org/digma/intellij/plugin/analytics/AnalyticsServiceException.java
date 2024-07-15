package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.common.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

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

    public int getErrorCode(){
        AnalyticsProviderException analyticsProviderException = ExceptionUtils.findCause(AnalyticsProviderException.class, this);
        if (analyticsProviderException != null) {
            return analyticsProviderException.getResponseCode();
        }

        return -1;
    }


    @NotNull
    public String getNonNullMessage() {

        if (getMessage() != null) {
            return getMessage();
        }

        AnalyticsProviderException analyticsProviderException = ExceptionUtils.findCause(AnalyticsProviderException.class, this);

        if (analyticsProviderException != null && analyticsProviderException.getMessage() != null) {
            return analyticsProviderException.getMessage();
        }

        return toString();
    }
}
