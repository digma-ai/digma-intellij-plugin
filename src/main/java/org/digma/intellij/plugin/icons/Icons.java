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

    static public class Insight {

        public static final Icon BOTTLENECK = IconLoader.getIcon("/icons/bottleneck.png", Icons.class);
        public static final Icon LOW_USAGE = IconLoader.getIcon("/icons/gauge_low.png", Icons.class);
        public static final Icon NORMAL_USAGE = IconLoader.getIcon("/icons/gauge_normal.png", Icons.class);
        public static final Icon HIGH_USAGE = IconLoader.getIcon("/icons/gauge_high.png", Icons.class);
        public static final Icon SLOW = IconLoader.getIcon("/icons/slow.png", Icons.class);
        public static final Icon TARGET = IconLoader.getIcon("/icons/target.png", Icons.class);
        public static final Icon THIN_TARGET = IconLoader.getIcon("/icons/thin-target.png", Icons.class);
    }
}
