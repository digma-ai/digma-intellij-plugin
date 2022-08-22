package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface AnalyticsServiceConnectionEvent {


    @com.intellij.util.messages.Topic.ProjectLevel
    Topic<AnalyticsServiceConnectionEvent> ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC = Topic.create("ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC", AnalyticsServiceConnectionEvent.class);

    void connectionLost();

    void connectionGained();

}
