package org.digma.intellij.plugin.model.discovery


open class CodeLessSpan(
    val spanId: String,
    val spanInstLibrary: String,
    val spanName: String,
    val spanDisplayName: String?,
    val methodId: String?,
    val functionNamespace: String?,
    val functionName: String?,
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CodeLessSpan

        if (spanId != other.spanId) return false
        if (spanInstLibrary != other.spanInstLibrary) return false
        if (spanName != other.spanName) return false
        if (methodId != other.methodId) return false
        if (functionNamespace != other.functionNamespace) return false
        return functionName == other.functionName
    }

    override fun hashCode(): Int {
        var result = spanId.hashCode()
        result = 31 * result + spanInstLibrary.hashCode()
        result = 31 * result + spanName.hashCode()
        result = 31 * result + (methodId?.hashCode() ?: 0)
        result = 31 * result + (functionNamespace?.hashCode() ?: 0)
        result = 31 * result + (functionName?.hashCode() ?: 0)
        return result
    }
}


class CodeLessSpanWithCodeLocation(
    spanId: String,
    spanInstLibrary: String,
    spanName: String,
    spanDisplayName: String?,
    methodId: String?,
    functionNamespace: String?,
    functionName: String?,
    val workspaceUri: Pair<String, Int>
) : CodeLessSpan(
    spanId,
    spanInstLibrary,
    spanName,
    spanDisplayName,
    methodId,
    functionNamespace,
    functionName
) {


}