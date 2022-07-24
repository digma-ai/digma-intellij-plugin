package org.digma.intellij.plugin.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import org.digma.intellij.plugin.ui.common.Laf;

import javax.swing.*;

import static org.digma.intellij.plugin.icons.IconsUtil.colorizeVsCodeIcon;
import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByWidth;

@SuppressWarnings({"unused", "SameParameterValue"})
public final class Icons {

    private Icons() {
    }

    public static final Icon QUESTION_MARK = AllIcons.General.QuestionDialog;
    public static final Icon DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8);
    public static final Icon ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8);


    public static class Insight {

        private Insight() {
        }

        //meaningful names for specific views,makes it easier to replace.
        public static final Icon BOTTLENECK = loadAndScaleInsightIcon("/icons/bottleneck.png");
        public static final Icon LOW_USAGE = loadAndScaleInsightIcon("/icons/gauge_low.png");
        public static final Icon NORMAL_USAGE = loadAndScaleInsightIcon("/icons/gauge_normal.png");
        public static final Icon HIGH_USAGE = loadAndScaleInsightIcon("/icons/gauge_high.png");
        public static final Icon SLOW = loadAndScaleInsightIcon("/icons/slow.png");
        public static final Icon WAITING_DATA = loadAndScaleInsightIcon("/icons/waiting-data.png");
        public static final Icon HOTSPOT = loadAndScaleInsightIcon("/icons/target.png");
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
