package org.digma.intellij.plugin.model.discovery

data class EndpointInfo(
    override val id: String,
    val containingMethodId: String,
    val containingFileUri: String,
    //todo: currently textRange is necessary to detect if the caret is in the scope of ktor endpoint, so only really
    // necessary for ktor. if used for supporting other framework need to make sure the the text range provided is correct
    val textRange: TextRange,
    val framework: EndpointFramework,
) : CodeObjectInfo {

    override fun idWithType(): String {
        return "endpoint:$id"
    }
}


enum class EndpointFramework {
    Micronaut, JaxrsJavax, JaxrsJakarta, Grpc, SpringBoot, Ktor
}