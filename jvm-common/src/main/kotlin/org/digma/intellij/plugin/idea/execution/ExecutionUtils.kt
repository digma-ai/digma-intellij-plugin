package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.module.Module
import org.digma.intellij.plugin.idea.deps.ModuleMetadata
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.settings.SpringBootObservabilityMode


fun isOtelAgentTracingInSettings(): Boolean {
    return SettingsState.getInstance().springBootObservabilityMode == SpringBootObservabilityMode.OtelAgent
}

fun isMicrometerTracingInSettings(): Boolean {
    return SettingsState.getInstance().springBootObservabilityMode == SpringBootObservabilityMode.Micrometer
}

fun getModuleMetadata(module: Module?): ModuleMetadata? {
    if (module == null)
        return null

    val modulesDepsService = ModulesDepsService.getInstance(module.project)
    val moduleExt = modulesDepsService.getModuleExt(module.name) ?: return null
    return moduleExt.metadata
}


fun mapToFlatString(attributes: Map<String, String>): String {
    return attributes.entries.joinToString(",")
}


//returns a map from a flat string of property value like : myProp1=myValue1,myProp2=myValue2, etc.
fun stringToMap(flatString: String, delimiter: String = ",", propertyDelimiter: String = "="): MutableMap<String, String> {
    return try {
        flatString.split(delimiter).associate {

            val entry = it.split(propertyDelimiter, limit = 2)

            val pair: Pair<String, String> = if (entry.size == 1) {
                val left = entry[0]
                val right = ""
                Pair(left, right)
            } else {
                val left = entry[0]
                val right = entry[1]
                Pair(left, right)
            }
            pair.first to pair.second
        }.toMutableMap()
    } catch (e: Throwable) {
        mutableMapOf()
    }
}


