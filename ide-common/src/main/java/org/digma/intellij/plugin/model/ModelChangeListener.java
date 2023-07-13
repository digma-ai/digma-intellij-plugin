package org.digma.intellij.plugin.model;

import com.intellij.util.messages.Topic;
import org.digma.intellij.plugin.ui.model.PanelModel;

public interface ModelChangeListener {

    @Topic.ProjectLevel
    Topic<ModelChangeListener> MODEL_CHANGED_TOPIC = Topic.create("MODEL_CHANGED_TOPIC", ModelChangeListener.class);

    void modelChanged(PanelModel newModel);

}