package org.digma.intellij.plugin.psi;


/**
 * Exception that may be thrown by a language service from methods that expect a supported file but
 * where invoked with a non-supported file.
 */
public class NonSupportedFileException extends Exception {

    public NonSupportedFileException(String message) {
        super(message);
    }
}
