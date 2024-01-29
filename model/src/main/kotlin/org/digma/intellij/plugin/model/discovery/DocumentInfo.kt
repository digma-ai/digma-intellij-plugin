package org.digma.intellij.plugin.model.discovery

data class DocumentInfo(
    val fileUri: String,
    val methods: MutableMap<String, MethodInfo>
) {

    fun addMethodInfo(key: String, value: MethodInfo) {
        methods[key] = value
    }

    fun getAllSpans(): List<SpanInfo> {
        return methods.map { entry -> entry.value.spans }.flatten()
    }

    fun getAllEndpoints(): List<EndpointInfo> {
        return methods.map { entry -> entry.value.endpoints }.flatten()
    }

}