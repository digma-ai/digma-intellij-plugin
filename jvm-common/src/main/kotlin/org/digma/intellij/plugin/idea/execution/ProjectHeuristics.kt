package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.idea.deps.ModuleExt
import org.digma.intellij.plugin.idea.deps.ModulesDepsService

open class ProjectHeuristics(private val project: Project) {


    //todo: taking ModulesDepsService.getModuleExt()
    // which is updated every minute and in rare cases we'll get an outdated ModuleExt.
    // instead we can use ModulesDepsService.buildMetadata so it will always be up to date but will impact performance a bit
    private fun getModulesMetadata(): List<ModuleExt> {
        return ModuleManager.getInstance(project).modules.mapNotNull {
            ModulesDepsService.getInstance(project).getModuleExt(it.name)
        }
    }


    /**
     * check if the project has only micronaut modules and not other known frameworks like spring boot or quarkus
     */
    fun hasOnlyMicronautModules(): Boolean {

        val modulesMetaData = getModulesMetadata()

        return hasMicronaut(modulesMetaData) &&
                !hasSpringBoot(modulesMetaData) &&
                !hasQuarkus(modulesMetaData)
    }


    /**
     * check if the project has only spring boot modules and not other known frameworks like micronaut or quarkus
     */
    fun hasOnlySpringBootModules(): Boolean {
        val modulesMetaData = getModulesMetadata()

        return hasSpringBoot(modulesMetaData) &&
                !hasMicronaut(modulesMetaData) &&
                !hasQuarkus(modulesMetaData)

    }

    /**
     * check if the project has only quarkus modules and not other known frameworks like micronaut or spring boot
     */
    fun hasOnlyQuarkusModules(): Boolean {

        val modulesMetaData = getModulesMetadata()

        return hasQuarkus(modulesMetaData) &&
                !hasSpringBoot(modulesMetaData) &&
                !hasMicronaut(modulesMetaData)

    }


    private fun hasSpringBoot(modulesMetaData: List<ModuleExt>): Boolean {
        return modulesMetaData.any { it.metadata.hasSpringBoot() }
    }

    private fun hasMicronaut(modulesMetaData: List<ModuleExt>): Boolean {
        return modulesMetaData.any { it.metadata.hasMicronaut() }
    }

    private fun hasQuarkus(modulesMetaData: List<ModuleExt>): Boolean {
        return modulesMetaData.any { it.metadata.hasQuarkus() }
    }


}