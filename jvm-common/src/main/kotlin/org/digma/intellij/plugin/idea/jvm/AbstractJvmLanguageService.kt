package org.digma.intellij.plugin.idea.jvm

import com.google.common.collect.ImmutableMap
import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.allowSlowOperation
import org.digma.intellij.plugin.idea.buildsystem.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.buildsystem.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.idea.psi.discovery.MicrometerTracingFramework
import org.digma.intellij.plugin.idea.psi.findMethodInClass
import org.digma.intellij.plugin.idea.psi.java.Constants
import org.digma.intellij.plugin.instrumentation.CanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.JvmCanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.MissingDependencyCause
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import java.util.Objects
import java.util.function.Consumer

abstract class AbstractJvmLanguageService(protected val project: Project) : LanguageService {

    protected val logger: Logger = Logger.getInstance(this::class.java)


    private val otelDependencyVersion = "1.26.0"
    private val otelCoordinates =
        UnifiedCoordinates("io.opentelemetry.instrumentation", "opentelemetry-instrumentation-annotations", otelDependencyVersion)
    private val mapBuildSystem2Dependency: ImmutableMap<JavaBuildSystem, UnifiedDependency>

    init {
        val builder: ImmutableMap.Builder<JavaBuildSystem, UnifiedDependency> = ImmutableMap.Builder()
        builder.put(JavaBuildSystem.UNKNOWN, UnifiedDependency(otelCoordinates, "compile"))
        builder.put(JavaBuildSystem.MAVEN, UnifiedDependency(otelCoordinates, null))
        builder.put(JavaBuildSystem.GRADLE, UnifiedDependency(otelCoordinates, "implementation"))
        mapBuildSystem2Dependency = builder.build()
    }


    //It's a different search for each jvm language
    abstract fun findClassByClassName(className: String): UClass?


    override fun findWorkspaceUrisForMethodCodeObjectIds(methodCodeObjectIds: List<String>): Map<String, Pair<String, Int>> {
        val workspaceUrls: MutableMap<String, Pair<String, Int>> = HashMap()

        methodCodeObjectIds.forEach(Consumer { methodId: String ->
            ReadActions.ensureReadAction {
                allowSlowOperation {
                    val uMethod = findMethodByMethodCodeObjectId(methodId)
                    uMethod?.getContainingUFile()?.sourcePsi?.let { psiFile ->
                        val url = PsiUtils.psiFileToUri(psiFile)
                        workspaceUrls[methodId] = Pair(url, uMethod.sourcePsi?.textOffset ?: 0)
                    }
                }
            }
        })

        return workspaceUrls
    }


    fun findMethodByMethodCodeObjectId(methodId: String?): UMethod? {
        if (methodId == null) return null

        if (!methodId.contains("\$_$")) {
            Log.log(logger::debug, "method id in findWorkspaceUrisForMethodCodeObjectIds does not contain \$_$ {}", methodId)
            return null
        }

        return ReadActions.ensureReadAction<UMethod> {
            if (!methodId.contains("\$_$")) {
                Log.log(logger::debug, "method id in netoFindPsiMethodByMethodCodeObjectId does not contain \$_$ {}", methodId)
                return@ensureReadAction null
            }

            //the code object id for inner classes separates inner classes name with $, but intellij index them with a dot
            val className = methodId.substring(0, methodId.indexOf("\$_$")).replace('$', '.')

            return@ensureReadAction allowSlowOperation<UMethod?> {
                val uClass = findClassByClassName(className)

                return@allowSlowOperation uClass?.let { cls ->
                    findMethodInClass(project, cls, methodId)
                }
            }
        }
    }


