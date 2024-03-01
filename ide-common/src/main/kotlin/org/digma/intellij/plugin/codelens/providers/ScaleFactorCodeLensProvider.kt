package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class ScaleFactorCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaScaleFactor"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Scale Factor"

//    override val groupId: String
//        get() = ID

}