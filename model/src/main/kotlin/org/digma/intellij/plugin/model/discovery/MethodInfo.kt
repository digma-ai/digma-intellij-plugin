package org.digma.intellij.plugin.model.discovery

data class MethodInfo(
    override val id: String, // the ID is without parameters
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
        spans.forEach {
            relatedIds.add(it.idWithType())
        }
        return relatedIds
    }

    override fun idWithType(): String {
        return "method:$id"
    }

    // currently it is used only for errors
    fun idWithParams(): String {
        if (parameters.isNullOrEmpty()) {
            return idWithType()
        }
        val paramsAsStr = parameters.joinToString(
            prefix = "(", postfix = ")", separator = ",",
            transform = { methodParameter -> methodParameter.typeShortName() })
        return idWithType() + paramsAsStr
    }

}