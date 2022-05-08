package org.digma.intellij.plugin.model

data class MethodUnderCaret(
    val id: String,
    val className: String,
    val filePath: String
) {

    override fun toString(): String {
        var toString = "${id}\n" +
                       "${className}\n" +
                       "${filePath}"
        return toString;
    }
}
