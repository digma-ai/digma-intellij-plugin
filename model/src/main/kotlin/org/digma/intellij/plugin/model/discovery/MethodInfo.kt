package org.digma.intellij.plugin.model.discovery

data class MethodInfo(override val id: String,
                      val name: String,
                      val containingClass: String,
                      val containingNamespace: String,
                      val containingFileUri: String,
                      val spans: List<SpanInfo>  ) : CodeObjectInfo {


    fun getRelatedCodeObjectIds(): List<String> {
        val relatedIds: MutableList<String> = ArrayList()
        spans.forEach{
            relatedIds.add(it.idWithType())
        }
        return relatedIds
    }

    override fun idWithType(): String {
        return "method:$id"
    }
}