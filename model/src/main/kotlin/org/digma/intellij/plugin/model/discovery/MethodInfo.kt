package org.digma.intellij.plugin.model.discovery

data class MethodInfo(
    override val id: String, // CodeObjectId (without type (prefix of 'method:'))
    val name: String,
    val containingClass: String,
    val containingNamespace: String,
    val containingFileUri: String,
    val offsetAtFileUri: Int,
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

    fun nameWithParams():String{
        return name + getParamsPartFromId()
    }

    private fun getParamsPartFromId():String{
        val indexOf = id.indexOf('(')
        if (indexOf > 0){
            return id.substring(indexOf,id.length)
        }
        return ""
    }
}