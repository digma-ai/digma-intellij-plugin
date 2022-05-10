package org.digma.intellij.plugin.model

data class MethodUnderCaret(
    val id: String,
    val name:String,
    val className: String,
    val fileUri: String
) {

    override fun toString(): String {
        val toString = "${id}\n" +
                       "${name}\n" +
                       "${className}\n" +
                       "${fileUri}"
        return toString;
    }
}
