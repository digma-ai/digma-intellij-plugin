package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.execution.configurations.RunConfiguration

/**
 * Everything related to kotlin language that needs to access classes from kotlin plugin needs to go through
 * methods here which in turn call KotlinLanguageServiceSupplementary.
 * This class should never import any class from kotlin plugin, the only class that can import classes from kotlin
 * plugin is KotlinLanguageServiceSupplementaryImpl
 * every method here should expect null for KotlinLanguageServiceSupplementary because it is registered
 * conditionally if kotlin plugin is enabled.
 */
fun isKotlinRunConfiguration(configuration: RunConfiguration): Boolean {
    return KotlinLanguageServiceSupplementary.getInstance(configuration.project)?.isKotlinConfiguration(configuration) ?: false
}
