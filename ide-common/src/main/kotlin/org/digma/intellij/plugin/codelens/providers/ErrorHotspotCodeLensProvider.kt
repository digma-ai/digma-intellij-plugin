package org.digma.intellij.plugin.codelens.providers

import org.digma.intellij.plugin.codelens.DigmaCodeVisionProviderBase

class ErrorHotspotCodeLensProvider : DigmaCodeVisionProviderBase() {

    companion object {
        const val ID = "DigmaErrorHotspot"
    }

    override val id: String
        get() = ID

    override val name: String
        get() = "Digma Error Hotspot Method Hints"

    override val groupId: String
        get() = ID

}