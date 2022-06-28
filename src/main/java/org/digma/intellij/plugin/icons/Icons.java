package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }

    private static final int INSIGHT_ICON_SIZE = 48;

    public static final Icon TOOL_WINDOW = loadAndScaleIcon("/icons/digma.png", 13);
    public static final Icon METHOD_32 = loadAndScaleIcon("/icons/insight/method.png", 32);
    public static final Icon DOCUMENT_32 = loadAndScaleIcon("/icons/insight/csharpfile.png", 32);
    public static final Icon EMPTY = AllIcons.General.InspectionsErrorEmpty;

    public static final Icon TELESCOPE_32 = loadAndScaleIcon("/icons/insight/telescope.png", 32);
    public static final Icon ENDPOINT_32 = loadAndScaleIcon("/icons/insight/endpoint.png", 32);
    public static final Icon TRACE_INTO_16 = AllIcons.Actions.TraceInto;
    public static final Icon STEP_OUT_16 = AllIcons.Actions.StepOut;
    public static final Icon QUESTION_MARK = AllIcons.General.QuestionDialog;
    public static final Icon BACK = AllIcons.Actions.Back;
    public static final Icon FORWARD = AllIcons.Actions.Forward;
    public static final Icon BACK_ROLLOVER = loadAndScaleIcon("/icons/back-rollover.png", 48);
    public static final Icon FORWARD_ROLLOVER = loadAndScaleIcon("/icons/forward-rollover.png", 48);
    public static final Icon RED_THUNDER = loadAndScaleIcon("/icons/thunder.png", 48);

    public static class Insight {

        private Insight() {
        }

        public static final Icon INSIGHT_METHOD_SCOPE = METHOD_32;
        public static final Icon INSIGHT_DOCUMENT_SCOPE = DOCUMENT_32;
        public static final Icon INSIGHT_EMPTY_SCOPE = EMPTY;
        public static final Icon BOTTLENECK = loadAndScaleIcon("/icons/insight/bottleneck.png", INSIGHT_ICON_SIZE);
        public static final Icon LOW_USAGE = loadAndScaleIcon("/icons/insight/gauge_low.png", INSIGHT_ICON_SIZE);
        public static final Icon NORMAL_USAGE = loadAndScaleIcon("/icons/insight/gauge_normal.png", INSIGHT_ICON_SIZE);
        public static final Icon HIGH_USAGE = loadAndScaleIcon("/icons/insight/gauge_high.png", INSIGHT_ICON_SIZE);
        public static final Icon SLOW = loadAndScaleIcon("/icons/insight/slow.png", INSIGHT_ICON_SIZE);

        //meaningful names for specific views,makes it easier to replace.
        public static final Icon HOTSPOT = loadAndScaleIcon("/icons/insight/target.png", INSIGHT_ICON_SIZE);
        public static final Icon SPAN_GROUP_TITLE = TELESCOPE_32;
        public static final Icon HTTP_GROUP_TITLE = ENDPOINT_32;
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


}
