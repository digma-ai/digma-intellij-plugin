package org.digma.intellij.plugin.analytics;

public class CantConstructClientException extends AnalyticsProviderException {

    public CantConstructClientException(Throwable e) {
        super(e);
    }
}
