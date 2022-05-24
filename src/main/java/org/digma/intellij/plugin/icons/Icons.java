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
    public static final Icon INSIGHT_METHOD_SCOPE = IconLoader.getIcon("/icons/method.png", Icons.class);

    public static final Icon HOTSPOT = IconLoader.getIcon("/icons/hotspot.png", Icons.class);
    public static final Icon HOTSPOT_32 = IconLoader.getIcon("/icons/hotspot-32.png", Icons.class);
    public static final Icon HOTSPOT_24 = IconLoader.getIcon("/icons/hotspot-24.png", Icons.class);
    public static final Icon TELESCOPE_12 = IconLoader.getIcon("/icons/telescope-12.png", Icons.class);
    public static final Icon INTERFACE_16 = AllIcons.Nodes.Interface;

    static public class Insight {
        public static final Icon BOTTLENECK = loadAndScaleIcon("/icons/insight/bottleneck.png", 32);
        public static final Icon LOW_USAGE = loadAndScaleIcon("/icons/insight/gauge_low.png", 32);
        public static final Icon NORMAL_USAGE = loadAndScaleIcon("/icons/insight/gauge_normal.png", 32);
        public static final Icon HIGH_USAGE = loadAndScaleIcon("/icons/insight/gauge_high.png", 32);
        public static final Icon SLOW = loadAndScaleIcon("/icons/insight/slow.png", 32);
        public static final Icon TARGET = loadAndScaleIcon("/icons/insight/target.png", 32);
        public static final Icon TARGET_24 = loadAndScaleIcon("/icons/insight/target.png",24);
        public static final Icon THIN_TARGET = loadAndScaleIcon("/icons/insight/thin-target.png", 32);
    }



    public static Icon loadAndScaleIcon(String path,int size) {
        return loadAndScaleIcon(path,size,size);
    }
    public static Icon loadAndScaleIcon(String path,int width,int height) {
        try {
            InputStream inputStream = Icons.class.getResourceAsStream(path);
            BufferedImage image = ImageIO.read(inputStream);
            Image resized = image.getScaledInstance(width,height, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }






}
