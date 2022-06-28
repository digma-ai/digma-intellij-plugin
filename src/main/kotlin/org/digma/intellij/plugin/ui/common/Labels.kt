package org.digma.intellij.plugin.ui.common

fun asHtml(content: String): String {
    return "<html><body>${content}</body>"
}

fun wrapCentered(content: String): String {
    return "<center>${content}</center>"
}


fun htmlSpanTitle():String{
    return Html.span(Html.INSIGHTS_TITLE)
}

fun htmlSpanWhite():String{
    return Html.span(Html.INSIGHTS_WHITE)
}

fun htmlSpanSmoked():String{
    return Html.span(Html.INSIGHTS_SMOKED)
}