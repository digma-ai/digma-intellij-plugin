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

