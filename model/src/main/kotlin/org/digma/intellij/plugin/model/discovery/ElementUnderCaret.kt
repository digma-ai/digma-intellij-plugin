package org.digma.intellij.plugin.model.discovery

//currently keep this interface internal. we only support MethodUnderCaret discovery.
internal interface ElementUnderCaret {
    val id: String
    val fileUri: String
    val type: ElementUnderCaretType
    val caretOffset: Int
    val isSupportedFile: Boolean

}

enum class ElementUnderCaretType {
    Method
}