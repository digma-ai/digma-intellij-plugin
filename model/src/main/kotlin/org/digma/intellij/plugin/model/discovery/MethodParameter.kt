package org.digma.intellij.plugin.model.discovery

data class MethodParameter(
    val typeFqn: String,
    val name: String
) {

    fun typeShortName(): String {
        if (typeFqn == null) {
            return "";
        }
        return typeFqn.split('.').last()
    }

}
