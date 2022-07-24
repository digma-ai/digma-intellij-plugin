package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Font
import javax.swing.Icon
import javax.swing.JLabel

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
            @JvmStatic val DEFAULT_LABEL_FOREGROUND: Color = JBColor("Label.foreground", JLabel().foreground)

            @JvmStatic val PLUGIN_BACKGROUND: JBColor = JBColor.namedColor("Plugins.background", JBColor.PanelBackground)

            //val LIST_ITEM_BACKGROUND: JBColor = JBColor(0xE6EEF7, 0x45494A)
            @JvmStatic val LIST_ITEM_BACKGROUND: JBColor = JBColor(Color(0, 0, 50, 15), Color(200, 200, 255, 20))
            @JvmStatic val TRANSPARENT: Color = Color(0, 0, 0, 0)
            @JvmStatic val BLUE_LIGHT_SHADE: Color = Color(0x8f90ff)
            @JvmStatic val ERROR_RED: Color = Color(0xf95959)      // same as in VS Code plugin
            @JvmStatic val ERROR_ORANGE: Color = Color(0xfdb44b)   // same as in VS Code plugin
            @JvmStatic val ERROR_GREEN: Color = Color(0x7dd87d)    // same as in VS Code plugin
            @JvmStatic val SIMPLE_ICON_COLOR: JBColor = JBColor(0x222222, 0xDDDDDD)
            @JvmStatic val GRAY: Color = Color(0x8A8A8A)
        }
    }

    class Icons{
        companion object{
            @JvmStatic val EMPTY: Icon = AllIcons.General.InspectionsErrorEmpty
            //@JvmStatic val METHOD: JBIcon = JBIcon(IconConsts.METHOD, IconConsts.METHOD_WHITE)
            @JvmStatic val METHOD: Icon = SvgIcon.withColor("/icons/method.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val FILE: Icon = SvgIcon.withColor("/icons/file.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val TELESCOPE: Icon = SvgIcon.withColor("/icons/telescope.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val TELESCOPE_BLUE_LIGHT_SHADE: Icon = SvgIcon.withColor("/icons/telescope.svg", Colors.BLUE_LIGHT_SHADE)
            @JvmStatic val INTERFACE: Icon = SvgIcon.withColor("/icons/interface.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val BACK: Icon = SvgIcon.withColor("/icons/arrow-left.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val FORWARD: Icon = SvgIcon.withColor("/icons/arrow-right.svg", Colors.DEFAULT_LABEL_FOREGROUND)
            @JvmStatic val EVENT_RED: Icon = SvgIcon.withColor("/icons/event.svg", Colors.ERROR_RED)
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













