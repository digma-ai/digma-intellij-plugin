package org.digma.intellij.plugin.common

class StringUtils {

    companion object {

        fun firstPart(str: String, maxLen: Int): String {
            if (maxLen >= str.length) {
                return str
            }
            return str.substring(0, maxLen)
        }

        fun evalBoolean(str: String): Boolean {
            val cleanVal = str.trim().lowercase()

            return when (cleanVal) {
                "true" -> true
                "yes" -> true
                else -> false
            }
        }
    }

}