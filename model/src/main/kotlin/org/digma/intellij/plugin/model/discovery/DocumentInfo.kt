package org.digma.intellij.plugin.model.discovery

//todo: maybe change fileUri to a URI type. when serializing to index it can be serialized/deserialized as string
data class DocumentInfo(
    val fileUri: String,
    val methods: MutableMap<String, MethodInfo>
) {

    fun addMethodInfo(key: String, value: MethodInfo) {
        methods[key] = value
    }
}