package org.digma.intellij.plugin.toolwindow;

import javax.swing.*;
import java.awt.*;

public class ThemeUtil {

    public static String getCurrentThemeName() {
        String lafClassName = UIManager.getLookAndFeel().getClass().getSimpleName();
        if ("IntelliJLaf".equals(lafClassName) || "MacIntelliJLaf".equals(lafClassName)) {
            Color bgColor = UIManager.getLookAndFeelDefaults().getColor("EditorPane.background");
            if (bgColor != null && isDark(bgColor)) {
                return UiTheme.DARK.getThemeName();
            } else {
                return UiTheme.LIGHT.getThemeName();
            }
        } else {
            return UiTheme.DARK.getThemeName();
        }
    }

    private static boolean isDark(Color color) {
        // Calculate the perceived brightness of the color using the sRGB color space
        double brightness = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255;
        return brightness < 0.5;
    }
}
