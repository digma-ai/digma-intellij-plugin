package org.digma.intellij.plugin.psi;

public class PsiFileNotFountException extends Exception{
    public PsiFileNotFountException() {
    }

    public PsiFileNotFountException(String message) {
        super(message);
    }

    public PsiFileNotFountException(String message, Throwable cause) {
        super(message, cause);
    }

    public PsiFileNotFountException(Throwable cause) {
        super(cause);
    }

    public PsiFileNotFountException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
