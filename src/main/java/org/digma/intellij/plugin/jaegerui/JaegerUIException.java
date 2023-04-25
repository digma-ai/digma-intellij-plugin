package org.digma.intellij.plugin.jaegerui;

public class JaegerUIException extends RuntimeException{
    public JaegerUIException() {
    }

    public JaegerUIException(String message) {
        super(message);
    }

    public JaegerUIException(String message, Throwable cause) {
        super(message, cause);
    }

    public JaegerUIException(Throwable cause) {
        super(cause);
    }

    public JaegerUIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
