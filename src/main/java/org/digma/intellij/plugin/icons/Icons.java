package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import org.digma.intellij.plugin.ui.common.Laf;

import javax.swing.*;

import static org.digma.intellij.plugin.icons.IconsUtil.colorizeVsCodeIcon;
import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByWidth;

@SuppressWarnings({"unused", "SameParameterValue"})
public final class Icons {

    private Icons() {
    }

    public static final Icon METHOD = IconLoader.getIcon("/icons/method.svg", Icons.class.getClassLoader());
    public static final Icon METHOD_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/method.svg", Laf.INSTANCE.getInsightsIconsWhite());

    public static final Icon FILE = IconLoader.getIcon("/icons/file.svg", Icons.class.getClassLoader());
    public static final Icon FILE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/file.svg", Laf.INSTANCE.getInsightsIconsWhite());
    public static final Icon EMPTY = AllIcons.General.InspectionsErrorEmpty;

    public static final Icon TELESCOPE = IconLoader.getIcon("/icons/telescope.svg", Icons.class.getClassLoader());
    public static final Icon TELESCOPE_BLUE_LIGHT_SHADE = colorizeVsCodeIcon("/icons/telescope.svg", Laf.Colors.Companion.getBLUE_LIGHT_SHADE());
    public static final Icon TELESCOPE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/telescope.svg", Laf.INSTANCE.getInsightsIconsWhite());

    public static final Icon INTERFACE = IconLoader.getIcon("/icons/interface.svg", Icons.class.getClassLoader());
    public static final Icon INTERFACE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/interface.svg", Laf.INSTANCE.getInsightsIconsWhite());

    //    public static final Icon TRACE_INTO_16 = AllIcons.Actions.TraceInto;
//    public static final Icon STEP_OUT_16 = AllIcons.Actions.StepOut;

    public static final Icon QUESTION_MARK = AllIcons.General.QuestionDialog;
    public static final Icon BACK_WHITE = colorizeVsCodeIcon("/icons/arrow-left.svg", Laf.INSTANCE.getNavigationButtonColor());
    public static final Icon BACK_BLACK = IconLoader.getIcon("/icons/arrow-left.svg", Icons.class.getClassLoader());
    public static final Icon FORWARD_WHITE = colorizeVsCodeIcon("/icons/arrow-right.svg", Laf.INSTANCE.getNavigationButtonColor());
    public static final Icon FORWARD_BLACK = IconLoader.getIcon("/icons/arrow-right.svg", Icons.class.getClassLoader());

    public static final Icon EVENT = IconLoader.getIcon("/icons/event.svg", Icons.class.getClassLoader());
    public static final Icon EVENT_RED = colorizeVsCodeIcon("/icons/event.svg", Laf.Colors.getERROR_RED());

    public static final Icon DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8);
    public static final Icon ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8);


    public static class Insight {

        private Insight() {
        }

        public static final Icon INSIGHT_METHOD_SCOPE = METHOD_INSIGHTS_WHITE;
        public static final Icon INSIGHT_DOCUMENT_SCOPE = FILE_INSIGHTS_WHITE;
        public static final Icon INSIGHT_EMPTY_SCOPE = EMPTY;
        public static final Icon BOTTLENECK = loadAndScaleInsightIcon("/icons/bottleneck.png");
        public static final Icon LOW_USAGE = loadAndScaleInsightIcon("/icons/gauge_low.png");
        public static final Icon NORMAL_USAGE = loadAndScaleInsightIcon("/icons/gauge_normal.png");
        public static final Icon HIGH_USAGE = loadAndScaleInsightIcon("/icons/gauge_high.png");
        public static final Icon SLOW = loadAndScaleInsightIcon("/icons/slow.png");
        public static final Icon WAITING_DATA = loadAndScaleInsightIcon("/icons/waiting-data.png");

        //meaningful names for specific views,makes it easier to replace.
        public static final Icon HOTSPOT = loadAndScaleInsightIcon("/icons/target.png");
        public static final Icon SPAN_GROUP_TITLE = TELESCOPE_INSIGHTS_WHITE;
        public static final Icon HTTP_GROUP_TITLE = INTERFACE_INSIGHTS_WHITE;

        public static final Icon SPAN_DURATION_DROPPED = DROPPED;
        public static final Icon SPAN_DURATION_ROSE = ROSE;
    }

//    public static class Error {
//        public static final Icon RAISED_HERE = STEP_OUT_16;
//        public static final Icon HANDLED_HERE = TRACE_INTO_16;
//    }

    private static Icon loadAndScaleInsightIcon(String path) {
        return loadAndScaleInsightIconByWidth(path);
    }


    private static Icon loadAndScaleInsightIconByWidth(String path) {
        int size = Laf.INSTANCE.scaleIcons(Laf.Sizes.getINSIGHT_ICON_SIZE() );
        return loadAndScaleIconObjectByWidth(path, size);
    }

    private static Icon loadAndScaleIconByWidth(String path,int width) {
        return loadAndScaleIconObjectByWidth(path, width);
    }

}
