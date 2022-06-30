package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import org.apache.commons.io.FileUtils;
import org.digma.intellij.plugin.ui.common.Html;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }

    public static final int INSIGHT_ICON_SIZE = 32;
    public static final int ERROR_SCORE_PANEL_SIZE = 32;//its here because it should be the same size as icons
    public static final int ERROR_DETAILS_BACK_BUTTON_SIZE = 32;//its here because it should be the same size as icons
    public static final int ERROR_DETAILS_NAVIGATION_BUTTON_SIZE = 32;//its here because it should be the same size as icons

    public static final Icon TOOL_WINDOW = loadAndScaleIcon("/icons/digma.png", 13);

    public static final Icon METHOD = IconLoader.getIcon("/icons/method.svg", Icons.class.getClassLoader());
    public static final Icon METHOD_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/method.svg", Html.INSIGHTS_WHITE);

    public static final Icon FILE = IconLoader.getIcon("/icons/file.svg", Icons.class.getClassLoader());
    public static final Icon FILE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/file.svg", Html.INSIGHTS_WHITE);
    public static final Icon EMPTY = AllIcons.General.InspectionsErrorEmpty;

    public static final Icon TELESCOPE = IconLoader.getIcon("/icons/telescope.svg", Icons.class.getClassLoader());
    public static final Icon TELESCOPE_BLUE_LIGHT_SHADE = colorizeVsCodeIcon("/icons/telescope.svg", Html.BLUE_LIGHT_SHADE);
    public static final Icon TELESCOPE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/telescope.svg", Html.INSIGHTS_WHITE);

    public static final Icon INTERFACE = IconLoader.getIcon("/icons/interface.svg", Icons.class.getClassLoader());
    public static final Icon INTERFACE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/interface.svg", Html.INSIGHTS_WHITE);
    public static final Icon TRACE_INTO_16 = AllIcons.Actions.TraceInto;
    public static final Icon STEP_OUT_16 = AllIcons.Actions.StepOut;
    public static final Icon QUESTION_MARK = AllIcons.General.QuestionDialog;
    public static final Icon BACK_WHITE = AllIcons.Actions.Back;
    public static final Icon BACK_BLACK = colorizeIntellijIconIcon(AllIcons.Actions.Back, Color.BLACK);
    public static final Icon FORWARD_WHITE = AllIcons.Actions.Forward;
    public static final Icon FORWARD_BLACK = colorizeIntellijIconIcon(AllIcons.Actions.Forward, Color.BLACK);
    public static final Icon EVENT = IconLoader.getIcon("/icons/event.svg", Icons.class.getClassLoader());
    public static final Icon EVENT_RED = colorizeVsCodeIcon("/icons/event.svg", Html.RED);




    public static class Insight {

        private Insight() {
        }

        public static final Icon INSIGHT_METHOD_SCOPE = METHOD_INSIGHTS_WHITE;
        public static final Icon INSIGHT_DOCUMENT_SCOPE = FILE_INSIGHTS_WHITE;
        public static final Icon INSIGHT_EMPTY_SCOPE = EMPTY;
        public static final Icon BOTTLENECK = loadAndScaleIcon("/icons/bottleneck.png", INSIGHT_ICON_SIZE);
        public static final Icon LOW_USAGE = loadAndScaleIcon("/icons/gauge_low.png", INSIGHT_ICON_SIZE);
        public static final Icon NORMAL_USAGE = loadAndScaleIcon("/icons/gauge_normal.png", INSIGHT_ICON_SIZE);
        public static final Icon HIGH_USAGE = loadAndScaleIcon("/icons/gauge_high.png", INSIGHT_ICON_SIZE);
        public static final Icon SLOW = loadAndScaleIcon("/icons/slow.png", INSIGHT_ICON_SIZE);

        //meaningful names for specific views,makes it easier to replace.
        public static final Icon HOTSPOT = loadAndScaleIcon("/icons/target.png", INSIGHT_ICON_SIZE);
        public static final Icon SPAN_GROUP_TITLE = TELESCOPE_INSIGHTS_WHITE;
        public static final Icon HTTP_GROUP_TITLE = INTERFACE_INSIGHTS_WHITE;
    }

    public static class Error {
        public static final Icon RAISED_HERE = STEP_OUT_16;
        public static final Icon HANDLED_HERE = TRACE_INTO_16;
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


    public static Icon colorizeIntellijIconIcon(@NotNull Icon icon, Color newColor) {
        return IconUtil.colorize(icon, newColor);
    }


}
