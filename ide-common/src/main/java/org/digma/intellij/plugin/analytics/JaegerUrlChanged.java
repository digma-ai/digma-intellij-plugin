package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface JaegerUrlChanged {

    @Topic.ProjectLevel
    Topic<JaegerUrlChanged> JAEGER_URL_CHANGED_TOPIC = Topic.create("JAEGER_URL_CHANGED_TOPIC", JaegerUrlChanged.class);

    void jaegerUrlChanged(String newJaegerUrl);

}
