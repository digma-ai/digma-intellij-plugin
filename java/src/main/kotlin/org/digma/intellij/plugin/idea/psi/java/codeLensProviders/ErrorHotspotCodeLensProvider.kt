package org.digma.intellij.plugin.idea.psi.java.codeLensProviders

import org.digma.intellij.plugin.idea.psi.java.JavaCodeVisionProvider

class ErrorHotspotCodeLensProvider: JavaCodeVisionProvider() {

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