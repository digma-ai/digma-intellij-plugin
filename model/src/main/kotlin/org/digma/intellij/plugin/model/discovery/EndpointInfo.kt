package org.digma.intellij.plugin.model.discovery

data class EndpointInfo(
    override val id: String,
    val containingMethodId: String,
    val containingFileUri: String,
) : CodeObjectInfo {

    override fun idWithType(): String {
        return "endpoint:$id"
    }
}
