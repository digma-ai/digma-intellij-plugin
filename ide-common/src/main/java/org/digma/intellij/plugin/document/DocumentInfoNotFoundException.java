package org.digma.intellij.plugin.document;

public class DocumentInfoNotFoundException extends RuntimeException{
    public DocumentInfoNotFoundException() {
    }

    public DocumentInfoNotFoundException(String message) {
        super(message);
    }

    public DocumentInfoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentInfoNotFoundException(Throwable cause) {
        super(cause);
    }

    public DocumentInfoNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
