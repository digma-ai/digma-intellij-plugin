package org.digma.intellij.plugin.documentation;

public class DocumentationException extends RuntimeException {
    public DocumentationException() {
    }

    public DocumentationException(String message) {
        super(message);
    }

    public DocumentationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentationException(Throwable cause) {
        super(cause);
    }

    public DocumentationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
