package org.digma.intellij.plugin.navigation;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ViewChangedEvent {

    @Topic.ProjectLevel
    Topic<ViewChangedEvent> VIEW_CHANGED_TOPIC = Topic.create("VIEW_CHANGED_TOPIC", ViewChangedEvent.class);

    void viewChanged(@NotNull List<View> views);
}
