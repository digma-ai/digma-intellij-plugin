package org.digma.intellij.plugin.refreshInsightsTask;

import com.intellij.util.messages.Topic;

public interface RefreshInsightsTaskScheduled {

    @Topic.ProjectLevel
    Topic<RefreshInsightsTaskScheduled> REFRESH_INSIGHTS_TASK_TOPIC = Topic.create("REFRESH_INSIGHTS_TASK_TOPIC", RefreshInsightsTaskScheduled.class);

    void refreshInsightsTaskStarted();

    void refreshInsightsTaskFinished();

}