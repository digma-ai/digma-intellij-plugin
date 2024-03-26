package org.digma.intellij.plugin.idea.buildsystem

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.buildsystem.BuildSystemHelper
import java.util.ServiceLoader

//Do not change to light service because it will always register.
// we want it to register only in Idea.
// see: org.digma.intellij-with-jvm.xml
@Suppress("LightServiceMigrationCode")
class BuildSystemChecker(private val project: Project) {

    private var buildSystemHelpers: List<BuildSystemHelper>

    init {
        //ServiceLoader doesn't work in intellij as it is in any other java application.
        //see https://youtrack.jetbrains.com/issue/IDEA-241229
        //so changing the context class loader for loading is the workaround
        val currentClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().setContextClassLoader(this::class.java.classLoader)
        buildSystemHelpers =
            ServiceLoader.load(BuildSystemHelper::class.java).stream().map { it.get() }.toList()
        Thread.currentThread().setContextClassLoader(currentClassLoader)
    }


    fun determineBuildSystem(module: Module?): BuildSystem {
        if (module == null) {
            return BuildSystem.INTELLIJ
        }

        val moduleProperties = ExternalSystemModulePropertyManager.getInstance(module)
        val externalSystemId = moduleProperties.getExternalSystemId()


        return externalSystemId?.let { systemId ->
            val buildSystemHelper = buildSystemHelpers.find { it.isBuildSystem(systemId) }
            buildSystemHelper?.getBuildSystem(systemId) ?: BuildSystem.INTELLIJ
        } ?: BuildSystem.INTELLIJ
    }

}