package org.digma.intellij.plugin.analytics;

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
}
