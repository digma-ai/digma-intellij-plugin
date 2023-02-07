package org.digma.intellij.plugin.editor

private var focusedDocumentName: String = ""

fun setFocusedDocumentName(fileName: String) {
    focusedDocumentName = fileName
}
fun getFocusedDocumentName(): String {
    return focusedDocumentName
}