package org.digma.intellij.plugin.common

fun normalizeExtendedObservabilityValue(value: String?): String? {
    return value?.let { nonNullVal ->
        val withoutNewLine = nonNullVal
            .replace(Regex("\n", RegexOption.LITERAL), "")
            .replace(Regex("\t", RegexOption.LITERAL), "")
            .replace(Regex(" ", RegexOption.LITERAL), "")
        val parts = withoutNewLine.split(';').map { it.trim() }.filter { it.isNotBlank() }
        parts.joinToString(";")
    }
}