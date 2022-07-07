package org.digma.intellij.plugin.model.discovery

data class MethodInfo(
    override val id: String, // CodeObjectId (without type (prefix of 'method:'))
    val name: String,
    val containingClass: String,
    val containingNamespace: String,
    val containingFileUri: String,
    val offsetAtFileUri: Int,
    val parameters: List<MethodParameter>,
    val spans: List<SpanInfo>
) : CodeObjectInfo {


    fun getRelatedCodeObjectIds(): List<String> {
        val relatedIds: MutableList<String> = ArrayList()
        relatedIds.add(idWithTypeButWithoutParams())
        spans.forEach {
            relatedIds.add(it.idWithType())
        }
        return relatedIds
    }

    override fun idWithType(): String {
        return "method:$id"
    }

    private fun idWithTypeButWithoutParams(): String {
        return removeParamsIfNeeded(idWithType())
    }

    private fun removeParamsIfNeeded(codeObjectId: String): String {
        val lastIndexOfOpeningParenthesis = codeObjectId.lastIndexOf('(')
        if (lastIndexOfOpeningParenthesis >= 0) {
            return codeObjectId.substring(0, lastIndexOfOpeningParenthesis)
        }
        return codeObjectId
    }

}