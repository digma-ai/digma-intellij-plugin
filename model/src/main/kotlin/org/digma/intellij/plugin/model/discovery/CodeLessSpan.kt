package org.digma.intellij.plugin.model.discovery


open class CodeLessSpan(
    val spanId: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CodeLessSpan

        return spanId == other.spanId
    }

    override fun hashCode(): Int {
        return spanId.hashCode()
    }
}


class CodeLessSpanWithCodeLocation(
    spanId: String,
    val workspaceUri: Pair<String, Int>
) : CodeLessSpan(
    spanId
)