package org.digma.intellij.plugin.analytics;

public class AuthenticationException extends AnalyticsProviderException {

    public AuthenticationException(int code, String message) {
        super(code, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(Throwable cause) {
        super(cause);
    }
}
