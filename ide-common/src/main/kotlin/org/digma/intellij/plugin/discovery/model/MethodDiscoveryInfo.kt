package org.digma.intellij.plugin.discovery.model

class MethodDiscoveryInfo(
    val containingFile: FileDiscoveryInfo,
    val id: String,
    val name: String,
    val containingClass: String,
    val containingNamespace: String, //namespace for c# is namespace, for java its package
    val spans: MutableList<SpanDiscoveryInfo> = mutableListOf(),
    val endpoints: MutableList<EndpointDiscoveryInfo> = mutableListOf(),
){

    //todo: equals and HashCode and toString
}
