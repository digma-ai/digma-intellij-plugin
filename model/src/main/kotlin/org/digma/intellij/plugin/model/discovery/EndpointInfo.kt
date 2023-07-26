package org.digma.intellij.plugin.model.discovery

data class EndpointInfo(
    override val id: String,
    val containingMethodId: String,
    val containingFileUri: String,
    // negative value means that have no offset. used for navigation
    val offsetAtFileUri: Int,
) : CodeObjectInfo {

    override fun idWithType(): String {
        return "endpoint:$id"
    }
}
