package org.digma.intellij.plugin.icons;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.util.IconUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;

import static com.intellij.ui.scale.ScaleType.OBJ_SCALE;

public class IconsUtil {

    private static final Logger LOGGER = Logger.getInstance(IconsUtil.class);


    @SuppressWarnings("unused")
    public static Icon loadAndScaleIconObjectByFactor(String path, double scale) {
        return IconUtil.scale(IconLoader.getIcon(path, IconsUtil.class.getClassLoader()),
                ScaleContext.create(OBJ_SCALE.of(scale)));
    }


    public static Icon loadAndScaleIconObjectByWidth(String path, int width) {
        var icon = IconLoader.getIcon(path, IconsUtil.class.getClassLoader());
        //default icon is not a good scaling, hopefully IconUtil.scaleByIconWidth will succeed
        @SuppressWarnings("SuspiciousNameCombination")
        var defaultIcon = loadAndScaleIconBySize(path, width, width);
        return IconUtil.scaleByIconWidth(icon, null, defaultIcon);
    }


    public static Icon loadAndScaleIconBySize(String path, int width, int height) {
        try (InputStream inputStream = IconsUtil.class.getResourceAsStream(path)) {
            Objects.requireNonNull(inputStream);
            BufferedImage image = ImageIO.read(inputStream);
            Image resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Icon createRedDotBadgeIcon(Icon icon, int emptySize) {
        return com.intellij.execution.runners.ExecutionUtil.getIndicator(icon, emptySize, emptySize, JBColor.RED);
    }


}
