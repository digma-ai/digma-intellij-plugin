package org.digma.intellij.plugin.ui.common

import com.intellij.ui.components.JBTextArea
import io.github.parubok.text.multiline.MultilineLabel
import javax.swing.border.EmptyBorder

//fun multiLineLabel(text: String): MultilineLabel{
fun multiLineLabel(text: String): MultilineLabel{

//    val textArea = JBTextArea()
//    textArea.text = text
//    textArea.isEditable = false;
//    textArea.cursor = null;
//    textArea.isOpaque = false;
//    textArea.isFocusable = false;
////    textArea.lineWrap = true;
////    textArea.wrapStyleWord = true;
//    textArea.autoscrolls = true
//
//return textArea

    val label = MultilineLabel()
    label.text = text // set text - possibly requiring multiline presentation
    label.preferredWidthLimit = 1000 // the label's preferred width won't exceed 330 pixels
    label.lineSpacing = 1.2f // relative spacing between adjacent text lines
    label.maxLines = 4 // limit the label to 30 lines of text
    label.border = EmptyBorder(10, 5, 10, 5)
    return label
}