package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.digma.intellij.plugin.ui.common.Html;
import org.digma.intellij.plugin.ui.common.Laf;

import javax.swing.*;

import static org.digma.intellij.plugin.icons.IconsUtil.colorizeVsCodeIcon;
import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByWidth;

//@SuppressWarnings("unused")
public final class Icons {

    private Icons() {
    }



//    public static int INSIGHT_ICON_SIZE = Laf.INSTANCE.scaleIcons(32);
    //    public static final double INSIGHT_ICON_SCALE_FACTOR = 0.07d;
    public static final int ERROR_SCORE_PANEL_SIZE = 32;//its here because it should be the same size as icons
    public static final int ERROR_DETAILS_BACK_BUTTON_SIZE = 32;//its here because it should be the same size as icons
    public static final int ERROR_DETAILS_NAVIGATION_BUTTON_SIZE = 32;//its here because it should be the same size as icons

    public static final Icon METHOD = IconLoader.getIcon("/icons/method.svg", Icons.class.getClassLoader());
    public static Icon METHOD_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/method.svg", Laf.INSTANCE.getInsightsIconsWhite());

    public static final Icon FILE = IconLoader.getIcon("/icons/file.svg", Icons.class.getClassLoader());
    public static Icon FILE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/file.svg", Laf.INSTANCE.getInsightsIconsWhite());
    public static final Icon EMPTY = AllIcons.General.InspectionsErrorEmpty;

    public static final Icon TELESCOPE = IconLoader.getIcon("/icons/telescope.svg", Icons.class.getClassLoader());
    public static final Icon TELESCOPE_BLUE_LIGHT_SHADE = colorizeVsCodeIcon("/icons/telescope.svg", Html.BLUE_LIGHT_SHADE);
    public static Icon TELESCOPE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/telescope.svg", Laf.INSTANCE.getInsightsIconsWhite());

    public static final Icon INTERFACE = IconLoader.getIcon("/icons/interface.svg", Icons.class.getClassLoader());
    public static Icon INTERFACE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/interface.svg", Laf.INSTANCE.getInsightsIconsWhite());
    //    public static final Icon TRACE_INTO_16 = AllIcons.Actions.TraceInto;
//    public static final Icon STEP_OUT_16 = AllIcons.Actions.StepOut;
    public static final Icon QUESTION_MARK = AllIcons.General.QuestionDialog;
    public static final Icon BACK_WHITE = colorizeVsCodeIcon("/icons/arrow-left.svg", Laf.INSTANCE.getNavigationButtonColor());
    public static final Icon BACK_BLACK = IconLoader.getIcon("/icons/arrow-left.svg", Icons.class.getClassLoader());
    public static final Icon FORWARD_WHITE = colorizeVsCodeIcon("/icons/arrow-right.svg", Laf.INSTANCE.getNavigationButtonColor());
    public static final Icon FORWARD_BLACK = IconLoader.getIcon("/icons/arrow-right.svg", Icons.class.getClassLoader());
    public static final Icon EVENT = IconLoader.getIcon("/icons/event.svg", Icons.class.getClassLoader());
    public static final Icon EVENT_RED = colorizeVsCodeIcon("/icons/event.svg", Html.RED);

    public static final Icon DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8);
    public static final Icon ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8);


    public static class Insight {

        private Insight() {
        }

        public static Icon INSIGHT_METHOD_SCOPE = METHOD_INSIGHTS_WHITE;
        public static Icon INSIGHT_DOCUMENT_SCOPE = FILE_INSIGHTS_WHITE;
        public static final Icon INSIGHT_EMPTY_SCOPE = EMPTY;
        public static Icon BOTTLENECK = loadAndScaleInsightIcon("/icons/bottleneck.png");
        public static Icon LOW_USAGE = loadAndScaleInsightIcon("/icons/gauge_low.png");

        public static Icon NORMAL_USAGE = loadAndScaleInsightIcon("/icons/gauge_normal.png");
        public static Icon HIGH_USAGE = loadAndScaleInsightIcon("/icons/gauge_high.png");
        public static Icon SLOW = loadAndScaleInsightIcon("/icons/slow.png");

        //meaningful names for specific views,makes it easier to replace.
        public static Icon HOTSPOT = loadAndScaleInsightIcon("/icons/target.png");
        public static Icon SPAN_GROUP_TITLE = TELESCOPE_INSIGHTS_WHITE;
        public static Icon HTTP_GROUP_TITLE = INTERFACE_INSIGHTS_WHITE;

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



//    public static Icon loadAndScaleInsightIconByFactor(String path) {
//        return loadAndScaleIconObjectByFactor(path, INSIGHT_ICON_SCALE_FACTOR);
//    }








    //This is a dirty class . still needs some work,mostly because we want to support refresh of scaling a colors.
    //we want to refresh the colors and scaling of some icons when settings change.
    //this is a simple mutable static implementation, probably better to implement some icon loader with cache and refresh


    static {
        reload();
    }


    public static void reload() {
        //this is only to enable refresh of colors when changing settings, they can be final but then a plugin restart
        // is required when changing colors

        METHOD_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/method.svg", Laf.INSTANCE.getInsightsIconsWhite());
        FILE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/file.svg", Laf.INSTANCE.getInsightsIconsWhite());
        TELESCOPE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/telescope.svg", Laf.INSTANCE.getInsightsIconsWhite());
        INTERFACE_INSIGHTS_WHITE = colorizeVsCodeIcon("/icons/interface.svg", Laf.INSTANCE.getInsightsIconsWhite());

        Insight.INSIGHT_METHOD_SCOPE = METHOD_INSIGHTS_WHITE;
        Insight.INSIGHT_DOCUMENT_SCOPE = FILE_INSIGHTS_WHITE;
        Insight.SPAN_GROUP_TITLE = TELESCOPE_INSIGHTS_WHITE;
        Insight.HTTP_GROUP_TITLE = INTERFACE_INSIGHTS_WHITE;


        Insight.BOTTLENECK = loadAndScaleInsightIcon("/icons/bottleneck.png");
        Insight.LOW_USAGE = loadAndScaleInsightIcon("/icons/gauge_low.png");
        Insight.NORMAL_USAGE = loadAndScaleInsightIcon("/icons/gauge_normal.png");
        Insight.HIGH_USAGE = loadAndScaleInsightIcon("/icons/gauge_high.png");
        Insight.SLOW = loadAndScaleInsightIcon("/icons/slow.png");
        Insight.HOTSPOT = loadAndScaleInsightIcon("/icons/target.png");

    }

}
