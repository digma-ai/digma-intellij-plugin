package org.digma.intellij.plugin.observability;

import com.intellij.util.messages.Topic;

public interface ObservabilityChanged {

    @com.intellij.util.messages.Topic.ProjectLevel
    Topic<ObservabilityChanged> OBSERVABILITY_CHANGED_TOPIC = Topic.create("OBSERVABILITY_CHANGED_TOPIC", ObservabilityChanged.class);


    void observabilityChanged(boolean isObservabilityEnabled);
}
