package org.digma.intellij.plugin.icons;

import javax.swing.*;

import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByFactor;
import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByWidth;

/**
 * put here icons that usually load before our tool window and UI is built
 */
public final class AppIcons {

    private AppIcons() {
    }

    public static final Icon TOOL_WINDOW = loadAndScaleIconObjectByWidth("/icons/digma.png", 13);
    public static final Icon TOOL_WINDOW_OBSERVABILITY = loadAndScaleIconObjectByFactor("/icons/active-env.svg", 1);


}
