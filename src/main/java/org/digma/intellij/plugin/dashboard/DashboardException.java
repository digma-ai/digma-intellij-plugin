package org.digma.intellij.plugin.dashboard;

public class DashboardException extends RuntimeException {
    public DashboardException() {
    }

    public DashboardException(String message) {
        super(message);
    }

    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }

    public DashboardException(Throwable cause) {
        super(cause);
    }

    public DashboardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}