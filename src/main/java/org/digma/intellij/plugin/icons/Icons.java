package org.digma.intellij.plugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {}

    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/toolwindow.png", Icons.class);
    public static final Icon INSIGHT_METHOD_SCOPE = IconLoader.getIcon("/icons/method.png", Icons.class);

    public static final Icon HOTSPOT = IconLoader.getIcon("/icons/hotspot.png", Icons.class);
    public static final Icon HOTSPOT_32 = IconLoader.getIcon("/icons/hotspot-32.png", Icons.class);
    public static final Icon TELESCOPE_12 = IconLoader.getIcon("/icons/telescope-12.png", Icons.class);
}
