package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil.isUnderDarcula
import org.digma.intellij.plugin.ui.DigmaUIUtil.digmaColorToHex
import org.digma.intellij.plugin.ui.DigmaUIUtil.digmaDecodeColor
import org.digma.intellij.plugin.ui.common.Laf.Colors.Companion.SWING_GRAYED_LABEL_FOREGROUND
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
            default = Color(38, 38, 38)

        }
        return JBColor.namedColor("Editor.background", default)

        //todo: consider:
        //EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultBackground

    }



    fun getReadOnlyEditorBannerBackground():Color{
        return Swing.SHADE_YELLOW
    }


    fun getHtmlLabelGrayedColor():String{
        return Html.DARK_GRAY2
    }

    fun getSwingLabelGrayedColor():Color{
        return SWING_GRAYED_LABEL_FOREGROUND
    }


    fun getNavigationButtonColor():String{
        return Html.LIGHT_WHITE
    }


    fun getInsightsIconsWhite(): String {
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
            val DEFAULT_SWING_LABEL_FOREGROUND_HEX: String = digmaColorToHex(DEFAULT_SWING_LABEL_FOREGROUND)
            val SWING_GRAYED_LABEL_FOREGROUND: Color = digmaDecodeColor(getHtmlLabelGrayedColor(),Color.GRAY)
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













