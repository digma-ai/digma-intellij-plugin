package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }

    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/toolwindow.png", Icons.class);
    public static final Icon INSIGHT_METHOD_SCOPE_16 = AllIcons.Nodes.Method; // size of 16
    public static final Icon INSIGHT_METHOD_SCOPE = INSIGHT_METHOD_SCOPE_16;
    public static final Icon INSIGHT_DOCUMENT_SCOPE = INSIGHT_METHOD_SCOPE_16;
    public static final Icon INSIGHT_EMPTY_SCOPE = INSIGHT_METHOD_SCOPE_16;
    public static final Icon TELESCOPE_16 = loadAndScaleIcon("/icons/telescope.png", 16);
    public static final Icon INTERFACE_16 = AllIcons.Nodes.Interface;

    public static class Insight {

        private Insight() {
        }

        public static final Icon BOTTLENECK = loadAndScaleIcon("/icons/insight/bottleneck.png", 32);
        public static final Icon LOW_USAGE = loadAndScaleIcon("/icons/insight/gauge_low.png", 32);
        public static final Icon NORMAL_USAGE = loadAndScaleIcon("/icons/insight/gauge_normal.png", 32);
        public static final Icon HIGH_USAGE = loadAndScaleIcon("/icons/insight/gauge_high.png", 32);
        public static final Icon SLOW = loadAndScaleIcon("/icons/insight/slow.png", 32);
        public static final Icon THIN_TARGET = loadAndScaleIcon("/icons/insight/thin-target.png", 32);

        //meaningful names for specific views,makes it easier to replace sizes or read from properties files.
        public static final Icon HOTSPOT = loadAndScaleIcon("/icons/insight/target.png", 32);
        public static final Icon SPAN_GROUP_TITLE = TELESCOPE_16;
        public static final Icon HTTP_GROUP_TITLE = INTERFACE_16;
    }


    public static Icon loadAndScaleIcon(String path, int size) {
        return loadAndScaleIcon(path, size, size);
    }

    public static Icon loadAndScaleIcon(String path, int width, int height) {
        try (InputStream inputStream = Icons.class.getResourceAsStream(path)) {
            BufferedImage image = ImageIO.read(inputStream);
            Image resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
