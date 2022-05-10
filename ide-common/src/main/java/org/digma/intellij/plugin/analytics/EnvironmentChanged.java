package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface EnvironmentChanged {


    Topic<EnvironmentChanged> ENVIRONMENT_CHANGED_TOPIC = Topic.create("ENVIRONMENT_CHANGED_TOPIC", EnvironmentChanged.class);

    void environmentChanged(String newEnv);

}
