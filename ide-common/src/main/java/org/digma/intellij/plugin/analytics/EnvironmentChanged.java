package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface EnvironmentChanged {

    @com.intellij.util.messages.Topic.ProjectLevel
    Topic<EnvironmentChanged> ENVIRONMENT_CHANGED_TOPIC = Topic.create("ENVIRONMENT_CHANGED_TOPIC", EnvironmentChanged.class);

    void environmentChanged(@Nullable String newEnv, boolean refreshInsightsView);

    void environmentsListChanged(List<String> newEnvironments);

}
