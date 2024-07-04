package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.eclipse.sisu.Nullable;

import java.util.List;

public interface EnvironmentChanged {

    @com.intellij.util.messages.Topic.ProjectLevel
    Topic<EnvironmentChanged> ENVIRONMENT_CHANGED_TOPIC = Topic.create("ENVIRONMENT_CHANGED_TOPIC", EnvironmentChanged.class);

    void environmentChanged(@Nullable Env newEnv);

    void environmentsListChanged(List<Env> newEnvironments);

}
