package org.digma.intellij.plugin.idea.instrumentation

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.idea.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.buildsystem.JvmBuildSystemHelperService
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.discovery.WITH_SPAN_ANNOTATION_FQN
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.jetbrains.uast.UMethod
import kotlin.coroutines.coroutineContext

abstract class AbstractJvmInstrumentationProvider(protected val project: Project, protected val languageService: AbstractJvmLanguageService) :
    InstrumentationProvider {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    private val otelDependencyVersion = "1.26.0"
    private val otelCoordinates =
        UnifiedCoordinates("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-annotations", otelDependencyVersion)
    private val mapBuildSystem2Dependency: Map<BuildSystem, UnifiedDependency> = mapOf(
        BuildSystem.INTELLIJ to UnifiedDependency(otelCoordinates, "compile"),
        BuildSystem.MAVEN to UnifiedDependency(otelCoordinates, null),
        BuildSystem.GRADLE to UnifiedDependency(otelCoordinates, "implementation")
    )


    override suspend fun buildMethodObservabilityInfo(methodId: String): MethodObservabilityInfo {
        val uMethod = languageService.findUMethodByMethodCodeObjectId(methodId)
        coroutineContext.ensureActive()
        return uMethod?.let { method ->
            readAction {
                val containingModule = method.sourcePsi?.let { ModuleUtilCore.findModuleForPsiElement(it) }
                containingModule?.let { module ->
                    if (!hasNecessaryDependencies(project, module)) {
                        MethodObservabilityInfo(methodId, hasMissingDependency = true, canInstrumentMethod = false, hasAnnotation = false)
                    } else {
                        val annotationClassFqn = getAnnotationClassFqn(project, module)
                        val hasAnnotation = method.uAnnotations.any { uAnnotation -> uAnnotation.qualifiedName == annotationClassFqn }
                        MethodObservabilityInfo(
                            methodId,
                            hasMissingDependency = false,
                            canInstrumentMethod = true,
                            annotationClassFqn = annotationClassFqn,
                            hasAnnotation = hasAnnotation
                        )
                    }
                }
            }
        } ?: MethodObservabilityInfo(methodId, hasMissingDependency = false, canInstrumentMethod = false, hasAnnotation = false)
    }

    override suspend fun addObservabilityDependency(methodId: String) {

        val uMethod = languageService.findUMethodByMethodCodeObjectId(methodId)
        if (uMethod?.sourcePsi == null) {
            Log.warn(logger, "Failed to get PsiMethod from method id '{}'", methodId)
            return
        }

        withContext(Dispatchers.EDT) {
            @Suppress("UnstableApiUsage")
            val added = writeAction {
                addDependencyToOtelLib(uMethod, methodId)
            }
            if (added) {
                withContext(Dispatchers.Default) {
                    ProjectRefreshAction.Manager.refreshProject(project)
                }
            }
        }
    }

    override suspend fun addObservability(methodId: String) {

        val observabilityInfo = buildMethodObservabilityInfo(methodId)
        if (observabilityInfo.hasAnnotation) {
            return
        }

        val instrumented = withContext(Dispatchers.EDT) {
            instrumentMethod(observabilityInfo)
        }
        if (instrumented){
            withContext(Dispatchers.Default) {
                ProjectRefreshAction.Manager.refreshProject(project)
            }
        }
    }


    @Suppress("UnstableApiUsage")
    @RequiresWriteLock
    private fun addDependencyToOtelLib(uMethod: UMethod, methodId: String):Boolean {
        val module = uMethod.sourcePsi?.let {
            ModuleUtilCore.findModuleForPsiElement(it)
        }
        if (module == null) {
            Log.warn(logger, "Failed to add dependencies OTEL lib since could not lookup module by methodId='{}'", methodId)
            return false
        }

        if (isSpringBootAndMicrometer(project, module)) {
            return addDepsForSpringBootAndMicrometer(module)
        } else {
            val moduleBuildSystem = project.service<JvmBuildSystemHelperService>().determineBuildSystem(module)
            val dependencyLib = mapBuildSystem2Dependency[moduleBuildSystem]
            if (dependencyLib != null) {
                DependencyModifierService.Companion.getInstance(project).addDependency(module, dependencyLib)
                return true
            }
            return false
        }
    }


    private fun addDepsForSpringBootAndMicrometer(module: Module): Boolean {
        val modulesDepsService = ModulesDepsService.Companion.getInstance(project)
        val moduleExt = modulesDepsService.getModuleExt(module.name)
        if (moduleExt == null) {
            Log.log(
                logger::warn,
                "Failed add dependencies of Spring Boot Micrometer since could not lookup module ext by module name='{}'",
                module.name
            )
            return false
        }
        val project = module.project
        val springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.Companion.getInstance(project)
        springBootMicrometerConfigureDepsService.addMissingDependenciesForSpringBootObservability(moduleExt)
        return true
    }


    @RequiresReadLock(generateAssertion = false)
    private fun hasNecessaryDependencies(project: Project, module: Module): Boolean {
        return if (isSpringBootAndMicrometer(project, module)) {
            val moduleExt = ModulesDepsService.Companion.getInstance(project).getModuleExt(module.name)
            moduleExt?.let {
                ModulesDepsService.Companion.getInstance(project).isModuleHasNeededDependenciesForSpringBootWithMicrometer(moduleExt.metadata)
            } ?: false
        } else {
            val annotationClassFqn = WITH_SPAN_ANNOTATION_FQN
            val annotationPsiClass = JavaPsiFacade.getInstance(project).findClass(
                annotationClassFqn,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
            )
            annotationPsiClass != null
        }
    }


    private fun isSpringBootAndMicrometer(project: Project, module: Module): Boolean {
        return (ModulesDepsService.Companion.getInstance(project).isSpringBootModule(module)
                && SpringBootMicrometerConfigureDepsService.Companion.isSpringBootWithMicrometer())
    }

    private fun getAnnotationClassFqn(project: Project, module: Module): String =
        if (isSpringBootAndMicrometer(project, module)) {
            MicrometerTracingFramework.Companion.OBSERVED_FQN
        } else {
            WITH_SPAN_ANNOTATION_FQN
        }

}