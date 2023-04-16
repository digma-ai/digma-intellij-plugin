package org.digma.intellij.plugin.ui.common

import java.awt.Font
import javax.swing.JButton
import javax.swing.JLabel



fun boldFonts(label: JLabel){
    val f: Font = label.font
    label.font = f.deriveFont(Font.BOLD)
}

fun boldFonts(button: JButton){
    val f: Font = button.font
    button.font = f.deriveFont(Font.BOLD)
}