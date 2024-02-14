package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.GlobalEditorScheme
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.icons.IconsUtil
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

    fun scaleSize(size: Int): Int {
        return JBUI.scale(size)
    }

    fun scaleBorders(size: Int): Int {
        return size
        //todo: need to consider if to scale borders
        //JBUI.scale(size)
    }


    //can be called from java code: Laf.INSTANCE.getColorHex(Laf.Colors.getPLUGIN_BACKGROUND())
    fun getColorHex(jbColor: JBColor): String {
        return jbColor.getHex()
    }


    class Colors {
        companion object {
            @JvmStatic
            val DEFAULT_LABEL_FOREGROUND: Color = JBColor.foreground()

            @JvmStatic
            val PLUGIN_BACKGROUND: JBColor = JBColor.namedColor("Plugins.background", JBColor.PanelBackground)

            @JvmStatic
            val EDITOR_BACKGROUND: JBColor = JBColor.namedColor("Editor.background", GlobalEditorScheme.getDefaultBackground())

            @JvmStatic
            val DROP_DOWN_HEADER_TEXT_COLOR: Color = Color(0x7C7C94)

            @JvmStatic
            val LIST_ITEM_BACKGROUND: JBColor = JBColor(Color(0, 0, 50, 15), Color(200, 200, 255, 20))

            //            val A: Color = JBColor(Gray._0, Gray.xBB)
            val A: Color = Gray._0

            @JvmStatic
            val TRANSPARENT: Color = Color(0, 0, 0, 0)

            @JvmStatic
            val BLUE_LIGHT_SHADE: Color = Color(0x8f90ff)

            @JvmStatic
            val RED_OF_MISSING: JBColor = JBColor(0xE00036, 0xF93967)

            @JvmStatic
            val LINK_TEXT: JBColor = JBColor(0x426DDA, 0xB9C2EB)

            @JvmStatic
            val ERROR_RED: Color = Color(0xf95959)      // same as in VS Code plugin

            @JvmStatic
            val ERROR_ORANGE: Color = Color(0xfdb44b)   // same as in VS Code plugin

            @JvmStatic
            val ERROR_GREEN: Color = Color(0x7dd87d)    // same as in VS Code plugin

            @JvmStatic
            val BUTTON_BACKGROUND: Color = Color(0x5154EC) // previous values: 0x4245D0, 0x3538CD

            @JvmStatic
            val BUTTON_FONT: Color = Color(0xE2E7FF)

            //@JvmStatic val SIMPLE_ICON_COLOR: JBColor = JBColor(0x222222, 0xDDDDDD)
            @JvmStatic
            val GRAY: Color = Color(0x8A8A8A)
            //the LIVE_BUTTON_BORDER colors should match the colors of the rect in LiveButtonFullIconDark and LiveButtonFullIconLight
//            @JvmStatic val LIVE_BUTTON_BORDER_DARK: Color = Color(0x414363)
//            @JvmStatic val LIVE_BUTTON_BORDER_LIGHT: Color = Color(0x8f90ff)
        }
    }

    class Icons {
        class General {
            companion object {
                @JvmStatic
                val HOME: Icon = IconsUtil.loadIcon("/icons/home.svg")

                @JvmStatic
                val HOME_DEFAULT_DARK: Icon = IconsUtil.loadIcon("/icons/home-default-dark.svg")

                @JvmStatic
                val HOME_HOVER_DARK: Icon = IconsUtil.loadIcon("/icons/home-hover-dark.svg")

                @JvmStatic
                val HOME_SELECTED_DARK: Icon = IconsUtil.loadIcon("/icons/home-selected-dark.svg")

                @JvmStatic
                val HOME_DEFAULT_LIGHT: Icon = IconsUtil.loadIcon("/icons/home-default-light.svg")

                @JvmStatic
                val HOME_HOVER_LIGHT: Icon = IconsUtil.loadIcon("/icons/home-hover-light.svg")

                @JvmStatic
                val HOME_SELECTED_LIGHT: Icon = IconsUtil.loadIcon("/icons/home-selected-light.svg")

                @JvmStatic
                val PROJECT_DARK: Icon = IconsUtil.loadIcon("/icons/project-dark.svg")

                @JvmStatic
                val PROJECT_LIGHT: Icon = IconsUtil.loadIcon("/icons/project-light.svg")

                //@JvmStatic val ARROW_UP: Icon = IconsUtil.loadIcon("/icons/arrow-up.svg")
                //@JvmStatic val ARROW_DOWN: Icon = IconsUtil.loadIcon("/icons/arrow-down.svg")
                //@JvmStatic val DIGMA_LOGO: Icon = IconsUtil.loadIcon("/icons/digma-logo.svg")
                //@JvmStatic val RELATED_INSIGHTS: Icon = IconsUtil.loadIcon("/icons/related-insights.svg")
//                @JvmStatic
//                val POINTER: Icon = IconsUtil.loadIcon("/icons/pointer.svg")

                @JvmStatic
                val SLACK: Icon = IconsUtil.loadIcon("/icons/slack.svg")

                @JvmStatic
                val TARGET: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/target-light.svg")
                else IconsUtil.loadIcon("/icons/target-dark.svg")

                @JvmStatic
                val TARGET10: Icon = IconsUtil.loadIcon("/icons/target10.svg")

                @JvmStatic
                val TARGET_PRESSED: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/target-pressed-light.svg")
                else IconsUtil.loadIcon("/icons/target-pressed-dark.svg")

                @JvmStatic
                val CODE_LOCATION_LINK: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/code-location-link-light.svg")
                else IconsUtil.loadIcon("/icons/code-location-link-dark.svg")

                @JvmStatic
                val ACTIVE_GREEN: Icon = IconsUtil.loadIcon("/icons/active-green.svg")
            }
        }

        class ErrorDetails {
            companion object {
                @JvmStatic
                val BACK: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/arrow-left-light.svg")
                else IconsUtil.loadIcon("/icons/arrow-left-dark.svg")

                @JvmStatic
                val FORWARD: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/arrow-right-light.svg")
                else IconsUtil.loadIcon("/icons/arrow-right-dark.svg")

                @JvmStatic
                val EVENT_RED: Icon = IconsUtil.loadIcon("/icons/event-red.svg")

                @JvmStatic
                val TELESCOPE_BLUE_LIGHT_SHADE: Icon = IconsUtil.loadIcon("/icons/telescope-blue.svg")
            }
        }

        class Environment {
            companion object {
                @JvmStatic
                val ENVIRONMENT_HAS_USAGE = IconsUtil.loadIcon("/icons/active-env.svg")

                @JvmStatic
                val ENVIRONMENT_HAS_NO_USAGE = IconsUtil.loadIcon("/icons/disabled-env.svg")
//                @JvmStatic
//                val NO_CONNECTION_ICON = IconsUtil.loadIcon("/icons/no-signal.svg")
            }
        }

        class Insight {
            companion object {
                // Scope icons
                @JvmStatic
                val EMPTY: Icon = AllIcons.General.InspectionsErrorEmpty

                @JvmStatic
                val METHOD: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/method-light.svg")
                else IconsUtil.loadIcon("/icons/method-dark.svg")

                @JvmStatic
                val FILE: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/scope-file-light.svg")
                else IconsUtil.loadIcon("/icons/scope-file-dark.svg")

                @JvmStatic
                val TELESCOPE: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/telescope-light.svg")
                else IconsUtil.loadIcon("/icons/telescope-dark.svg")

                @JvmStatic
                val INTERFACE: Icon = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/interface-light.svg")
                else IconsUtil.loadIcon("/icons/interface-dark.svg")

                //                @JvmStatic val MESSAGE: Icon = IconsUtil.loadAndColorizeIcon("/icons/message.svg", Colors.DEFAULT_LABEL_FOREGROUND)
                // Insight item icons
//                @JvmStatic val BOTTLENECK = IconsUtil.loadIcon("/icons/bottleneck.svg")
//                @JvmStatic val DURATION = IconsUtil.loadIcon("/icons/duration.svg")
//                @JvmStatic val ERRORS = IconsUtil.loadIcon("/icons/errors.svg")
//                @JvmStatic val HOTSPOT = IconsUtil.loadIcon("/icons/hotspot.svg")
//                @JvmStatic val LOW_USAGE = IconsUtil.loadIcon("/icons/traffic-low.svg")
//                @JvmStatic val N_PLUS_ONE = loadAndScaleIconByWidth("/icons/sql.png", BUTTON_SIZE_24)
//                @JvmStatic val NORMAL_USAGE = IconsUtil.loadIcon("/icons/traffic-normal.svg")
//                @JvmStatic val HIGH_USAGE = IconsUtil.loadIcon("/icons/traffic-high.svg")
//                @JvmStatic val WAITING_DATA = IconsUtil.loadIcon("/icons/sand-watch.svg")
//                @JvmStatic val SLOW = IconsUtil.loadIcon("/icons/snail.svg")
                @JvmStatic
                val THREE_DOTS = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/three-dots-light.svg")
                else IconsUtil.loadIcon("/icons/three-dots-dark.svg")

                @JvmStatic
                val REFRESH = if (JBColor.isBright())
                    IconsUtil.loadIcon("/icons/refresh-light.svg")
                else IconsUtil.loadIcon("/icons/refresh-dark.svg")
//                @JvmStatic val SCALE = IconsUtil.loadIcon("/icons/scale.svg")

//                @JvmStatic val QUESTION_MARK = AllIcons.General.QuestionDialog
//                @JvmStatic val SPAN_DURATION_DROPPED = loadAndScaleIconByWidth("/icons/dropped.png", 8)
//                @JvmStatic val SPAN_DURATION_ROSE = loadAndScaleIconByWidth("/icons/rose.png", 8)
            }
        }

        class Common {
            companion object {
                //                @JvmStatic val NoDataYetLight = IconsUtil.loadIcon("/icons/no_data_yet-light.svg")
//                @JvmStatic val NoDataYetDark = IconsUtil.loadIcon("/icons/no_data_yet-dark.svg")
//                @JvmStatic val NoObservabilityLight = IconsUtil.loadIcon("/icons/no_observability-light.svg")
//                @JvmStatic val NoObservabilityDark = IconsUtil.loadIcon("/icons/no_observability-dark.svg")
//                @JvmStatic val NoInsightsLight = IconsUtil.loadIcon("/icons/no_insights_light.svg")
//                @JvmStatic val NoInsightsDark = IconsUtil.loadIcon("/icons/no_insights_dark.svg")
                @JvmStatic
                val UpdateProductDark = IconsUtil.loadIcon("/icons/update-product-dark.svg")

                @JvmStatic
                val UpdateProductLight = IconsUtil.loadIcon("/icons/update-product-light.svg")

                //                @JvmStatic val DigmaLogo64 = loadAndScaleIconByWidth("/icons/digma.png", 64)
//                @JvmStatic val DigmaLogo16 = loadAndScaleIconByWidth("/icons/digma.png", 16)
                @JvmStatic
                val DigmaLogo = IconsUtil.loadIcon("/icons/digma.svg");

                @JvmStatic
                val FileDark = IconsUtil.loadIcon("/icons/file-dark.svg")

                @JvmStatic
                val FileLight = IconsUtil.loadIcon("/icons/file-light.svg")

                @JvmStatic
                val LoadingDark = IconsUtil.loadIcon("/icons/loader-anim-dark.svg")

                @JvmStatic
                val LoadingLight = IconsUtil.loadIcon("/icons/loader-anim-light.svg")

                @JvmStatic
                val ProcessingDark = IconsUtil.loadIcon("/icons/processing-dark.svg")

                @JvmStatic
                val ProcessingLight = IconsUtil.loadIcon("/icons/processing-light.svg")

                @JvmStatic
                val NoConnectionDark = IconsUtil.loadIcon("/icons/no-connection-dark.svg")

                @JvmStatic
                val NoConnectionLight = IconsUtil.loadIcon("/icons/no-connection-light.svg")

                @JvmStatic
                val NoErrorsDark = IconsUtil.loadIcon("/icons/no-errors-dark.svg")

                @JvmStatic
                val NoErrorsLight = IconsUtil.loadIcon("/icons/no-errors-light.svg")

                //These two icons LiveButtonFullIconDark and LiveButtonFullIconLight can be used as one icon
                // for the live view button, but we probably don't need to use them, it doesn't look good.
                // we use LiveIconDark and LiveIconLight plus regular text.
//                @JvmStatic val LiveButtonFullIconDark = IconsUtil.loadIcon("/icons/live-view-button-full-icon-dark.svg")
//                @JvmStatic val LiveButtonFullIconLight = IconsUtil.loadIcon("/icons/live-view-button-full-icon-light.svg")
//                @JvmStatic val LiveIconDark = IconsUtil.loadIcon("/icons/live-icon-dark.svg")
//                @JvmStatic val LiveIconLight = IconsUtil.loadIcon("/icons/live-icon-light.svg")
                @JvmStatic
                val NotificationsBellDark = IconsUtil.loadIcon("/icons/notification-bell-dark.svg")

                @JvmStatic
                val NotificationsBellDarkPressed = IconsUtil.loadIcon("/icons/notification-bell-dark-pressed.svg")

                @JvmStatic
                val NotificationsBellLight = IconsUtil.loadIcon("/icons/notification-bell-light.svg")

                @JvmStatic
                val NotificationsBellLightPressed = IconsUtil.loadIcon("/icons/notification-bell-light-pressed.svg")

                @JvmStatic
                val Dashboard: Icon = if (JBColor.isBright())
                    IconsUtil.loadAndScaleIconObjectByFactor("/icons/dashboard-light.svg", 2.0)
                else IconsUtil.loadAndScaleIconObjectByFactor("/icons/dashboard-dark.svg", 2.0)

                @JvmStatic
                val DashboardPressed: Icon = if (JBColor.isBright())
                    IconsUtil.loadAndScaleIconObjectByFactor("/icons/dashboard-light.svg", 1.6)
                else IconsUtil.loadAndScaleIconObjectByFactor("/icons/dashboard-dark.svg", 1.6)


                @JvmStatic
                val NavPrevDark: Icon = IconsUtil.loadAndScaleIconObjectByFactor("/icons/nav-prev-dark.svg", 2.0)

                @JvmStatic
                val NavPrevDarkPressed: Icon = IconsUtil.loadAndScaleIconObjectByFactor("/icons/nav-prev-dark.svg", 1.6)

                @JvmStatic
                val NavPrevLight: Icon = IconsUtil.loadAndScaleIconObjectByFactor("/icons/nav-prev-light.svg", 2.0)

                @JvmStatic
                val NavPrevLightPressed: Icon = IconsUtil.loadAndScaleIconObjectByFactor("/icons/nav-prev-light.svg", 1.6)
            }
        }

        class Misc {
            companion object {
                @JvmStatic
                val Quarkus = IconsUtil.loadIcon("/icons/quarkus.svg")

                @JvmStatic
                val SpringBoot = IconsUtil.loadIcon("/icons/spring-boot.svg")
            }
        }


    }

    class Sizes {
        companion object {
            //            @JvmStatic
//            val INSIGHT_ICON_SIZE_32: Int = 32
            const val PANEL_SIZE_32 = 32
            const val BUTTON_SIZE_32 = 32
            const val BUTTON_SIZE_24 = 24
//            const val BUTTON_SIZE_26 = 26
        }
    }

    class Fonts {
        companion object {
            val DEFAULT_LABEL_FONT: Font = com.intellij.util.ui.UIUtil.getLabelFont()
        }
    }

}
