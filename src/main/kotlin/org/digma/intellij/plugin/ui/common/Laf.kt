package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.icons.IconsUtil
import org.digma.intellij.plugin.ui.common.Laf.Sizes.Companion.INSIGHT_ICON_SIZE
import java.awt.Color
import java.awt.Font
import javax.swing.Icon

/**
 * It's not really a swing LAF,
 * just a class to manage the standard colors for labels that should look similar all over the plugin.
 * usually we just need the default label color,for example when changing html to gray and then back to white
 * we need the default label foreground.
 * Laf is one of the few classes that can be singleton in intellij plugins, it's a shared object between all open
 * projects in the jvm and can not consider per-project settings.
 *
 */
object Laf  {

    fun panelsListBackground(): Color {
        return Colors.PLUGIN_BACKGROUND
    }

    fun getLabelGrayedColor():Color{
        return Colors.GRAY
    }

    fun scalePanels(size: Int):Int{
        //todo:need to consider if to scale panels always
        return JBUI.scale(size)
    }

    fun scaleBorders(size: Int):Int{
        return size
        //todo: need to consider if to scale borders
        //JBUI.scale(size)
    }

    fun scaleIcons(size: Int): Int {
        //todo: need to consider if to scale icons always
        return JBUI.scale(size)
    }

    class Colors{
        companion object {
            @JvmStatic val DEFAULT_LABEL_FOREGROUND: Color = JBColor.foreground()
            @JvmStatic val PLUGIN_BACKGROUND: JBColor = JBColor.namedColor("Plugins.background", JBColor.PanelBackground)
            @JvmStatic val LIST_ITEM_BACKGROUND: JBColor = JBColor(Color(0, 0, 50, 15), Color(200, 200, 255, 20))
            @JvmStatic val TRANSPARENT: Color = Color(0, 0, 0, 0)
            @JvmStatic val BLUE_LIGHT_SHADE: Color = Color(0x8f90ff)
            @JvmStatic val ERROR_RED: Color = Color(0xf95959)      // same as in VS Code plugin
            @JvmStatic val ERROR_ORANGE: Color = Color(0xfdb44b)   // same as in VS Code plugin
            @JvmStatic val ERROR_GREEN: Color = Color(0x7dd87d)    // same as in VS Code plugin
            //@JvmStatic val SIMPLE_ICON_COLOR: JBColor = JBColor(0x222222, 0xDDDDDD)
            @JvmStatic val GRAY: Color = Color(0x8A8A8A)
        }
    }

    class Icons{
        class ErrorDetails{
            companion object {
                @JvmStatic val BACK: Icon = SvgIcon.withColor("/icons/arrow-left.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                @JvmStatic val FORWARD: Icon = SvgIcon.withColor("/icons/arrow-right.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                @JvmStatic val EVENT_RED: Icon = SvgIcon.withColor("/icons/event.svg", Colors.ERROR_RED)
                @JvmStatic val TELESCOPE_BLUE_LIGHT_SHADE: Icon = SvgIcon.withColor("/icons/telescope.svg", Colors.BLUE_LIGHT_SHADE)
            }
        }
        class Environment {
            companion object {
                @JvmStatic val ENVIRONMENT_HAS_USAGE = loadAndScaleIconByWidth("/icons/used.png", 8)
                @JvmStatic val ENVIRONMENT_HAS_NO_USAGE = loadAndScaleIconByWidth("/icons/unused.png", 8)
            }
        }
        class Insight{
            companion object {
                // Scope icons
                @JvmStatic val EMPTY: Icon = AllIcons.General.InspectionsErrorEmpty
                @JvmStatic val METHOD: Icon = SvgIcon.withColor("/icons/method.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                @JvmStatic val FILE: Icon = SvgIcon.withColor("/icons/file.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                @JvmStatic val TELESCOPE: Icon = SvgIcon.withColor("/icons/telescope.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                @JvmStatic val INTERFACE: Icon = SvgIcon.withColor("/icons/interface.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                // Insight item icons
                @JvmStatic val QUESTION_MARK = AllIcons.General.QuestionDialog
                @JvmStatic val BOTTLENECK = loadAndScaleInsightIcon("/icons/bottleneck.png")
                @JvmStatic val LOW_USAGE = loadAndScaleInsightIcon("/icons/gauge_low.png")
                @JvmStatic val NORMAL_USAGE = loadAndScaleInsightIcon("/icons/gauge_normal.png")
                @JvmStatic val HIGH_USAGE = loadAndScaleInsightIcon("/icons/gauge_high.png")
                @JvmStatic val SLOW = loadAndScaleInsightIcon("/icons/slow.png")
                @JvmStatic val WAITING_DATA = loadAndScaleInsightIcon("/icons/waiting-data.png")
                @JvmStatic val HOTSPOT = loadAndScaleInsightIcon("/icons/target.png")
                @JvmStatic val SPAN_DURATION_DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8)
                @JvmStatic val SPAN_DURATION_ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8)
            }
        }
        companion object {
            private fun loadAndScaleInsightIcon(path: String): Icon {
                return loadAndScaleInsightIconByWidth(path)
            }

            private fun loadAndScaleInsightIconByWidth(path: String): Icon {
                val size = scaleIcons(INSIGHT_ICON_SIZE)
                return IconsUtil.loadAndScaleIconObjectByWidth(path, size)
            }

            private fun loadAndScaleIconByWidth(path: String, width: Int): Icon {
                return IconsUtil.loadAndScaleIconObjectByWidth(path, width)
            }
        }
    }

    class Sizes{
        companion object {
            @JvmStatic
            val INSIGHT_ICON_SIZE: Int = 32
            const val ERROR_SCORE_PANEL_SIZE = 32
            const val ERROR_DETAILS_BACK_BUTTON_SIZE = 32
            const val ERROR_DETAILS_NAVIGATION_BUTTON_SIZE = 24
        }
    }

    class Fonts{
        companion object {
            val DEFAULT_LABEL_FONT: Font = com.intellij.util.ui.UIUtil.getLabelFont()
        }
    }

}













