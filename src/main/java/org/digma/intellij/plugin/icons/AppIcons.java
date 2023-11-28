package org.digma.intellij.plugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * put here icons that usually load before our tool window and UI is built
 */
public final class AppIcons {

    private AppIcons() {
    }

    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/digma.svg", AppIcons.class.getClassLoader());
    public static final Icon TOOL_WINDOW_OBSERVABILITY = IconLoader.getIcon("/icons/active-env.svg", AppIcons.class.getClassLoader());


}
