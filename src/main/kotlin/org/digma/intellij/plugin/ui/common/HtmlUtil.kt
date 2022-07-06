package org.digma.intellij.plugin.ui.common

import org.digma.intellij.plugin.ui.common.Laf.Colors.Companion.DEFAULT_SWING_LABEL_FOREGROUND_HEX
import org.digma.intellij.plugin.ui.common.Laf.getHtmlLabelGrayedColor

//kind of templating for html text in labels and buttons where we want to have the same color and style all over.
//just supporting two types of colors, either not change the color and so the component will have its swing LAF color,
//or use a specific color for white.
//most methods here will build html with the default label white and gray colors based on what the Laf class defines.
//for different color use extra methods.


fun asHtmlNoBoby(value: String): String{
    return "<html>$value</html>"
}


fun asHtml(content: String): String {
    return "<html><body>${content}</body>"
}



fun buildBoldGrayRegularText(boldPart: String,grayedPart: String,regularPart: String): String{

    val firstPart = spanBold(boldPart)

    val secondPart = spanGrayed(grayedPart)

    val thirdPart = span(regularPart)

    return asHtml("$firstPart $secondPart $thirdPart")

}

//for link labels where we never change the color
fun buildLinkTextWithGrayedAndDefaultLabelColorPart(linkPart: String,grayedPart: String,regularPart: String): String{

    val firstPart = spanNoColor(linkPart)

    val secondPart = spanGrayed(grayedPart)

    val thirdPart = span(DEFAULT_SWING_LABEL_FOREGROUND_HEX,regularPart)

    return asHtml("$firstPart $secondPart $thirdPart")

}


fun buildLinkTextWithTitleAndGrayedComment(title: String,comment: String): String{

    val titlePart = spanNoColor(title)

    val commentPart = spanGrayed(comment)

    return asHtml("$titlePart<br>$commentPart")
}




fun buildTitleGrayedComment(title: String,comment: String): String{
    return buildTitleGrayedComment(title, comment,false)
}

fun buildBoldTitleGrayedComment(title: String,comment: String): String{
    return buildTitleGrayedComment(title, comment,true)
}

fun buildTitleGrayedComment(title: String,comment: String,bold: Boolean): String{
    val titlePart = if (bold){
        spanBold(title)
    }else{
        span(title)
    }

    val commentPart = spanGrayed(comment)

    return asHtml("$titlePart<br>$commentPart")
}




fun buildTitleItalicGrayedComment(title: String,comment: String): String{
    return buildTitleItalicGrayedComment(title, comment,false)
}

fun buildBoldTitleItalicGrayedComment(title: String,comment: String): String{
    return buildTitleItalicGrayedComment(title, comment,true)
}

fun buildTitleItalicGrayedComment(title: String,comment: String,bold: Boolean): String{
    val titlePart = if (bold){
        spanBold(title)
    }else{
        span(title)
    }

    val commentPart = spanItalicGrayed(italic(comment))

    return asHtml("$titlePart<br>$commentPart")
}


fun spanItalicGrayed(value:String):String{
    return spanGrayed(italic(value))
}


fun spanBold(value:String):String{
     return spanBoldNoColor(value)
}

fun span(value:String):String{
    return spanNoColor(value)
}


fun spanGrayed(value:String):String{
    return span(getHtmlLabelGrayedColor(),value)
}



private fun italic(value: String): String {
    return "<i>$value</i>"
}

private fun spanBoldNoColor(value: String): String {
    return "<span><b>$value</b></span>"
}

private fun spanNoColor(value: String): String {
    return "<span>$value</span>"
}


fun span(color: String, value: String): String {
    return span(color, value, false)
}

fun span(color: String, value: String, bold: Boolean): String {
    return if (bold) {
        "<span style=\"color:$color\"><b>$value</b></span>"
    } else {
        "<span style=\"color:$color\">$value</span>"
    }
}


fun wrapCentered(value: String): String {
    return asHtml("<center>${value}</center>")
}