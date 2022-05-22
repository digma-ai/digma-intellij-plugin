package org.digma.intellij.plugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }

    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/toolwindow.png", Icons.class);
    public static final Icon INSIGHT_METHOD_SCOPE = IconLoader.getIcon("/icons/method.png", Icons.class);

    public static final Icon HOTSPOT = IconLoader.getIcon("/icons/hotspot.png", Icons.class);
    public static final Icon HOTSPOT_32 = IconLoader.getIcon("/icons/hotspot-32.png", Icons.class);
    public static final Icon TELESCOPE_12 = IconLoader.getIcon("/icons/telescope-12.png", Icons.class);

    static public class Insight {

        public static final Icon BOTTLENECK = IconLoader.getIcon("/icons/insight/bottleneck-32.png", Icons.class);
        public static final Icon LOW_USAGE = IconLoader.getIcon("/icons/insight/gauge_low-32.png", Icons.class);
        public static final Icon NORMAL_USAGE = IconLoader.getIcon("/icons/insight/gauge_normal-32.png", Icons.class);
        public static final Icon HIGH_USAGE = IconLoader.getIcon("/icons/insight/gauge_high-32.png", Icons.class);
        public static final Icon SLOW = IconLoader.getIcon("/icons/insight/slow-32.png", Icons.class);
        public static final Icon TARGET = IconLoader.getIcon("/icons/insight/target.png", Icons.class);
        public static final Icon THIN_TARGET = IconLoader.getIcon("/icons/insight/thin-target.png", Icons.class);
    }
}
