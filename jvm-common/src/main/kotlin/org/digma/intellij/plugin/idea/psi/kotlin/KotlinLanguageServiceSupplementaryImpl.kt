package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.execution.configurations.RunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration

class KotlinLanguageServiceSupplementaryImpl : KotlinLanguageServiceSupplementary {
    override fun isKotlinConfiguration(configuration: RunConfiguration): Boolean {
        return configuration is KotlinRunConfiguration
    }
}