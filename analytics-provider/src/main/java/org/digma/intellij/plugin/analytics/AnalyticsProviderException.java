package org.digma.intellij.plugin.analytics;

public class AnalyticsProviderException extends RuntimeException {

    private int responseCode;

    public AnalyticsProviderException(int code, String message) {
        super(message);
        this.responseCode = code;
    }

    public AnalyticsProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalyticsProviderException(Throwable cause) {
        super(cause);
    }

    public int getResponseCode() {
        return responseCode;
    }
}
