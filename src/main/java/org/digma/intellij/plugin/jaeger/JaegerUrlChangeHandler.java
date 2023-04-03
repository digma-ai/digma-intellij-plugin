package org.digma.intellij.plugin.jaeger;

import org.digma.intellij.plugin.analytics.JaegerUrlChanged;

/**
 * The central handler of JaegerUrlChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class JaegerUrlChangeHandler implements JaegerUrlChanged {

    @Override
    public void jaegerUrlChanged(String newJaegerUrl) {
        //nothing to do here
    }
}
