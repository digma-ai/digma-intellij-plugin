package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface TabsChanged {

    @Topic.ProjectLevel
    Topic<TabsChanged> TABS_CHANGED_TOPIC = Topic.create("TABS_CHANGED_TOPIC", TabsChanged.class);

    void activeTabIndexChanged(int newTabIndex);

}
