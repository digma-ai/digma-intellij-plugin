package org.digma.intellij.plugin.icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.scale.ScaleContext;
import com.intellij.util.IconUtil;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.intellij.ui.scale.ScaleType.OBJ_SCALE;

public class IconsUtil {




    public static Icon loadAndScaleIconObjectByFactor(String path, double scale) {
        return IconUtil.scale(IconLoader.getIcon(path, Icons.class.getClassLoader()),
                ScaleContext.create(OBJ_SCALE.of(scale)));
    }


    public static Icon loadAndScaleIconObjectByWidth(String path, int width) {
        var icon = IconLoader.getIcon(path, Icons.class.getClassLoader());
        //default icon is not a good scaling, hopefully IconUtil.scaleByIconWidth will succeed
        var defaultIcon = loadAndScaleIconBySize(path, width, width);
        return IconUtil.scaleByIconWidth(icon, null, defaultIcon);
    }


    public static Icon loadAndScaleIconBySize(String path, int width, int height) {
        try (InputStream inputStream = Icons.class.getResourceAsStream(path)) {
            BufferedImage image = ImageIO.read(inputStream);
            Image resized = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(resized);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }





//    public static Icon colorizeIntellijIconIcon(@NotNull Icon icon, Color newColor) {
//        return IconUtil.colorize(icon, newColor);
//    }



    public static Icon colorizeVsCodeIcon(String path, String newColor) {

        try (InputStream inputStream = Icons.class.getResourceAsStream(path)) {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            text = text.replaceAll("currentColor", newColor);
            var tmpFile = File.createTempFile("digma", ".svg");
            FileUtils.writeStringToFile(tmpFile, text, StandardCharsets.UTF_8);
            return IconLoader.findIcon(tmpFile.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
