package org.digma.intellij.plugin.analytics;

public class NoSelectedEnvironmentException extends AnalyticsServiceException {
    public NoSelectedEnvironmentException() {
    }

    public NoSelectedEnvironmentException(String message) {
        super(message);
    }

    public NoSelectedEnvironmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSelectedEnvironmentException(Throwable cause) {
        super(cause);
    }

    public NoSelectedEnvironmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
