package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project


/**
 * Why do we need KotlinLanguageServiceSupplementary ?
 * Everything related to kotlin language relies on the kotlin plugin.
 * but kotlin plugin can be disabled by user. if the plugin is disabled the kotlin support classes , psi , indexes
 * will not be available, the classes will not be in the IDE classpath. if our code tries to load any of those classes
 * it will crash.
 * our plugin is ready for classes not existing in the classpath, for example rider classes are not available in idea
 * and vice versa,and our code never access CSharpLanguageService directly, only through the LanguageService interface.
 * if we try to load CSharpLanguageService in idea the code will crash for non-existing classes.
 * same thing may happen with KotlinLanguageService if the kotlin plugin is disabled.
 * but with kotlin we do need to access kotlin plugin classes and services from various code fragments.
 * for example checking if a RunConfiguration is a KotlinRunConfiguration, if we try to load the class
 * KotlinRunConfiguration when kotlin plugin is disabled the code will crash.
 * So everything related to kotlin that needs to access classes from kotlin plugin should go through this service.
 * this interface class should not import any class from kotlin plugin, only KotlinLanguageServiceSupplementaryImpl
 * can import classes from kotlin plugin. it is registered conditionally if kotlin plugin is enabled.
 *
 *
 */
interface KotlinLanguageServiceSupplementary {

    companion object {

        fun getInstance(project: Project): KotlinLanguageServiceSupplementary? {
            return project.serviceOrNull<KotlinLanguageServiceSupplementary>()
        }
    }

    fun isKotlinConfiguration(configuration: RunConfiguration): Boolean

}