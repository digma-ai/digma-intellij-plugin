package org.digma.intellij.plugin.model.discovery

data class SpanInfo(override val id: String,
                    val name: String,
                    val containingMethodId: String,
                    val containingFileUri: String) : CodeObjectInfo {


    override fun idWithType(): String {
        return "span:$id"
    }
}