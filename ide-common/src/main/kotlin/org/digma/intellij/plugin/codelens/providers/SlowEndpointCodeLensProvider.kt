package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class SlowEndpointCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaSlowEndpoint"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Slow Endpoint"

//    override val groupId: String
//        get() = ID

}