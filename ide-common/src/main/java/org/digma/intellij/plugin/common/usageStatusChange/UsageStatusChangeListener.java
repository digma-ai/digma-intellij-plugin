package org.digma.intellij.plugin.common.usageStatusChange;

import com.intellij.util.messages.Topic;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;

public interface UsageStatusChangeListener {

    @Topic.ProjectLevel
    Topic<UsageStatusChangeListener> USAGE_STATUS_CHANGED_TOPIC = Topic.create("USAGE_STATUS_CHANGED_TOPIC", UsageStatusChangeListener.class);

    void usageStatusChanged(UsageStatusResult newUsageStatusResult);

}
