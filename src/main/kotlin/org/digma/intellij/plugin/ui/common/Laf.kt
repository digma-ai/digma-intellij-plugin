package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.GlobalEditorScheme
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.icons.IconsUtil
import org.digma.intellij.plugin.ui.common.Laf.Sizes.Companion.BUTTON_SIZE_24
import org.digma.intellij.plugin.ui.common.Laf.Sizes.Companion.INSIGHT_ICON_SIZE_32
import org.digma.intellij.plugin.ui.list.insights.ThreeDotsIcon
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
object Laf {

    fun panelsListBackground(): Color {
        return Colors.PLUGIN_BACKGROUND
    }

    fun getLabelGrayedColor(): Color {
        return Colors.GRAY
    }

    fun scalePanels(size: Int): Int {
        //todo:need to consider if to scale panels always
        return JBUI.scale(size)
    }

    fun scaleBorders(size: Int): Int {
        return size
        //todo: need to consider if to scale borders
        //JBUI.scale(size)
    }

    fun scaleIcons(size: Int): Int {
        //todo: need to consider if to scale icons always
        return JBUI.scale(size)
    }

    class Colors {
        companion object {
            @JvmStatic val DEFAULT_LABEL_FOREGROUND: Color = JBColor.foreground()
            @JvmStatic val PLUGIN_BACKGROUND: JBColor = JBColor.namedColor("Plugins.background", JBColor.PanelBackground)
            @JvmStatic val EDITOR_BACKGROUND: JBColor = JBColor.namedColor("Editor.background", GlobalEditorScheme.getDefaultBackground())
            @JvmStatic val DROP_DOWN_HEADER_TEXT_COLOR: Color = Color(0x7C7C94)
            @JvmStatic val LIST_ITEM_BACKGROUND: JBColor = JBColor(Color(0, 0, 50, 15), Color(200, 200, 255, 20))
            @JvmStatic val TRANSPARENT: Color = Color(0, 0, 0, 0)
            @JvmStatic val BLUE_LIGHT_SHADE: Color = Color(0x8f90ff)
            @JvmStatic val ERROR_RED: Color = Color(0xf95959)      // same as in VS Code plugin
            @JvmStatic val ERROR_ORANGE: Color = Color(0xfdb44b)   // same as in VS Code plugin
            @JvmStatic val ERROR_GREEN: Color = Color(0x7dd87d)    // same as in VS Code plugin
            @JvmStatic val BUTTON_BACKGROUND: Color = Color(0x3638CD) // suppose to be dark purple
            //@JvmStatic val SIMPLE_ICON_COLOR: JBColor = JBColor(0x222222, 0xDDDDDD)
            @JvmStatic val GRAY: Color = Color(0x8A8A8A)
        }
    }

