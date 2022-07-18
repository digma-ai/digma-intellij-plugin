package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil.isUnderDarcula
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.UIManager

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
        var default: Color = JBColor.DARK_GRAY
        if (isUnderDarcula()) {
            default = Color(60, 63, 65)
        }
        return JBColor.namedColor("Plugins.background", JBColor.PanelBackground)

        //todo: consider:
        //EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultBackground

    }

    fun getLabelGrayedColor():Color{
        return DigmaColors.GRAY
    }

    fun getNavigationButtonColor():Color{
        return DigmaColors.SIMPLE_ICON_COLOR
    }


    fun getInsightsIconsWhite():Color {
        return Colors.DEFAULT_SWING_LABEL_FOREGROUND_HEX
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
            val DEFAULT_SWING_LABEL_FOREGROUND: Color = UIManager.getColor("Label.foreground")?: JLabel().foreground
            val DEFAULT_SWING_LABEL_FOREGROUND_HEX: Color = DEFAULT_SWING_LABEL_FOREGROUND
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