    override fun canInstrumentMethod(project: Project, methodId: String?): CanInstrumentMethodResult {

        if (methodId == null) {
            return CanInstrumentMethodResult.failure()
        }

        class MyRunnable : Runnable {
            val progressIndicator: ProgressIndicator = EmptyProgressIndicator()

            var result: CanInstrumentMethodResult? = null

            override fun run() {
                val uMethod = findMethodByMethodCodeObjectId(methodId)
                if (uMethod?.sourcePsi == null) {
                    Log.log(logger::warn, "Failed to get Method from method id '{}'", methodId)
                    result = CanInstrumentMethodResult.failure()
                    return
                }

                progressIndicator.checkCanceled()

                val psiFile = uMethod.getContainingUFile()
                if (psiFile == null || !isSupportedFile(project, psiFile.sourcePsi)) {
                    Log.log(logger::warn, "Method's file is not supported file (methodId: {})", methodId)
                    result = CanInstrumentMethodResult.failure()
                    return
                }

                progressIndicator.checkCanceled()

                val module = ModuleUtilCore.findModuleForPsiElement(uMethod.sourcePsi!!)
                if (module == null) {
                    Log.log(logger::warn, "Failed to get module from PsiMethod '{}'", methodId)
                    result = CanInstrumentMethodResult.failure()
                    return
                }

                progressIndicator.checkCanceled()

                var annotationClassFqn = Constants.WITH_SPAN_FQN
                var dependencyCause = Constants.WITH_SPAN_DEPENDENCY_DESCRIPTION
                if (isSpringBootAndMicrometer(module)) {
                    progressIndicator.checkCanceled()

                    annotationClassFqn = MicrometerTracingFramework.OBSERVED_FQN
                    dependencyCause = MicrometerTracingFramework.OBSERVED_DEPENDENCY_DESCRIPTION

                    val modulesDepsService = ModulesDepsService.getInstance(project)
                    val moduleExt = modulesDepsService.getModuleExt(module.name)
                    if (moduleExt == null) {
                        Log.log(logger::warn, "Failed to not lookup module ext by module name='{}'", module.name)
                        result = CanInstrumentMethodResult.failure()
                        return
                    }
                    val hasDeps = modulesDepsService.isModuleHasNeededDependenciesForSpringBootWithMicrometer(moduleExt.metadata)
                    if (!hasDeps) {
                        result = CanInstrumentMethodResult(MissingDependencyCause(dependencyCause))
                        return
                    }
                }

                progressIndicator.checkCanceled()

                val annotationPsiClass = JavaPsiFacade.getInstance(project).findClass(
                    annotationClassFqn,
                    GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
                )
                if (annotationPsiClass == null) {
                    Log.log(logger::warn, "Cannot find WithSpan PsiClass (methodId: {}) (module:{})", methodId, module)
                    result = CanInstrumentMethodResult(MissingDependencyCause(dependencyCause))
                    return
                }

                result = JvmCanInstrumentMethodResult(methodId, uMethod, annotationPsiClass, psiFile)
            }
        }


        return Retries.retryWithResultAndDefault({
            val myRunnable = MyRunnable()
            val success = ProgressManager.getInstance()
                .runInReadActionWithWriteActionPriority(myRunnable, myRunnable.progressIndicator)
            if (!success) {
                throw RuntimeException("canInstrumentMethod read action failed")
            }
            Objects.requireNonNullElseGet(myRunnable.result) { CanInstrumentMethodResult.failure() }
        }, Throwable::class.java, 50, 5, CanInstrumentMethodResult.failure())
    }


    protected fun isSpringBootAndMicrometer(module: Module): Boolean {
        val modulesDepsService = ModulesDepsService.getInstance(project)
        val springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.getInstance(project)

        return (modulesDepsService.isSpringBootModule(module)
                && springBootMicrometerConfigureDepsService.isSpringBootWithMicrometer())
    }


    @Suppress("UnstableApiUsage")
    override fun addDependencyToOtelLib(project: Project, methodId: String) {
        val module = getModuleOfMethodId(methodId)
        if (module == null) {
            Log.log(logger::warn, "Failed to add dependencies OTEL lib since could not lookup module by methodId='{}'", methodId)
            return
        }
        if (!isSpringBootAndMicrometer(module)) {
            val moduleBuildSystem = determineBuildSystem(module)
            val dependencyLib = mapBuildSystem2Dependency[moduleBuildSystem]

            val dependencyModifierService = DependencyModifierService.getInstance(project)

            if (dependencyLib != null) {
                dependencyModifierService.addDependency(module, dependencyLib)
            }
            return
        }

        // handling spring boot with micrometer tracing
        addDepsForSpringBootAndMicrometer(module)
    }


    private fun getModuleOfMethodId(methodCodeObjectId: String): Module? {
        val uMethod = findMethodByMethodCodeObjectId(methodCodeObjectId)
        if (uMethod?.sourcePsi == null) {
            Log.log(logger::warn, "Failed to get PsiMethod from method id '{}'", methodCodeObjectId)
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(uMethod.sourcePsi!!)
        if (module == null) {
            Log.log(logger::warn, "Failed to get module from PsiMethod '{}'", methodCodeObjectId)
            return null
        }

        return module
    }


    private fun addDepsForSpringBootAndMicrometer(module: Module) {
        val modulesDepsService = ModulesDepsService.getInstance(project)
        val moduleExt = modulesDepsService.getModuleExt(module.name)
        if (moduleExt == null) {
            Log.log(
                logger::warn,
                "Failed add dependencies of Spring Boot Micrometer since could not lookup module ext by module name='{}'",
                module.name
            )
            return
        }
        val project = module.project
        val springBootMicrometerConfigureDepsService = SpringBootMicrometerConfigureDepsService.getInstance(project)
        springBootMicrometerConfigureDepsService.addMissingDependenciesForSpringBootObservability(moduleExt)
    }
}