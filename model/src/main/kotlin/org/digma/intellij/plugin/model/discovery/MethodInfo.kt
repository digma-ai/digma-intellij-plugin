package org.digma.intellij.plugin.model.discovery

import java.util.stream.Collectors

data class MethodInfo(
    override val id: String, // CodeObjectId (without type (prefix of 'method:'))
    val name: String,
    val containingClass: String,
    val containingNamespace: String, //namespace for c# is namespace, for java its the package
    val containingFileUri: String,
    val offsetAtFileUri: Int,
    val spans: List<SpanInfo>
) : CodeObjectInfo {

    companion object {
        fun removeType(objectId: String): String {
            if (objectId.startsWith("method:")) {
                return objectId.substringAfter("method:", objectId)
            }
            return objectId
        }
    }

    fun getRelatedCodeObjectIds(): List<String> {
        return spans.stream().map(SpanInfo::idWithType).collect(Collectors.toList())
    }

    override fun idWithType(): String {
        return "method:$id"
    }

    fun nameWithParams(): String {
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