package org.digma.intellij.plugin.model.discovery

interface CodeObjectInfo {
    val id: String

    fun idWithType(): String

    companion object {

        fun extractMethodName(codeObjectId: String): String {
            return codeObjectId.split("\$_\$")[1]
        }

        fun extractFqnClassName(codeObjectId: String): String {
            return codeObjectId.split("\$_\$")[0]
        }
    }
}