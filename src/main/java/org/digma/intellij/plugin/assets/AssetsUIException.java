package org.digma.intellij.plugin.assets;

public class AssetsUIException extends RuntimeException {
    public AssetsUIException() {
    }

    public AssetsUIException(String message) {
        super(message);
    }

    public AssetsUIException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssetsUIException(Throwable cause) {
        super(cause);
    }

    public AssetsUIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
