package org.digma.intellij.plugin.ui.common

import org.digma.intellij.plugin.ui.common.Laf.Colors.Companion.DEFAULT_LABEL_FOREGROUND
import org.digma.intellij.plugin.ui.common.Laf.Fonts.Companion.DEFAULT_LABEL_FONT
import java.awt.Color
import java.awt.Font
import java.awt.Rectangle
import javax.swing.JTextPane

open class BaseCopyableLabel(val myText: String) : JTextPane() {

    protected fun construct(myText: String) {
        isEditable = false
        isOpaque = false
        background = null
        border = null
        putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        font = DEFAULT_FONT
        text = myText
        toolTipText = myText
        isFocusCycleRoot = false
        isFocusTraversalPolicyProvider = false
        foreground = DEFAULT_FOREGROUND
    }

    companion object {
        private var DEFAULT_FONT: Font = DEFAULT_LABEL_FONT
        private var DEFAULT_FOREGROUND: Color = DEFAULT_LABEL_FOREGROUND
    }


    //JTextPane causes a swing event that will make the scroll pane scroll to its position,
    //we don't want that never with this component , this override will disable it.
    override fun scrollRectToVisible(aRect: Rectangle?) {
        //disable scrolling
    }
}

class CopyableLabel(myText: String) : BaseCopyableLabel(myText) {
    init {
        construct(myText)
    }
}


class CopyableLabelHtml(myText: String) : BaseCopyableLabel(myText) {
    init {
        contentType = "text/html"
        construct(myText)
    }
}