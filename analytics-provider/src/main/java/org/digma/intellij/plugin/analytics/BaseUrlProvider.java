package org.digma.intellij.plugin.analytics;

import javax.annotation.Nonnull;

//provides dynamically changing url to okhttp client
public interface BaseUrlProvider {

    @Nonnull
    String baseUrl();
}
