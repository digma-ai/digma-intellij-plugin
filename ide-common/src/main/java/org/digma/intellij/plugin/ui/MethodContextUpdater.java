package org.digma.intellij.plugin.ui;

import org.digma.intellij.plugin.model.MethodUnderCaret;

public interface MethodContextUpdater {

    void updateViewContent(MethodUnderCaret methodUnderCaret);

    void clearViewContent();
}
