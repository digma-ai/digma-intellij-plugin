package org.digma.intellij.plugin.ui;

import org.digma.intellij.plugin.psi.MethodIdentifier;

public interface MethodContextUpdater {

    void updateViewContent(MethodIdentifier methodIdentifier);

    void clearViewContent();
}
