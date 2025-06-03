package org.digma.intellij.plugin.discovery.model

import org.digma.intellij.plugin.model.discovery.EndpointFramework

class EndpointDiscoveryInfo(val parentMethod: MethodDiscoveryInfo, val id: String, val offset: Int, val framework: EndpointFramework) {

    //todo: equals and HashCode and toString
}
