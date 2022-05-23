package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }

    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/toolwindow.png", Icons.class);
    public static final Icon INSIGHT_METHOD_SCOPE = IconLoader.getIcon("/icons/method.png", Icons.class);

    public static final Icon HOTSPOT = IconLoader.getIcon("/icons/hotspot.png", Icons.class);
    public static final Icon HOTSPOT_32 = IconLoader.getIcon("/icons/hotspot-32.png", Icons.class);
    public static final Icon HOTSPOT_24 = IconLoader.getIcon("/icons/hotspot-24.png", Icons.class);
    public static final Icon TELESCOPE_12 = IconLoader.getIcon("/icons/telescope-12.png", Icons.class);
    public static final Icon INTERFACE_16 = AllIcons.Nodes.Interface;

    static public class Insight {
        public static final Icon BOTTLENECK = insightIcon(IconLoader.getIcon("/icons/insight/bottleneck.png", Icons.class));
        public static final Icon LOW_USAGE = insightIcon(IconLoader.getIcon("/icons/insight/gauge_low.png", Icons.class));
        public static final Icon NORMAL_USAGE = insightIcon(IconLoader.getIcon("/icons/insight/gauge_normal.png", Icons.class));
        public static final Icon HIGH_USAGE = insightIcon(IconLoader.getIcon("/icons/insight/gauge_high.png", Icons.class));
        public static final Icon SLOW = insightIcon(IconLoader.getIcon("/icons/insight/slow.png", Icons.class));
        public static final Icon TARGET = insightIcon(IconLoader.getIcon("/icons/insight/target.png", Icons.class));
        public static final Icon THIN_TARGET = insightIcon(IconLoader.getIcon("/icons/insight/thin-target.png", Icons.class));
    }

    public static ImageIcon scaledIcon(Icon icon, int size) {
        final Image origImage = IconLoader.toImage(icon);
        final Image scaledImage = origImage.getScaledInstance(size, size, Image.SCALE_DEFAULT);
        final ImageIcon imageIcon = new ImageIcon(scaledImage);
        return imageIcon;
    }

    public static ImageIcon insightIcon(Icon icon) {
        return scaledIcon(icon, 32);
    }
}
