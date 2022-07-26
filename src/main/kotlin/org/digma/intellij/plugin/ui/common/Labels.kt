package org.digma.intellij.plugin.ui.common

import java.awt.Font
import javax.swing.JLabel



fun boldFonts(label: JLabel){
    val f: Font = label.font
    label.font = f.deriveFont(Font.BOLD)
}