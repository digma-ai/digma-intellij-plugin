package org.digma.intellij.plugin.model.discovery

import java.util.stream.Collectors
import java.util.stream.Stream

data class MethodInfo(
    override val id: String, // CodeObjectId (without type (prefix of 'method:'))
    val name: String,
    val containingClass: String,
    val containingNamespace: String, //namespace for c# is namespace, for java its the package
    val containingFileUri: String,
    val offsetAtFileUri: Int,
    val spans: List<SpanInfo>,
) : CodeObjectInfo {

    private val endpoints: MutableList<EndpointInfo> = mutableListOf()

    //we need AdditionalIdsProvider only for python. this default implementation returns empty list
    var additionalIdsProvider: AdditionalIdsProvider = object: AdditionalIdsProvider{
        override fun provideAdditionalIds(methodInfo: MethodInfo): List<String> {
            return listOf()
        }
    }

    companion object {
        fun removeType(objectId: String): String {
            if (objectId.startsWith("method:")) {
                return objectId.substringAfter("method:", objectId)
            }
            return objectId
        }
    }

    fun getRelatedCodeObjectIdsWithType(): List<String> {
        val spansStream = spans.stream().map(SpanInfo::idWithType)
        val endpointsStream = endpoints.stream().map(EndpointInfo::idWithType)

        return Stream.concat(spansStream, endpointsStream).collect(Collectors.toList())
    }

    fun getRelatedCodeObjectIds(): List<String> {
        val spansStream = spans.stream().map(SpanInfo::id)
        val endpointsStream = endpoints.stream().map(EndpointInfo::id)

        return Stream.concat(spansStream, endpointsStream).collect(Collectors.toList())
    }

    override fun idWithType(): String {
        return "method:$id"
    }

    fun additionalIdsWithType():List<String>{
        return additionalIdsProvider.provideAdditionalIds(this)
    }

    //for python, we need to send multiple ids,see PythonAdditionalIdsProvider
    fun allIds():List<String>{
        return mutableListOf(idWithType()).plus(additionalIdsWithType())
    }

    fun nameWithParams(): String {
        return name + getParamsPartFromId()
    }

    private fun getParamsPartFromId(): String {
        val indexOf = id.indexOf('(')
        if (indexOf > 0) {
            return id.substring(indexOf, id.length)
        }
        return ""
    }

    fun addEndpoint(endpoint: EndpointInfo) {
        endpoints.add(endpoint)
    }


    interface AdditionalIdsProvider{
        fun provideAdditionalIds(methodInfo: MethodInfo):List<String>
    }
}