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
) : CodeObjectInfo {

    private val innerSpans: MutableList<SpanInfo> = mutableListOf()

    val spans: List<SpanInfo>
        get() = innerSpans

    private val innerEndpoints: MutableList<EndpointInfo> = mutableListOf()

    val endpoints: List<EndpointInfo>
        get() = innerEndpoints

    /*
        Note about id:
        a method has a code object id, its language dependent. and usually this id is used to get insights from
        the backend and to match a method to insights when the cursor moves between methods.
        in java and C# only this one id is necessary.
        But in python we need to use few patterns for the id. so a method has a main id, but when sending insights request
        we need to send multiple patters that are based on the main id. and when matching method id when the cursor moves
        between methods we need to match to multiple patterns.
        So we have an AdditionalIdsProvider , the default for java and C# does nothing and returns an empty list.
        But PythonAdditionalIdsProvider returns multiple ids.
        and MethodInfo has two corresponding methods: allIdsWithType and allIdsWithoutType.
        care should be taken when using one or the other.

     */


    //we need AdditionalIdsProvider only for python. this default implementation returns empty list
    var additionalIdsProvider: AdditionalIdsProvider = object : AdditionalIdsProvider {
        override fun provideAdditionalIdsWithType(methodInfo: MethodInfo): List<String> {
            return listOf()
        }

        override fun provideAdditionalIdsWithoutType(methodInfo: MethodInfo): List<String> {
            return listOf()
        }
    }

    companion object {

        @JvmStatic
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

    fun hasRelatedCodeObjectIds(): Boolean {
        return spans.isNotEmpty() || endpoints.isNotEmpty()
    }

    //Don't use idWithType, use allIdsWithType or allIdsWithoutType
    override fun idWithType(): String {
        return "method:$id"
    }

    private fun additionalIdsWithType(): List<String> {
        return additionalIdsProvider.provideAdditionalIdsWithType(this)
    }

    private fun additionalIdsWithoutType(): List<String> {
        return additionalIdsProvider.provideAdditionalIdsWithoutType(this)
    }

    //for python, we need to send multiple ids,see PythonAdditionalIdsProvider
    fun allIdsWithType(): List<String> {
        return mutableListOf(idWithType()).plus(additionalIdsWithType())
    }

    fun allIdsWithoutType(): List<String> {
        return mutableListOf(id).plus(additionalIdsWithoutType())
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

    fun addSpan(theSpan: SpanInfo) {
        innerSpans.add(theSpan)
    }

    fun addSpans(theSpans: Collection<SpanInfo>) {
        innerSpans.addAll(theSpans)
    }

    fun addEndpoint(endpoint: EndpointInfo) {
        innerEndpoints.add(endpoint)
    }


    interface AdditionalIdsProvider {
        fun provideAdditionalIdsWithType(methodInfo: MethodInfo): List<String>
        fun provideAdditionalIdsWithoutType(methodInfo: MethodInfo): List<String>
    }
}