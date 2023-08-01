package org.digma.intellij.plugin.troubleshooting;

public class TroubleshootingException extends RuntimeException {
    public TroubleshootingException() {
    }

    public TroubleshootingException(String message) {
        super(message);
    }

    public TroubleshootingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TroubleshootingException(Throwable cause) {
        super(cause);
    }

    public TroubleshootingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
