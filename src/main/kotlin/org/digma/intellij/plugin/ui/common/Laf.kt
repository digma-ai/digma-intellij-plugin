package org.digma.intellij.plugin.ui.common

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil.isUnderDarcula
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.settings.SettingsChangeListener
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.DigmaUIUtil.digmaColorToHex
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.UIManager

/**
 * its not really a swing LAF,
 * just a class to manage the standard colors for labels that should look similar all over the plugin.
 */
object Laf : SettingsChangeListener {

    private var settings: SettingsState? = null

    fun setSettings(settings: SettingsState){
        this.settings = settings
        Icons.reload()
        settings.addChangeListener(this)
    }


    override fun settingsChanged(settingsState: SettingsState?) {
        //reload some resources if necessary , maybe icons
        Icons.reload()
    }

    fun isUsingSystemLAF(): Boolean{
        return if (settings == null)
            true
        else {
            settings!!.isUseSystemLAF
        }
    }


    fun panelsListBackground(): Color {

//        return EditorColorsManager.getInstance().schemeForCurrentUITheme.defaultBackground

        var default: Color = JBColor.DARK_GRAY
        if (isUnderDarcula()) {
            default = Color(38, 38, 38)

        }
        return JBColor.namedColor("Editor.background", default)
    }


    /**
     * returns the color selected for html labels.
     * the method does not considert isUsingSystemLAF,caller should.
     */
    fun getHtmlLabelColor():String{
        return if(settings == null)
            Html.CHINESE_SILVER
        else
            settings!!.htmlLabelColor
    }


    fun getHtmlLabelGrayedColor():String{
        return if(settings == null)
            Html.DARK_GRAY2
        else
            settings!!.grayedColor
    }

    fun getSwingLabelGrayedColor():Color{

        try {
            return Color.decode(getHtmlLabelGrayedColor())
        }catch(e: Exception){
            return Color.GRAY
        }
    }


    fun getNavigationButtonColor():String{
        return Html.LIGHT_WHITE
    }



   fun getInsightsIconsWhite():String{
       return if (isUsingSystemLAF())
           Colors.DEFAULT_SWING_LABEL_FOREGROUND_HEX
       else
           getHtmlLabelColor()
    }



    fun scalePanels(size: Int):Int{
        return if (settings != null && settings!!.scalePanels)
            JBUI.scale(size)
        else
            size
    }

    fun scaleBorders(size: Int):Int{
        return if (settings != null && settings!!.scaleBorders)
            JBUI.scale(size)
        else
            size
    }

    fun scaleIcons(size: Int): Int {
        return if (settings != null && settings!!.scaleIcons)
            JBUI.scale(size)
        else
           size
    }


    class Colors{
        companion object {
            val DEFAULT_SWING_LABEL_FOREGROUND: Color = UIManager.getColor("Label.foreground")?: JLabel().foreground
            val DEFAULT_SWING_LABEL_FOREGROUND_HEX: String = digmaColorToHex(DEFAULT_SWING_LABEL_FOREGROUND)
        }
    }

    class Sizes{
        companion object {
            @JvmStatic
            val INSIGHT_ICON_SIZE: Int = 32
        }
    }





    class Fonts{
        companion object {
            val DEFAULT_LABEL_FONT: Font = com.intellij.util.ui.UIUtil.getLabelFont()
        }
    }


}