    class Icons{
        class General{
            companion object {
                @JvmStatic val HOME: Icon = SvgIcon.asIs("/icons/home.svg")
                @JvmStatic val ARROW_UP: Icon = SvgIcon.asIs("/icons/arrow-up.svg")
                @JvmStatic val ARROW_DOWN: Icon = SvgIcon.asIs("/icons/arrow-down.svg")
                @JvmStatic val DIGMA_LOGO: Icon = SvgIcon.asIs("/icons/digma-logo.svg")
                @JvmStatic val RELATED_INSIGHTS: Icon = SvgIcon.asIs("/icons/related-insights.svg")
                @JvmStatic val POINTER: Icon = SvgIcon.asIs("/icons/pointer.svg")
                @JvmStatic val SLACK: Icon = SvgIcon.asIs("/icons/slack.svg")
            }
        }

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
                @JvmStatic val ENVIRONMENT_HAS_USAGE = SvgIcon.asIs("/icons/active-env.svg")
                @JvmStatic
                val ENVIRONMENT_HAS_NO_USAGE = SvgIcon.asIs("/icons/disabled-env.svg")
                @JvmStatic
                val NO_CONNECTION_ICON = SvgIcon.asIs("/icons/no-signal.svg")
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
                @JvmStatic val MESSAGE: Icon = SvgIcon.withColor("/icons/message.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                // Insight item icons
                @JvmStatic val BOTTLENECK = SvgIcon.asIs("/icons/bottleneck.svg")
                @JvmStatic val DURATION = SvgIcon.asIs("/icons/duration.svg")
                @JvmStatic val ERRORS = SvgIcon.asIs("/icons/errors.svg")
                @JvmStatic val HOTSPOT = SvgIcon.asIs("/icons/hotspot.svg")
                @JvmStatic val LOW_USAGE = SvgIcon.asIs("/icons/traffic-low.svg")
                @JvmStatic val N_PLUS_ONE = loadAndScaleIconByWidth("/icons/sql.png", BUTTON_SIZE_24)
                @JvmStatic val NORMAL_USAGE = SvgIcon.asIs("/icons/traffic-normal.svg")
                @JvmStatic val HIGH_USAGE = SvgIcon.asIs("/icons/traffic-high.svg")
                @JvmStatic val WAITING_DATA = SvgIcon.asIs("/icons/sand-watch.svg")
                @JvmStatic val SLOW = SvgIcon.asIs("/icons/snail.svg")
                @JvmStatic val THREE_DOTS = ThreeDotsIcon.asIs("/icons/three-dots.svg")
                @JvmStatic val REFRESH = ThreeDotsIcon.asIs("/icons/repeat.svg")
                @JvmStatic val SCALE = SvgIcon.asIs("/icons/scale.svg")

                @JvmStatic val QUESTION_MARK = AllIcons.General.QuestionDialog
                @JvmStatic val SPAN_DURATION_DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8)
                @JvmStatic val SPAN_DURATION_ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8)
            }
        }

        class Common {
            companion object {
                @JvmStatic val Loading = AllIcons.Actions.BuildLoadChanges
                @JvmStatic val NoDataYetLight = SvgIcon.asIs("/icons/no_data_yet-light.svg")
                @JvmStatic val NoDataYetDark = SvgIcon.asIs("/icons/no_data_yet-dark.svg")
                @JvmStatic val NoObservabilityLight = SvgIcon.asIs("/icons/no_observability-light.svg")
                @JvmStatic val NoObservabilityDark = SvgIcon.asIs("/icons/no_observability-dark.svg")
                @JvmStatic val SummaryEmptyLight = SvgIcon.asIs("/icons/summary_empty_light.svg")
                @JvmStatic val SummaryEmptyDark = SvgIcon.asIs("/icons/summary_empty_dark.svg")
                @JvmStatic val UpdateProductDark = SvgIcon.asIs("/icons/update-product-dark.svg")
                @JvmStatic val UpdateProductLight = SvgIcon.asIs("/icons/update-product-light.svg")
                @JvmStatic val Mascot64 = loadAndScaleIconByWidth("/icons/digma.png", 64)
                @JvmStatic val Mascot16 = loadAndScaleIconByWidth("/icons/digma.png", 16)
            }
        }

        companion object {
            private fun loadAndScaleInsightIcon(path: String): Icon {
                return loadAndScaleInsightIconByWidth(path)
            }

            private fun loadAndScaleInsightIconByWidth(path: String): Icon {
                val size = scaleIcons(INSIGHT_ICON_SIZE_32)
                return IconsUtil.loadAndScaleIconObjectByWidth(path, size)
            }

            private fun loadAndScaleIconByWidth(path: String, width: Int): Icon {
                return IconsUtil.loadAndScaleIconObjectByWidth(path, width)
            }
        }
    }

    class Sizes {
        companion object {
            @JvmStatic
            val INSIGHT_ICON_SIZE_32: Int = 32
            const val PANEL_SIZE_32 = 32
            const val BUTTON_SIZE_32 = 32
            const val BUTTON_SIZE_24 = 24
            const val BUTTON_SIZE_26 = 26
        }
    }

    class Fonts {
        companion object {
            val DEFAULT_LABEL_FONT: Font = com.intellij.util.ui.UIUtil.getLabelFont()
        }
    }

}
