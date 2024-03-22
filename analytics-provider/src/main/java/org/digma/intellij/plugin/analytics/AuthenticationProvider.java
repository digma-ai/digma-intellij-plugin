package org.digma.intellij.plugin.analytics;

import javax.annotation.CheckForNull;

public interface AuthenticationProvider {

    @CheckForNull
    String getAuthenticationToken();

}
