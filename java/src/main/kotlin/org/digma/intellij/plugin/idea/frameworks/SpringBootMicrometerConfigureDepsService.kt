package org.digma.intellij.plugin.idea.frameworks

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.collaboration.async.DisposingScope
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.build.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.build.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModuleExt
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.settings.SpringBootObservabilityMode
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SpringBootMicrometerConfigureDepsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(SpringBootMicrometerConfigureDepsService::class.java)


        val MicrometerTracingBridgeOtelCoordinates = UnifiedCoordinates("io.micrometer", "micrometer-tracing-bridge-otel", "1.1.2")
        val OtelExporterOtlpCoordinates = UnifiedCoordinates("io.opentelemetry", "opentelemetry-exporter-otlp", "1.26.0")
        val DigmaSpringBootMicrometerAutoconfCoordinates =
            UnifiedCoordinates("io.github.digma-ai", "digma-spring-boot-micrometer-tracing-autoconf", "0.7.4")

        @JvmStatic
        fun getInstance(project: Project): SpringBootMicrometerConfigureDepsService {
            return project.getService(SpringBootMicrometerConfigureDepsService::class.java)
        }

        fun getSpringBootStarterActuatorDependency(javaBuildSystem: JavaBuildSystem, springBootVersion: String): UnifiedDependency {
            val libCoordinates = UnifiedCoordinates("org.springframework.boot", "spring-boot-starter-actuator", springBootVersion)

            return buildUnifiedDependency(libCoordinates, javaBuildSystem)
        }

        fun getSpringBootStarterAopDependency(javaBuildSystem: JavaBuildSystem, springBootVersion: String): UnifiedDependency {
            val libCoordinates = UnifiedCoordinates("org.springframework.boot", "spring-boot-starter-aop", springBootVersion)

            return buildUnifiedDependency(libCoordinates, javaBuildSystem)
        }

        fun buildUnifiedDependency(libCoordinates: UnifiedCoordinates, javaBuildSystem: JavaBuildSystem): UnifiedDependency {
            return buildUnifiedDependency(libCoordinates, javaBuildSystem, true)
        }

        fun buildUnifiedDependency(
            libCoordinates: UnifiedCoordinates,
            javaBuildSystem: JavaBuildSystem,
            removeVersionIfCan: Boolean,
        ): UnifiedDependency {
            val dep: UnifiedDependency =
                when (javaBuildSystem) {
                    JavaBuildSystem.MAVEN -> UnifiedDependency(libCoordinates, null)
                    JavaBuildSystem.GRADLE -> {
                        val newLibCoordinates =
                            if (!removeVersionIfCan) {
                                libCoordinates
                            } else {
                                coordsWithoutVersion(libCoordinates)
                            }
                        return UnifiedDependency(newLibCoordinates, "implementation")
                    }

                    else -> UnifiedDependency(libCoordinates, "compile")
                }

            return dep
        }

        private fun coordsWithoutVersion(orig: UnifiedCoordinates): UnifiedCoordinates {
            // version as empty string works well, while null value throws exception (both for maven and gradle)
            return UnifiedCoordinates(orig.groupId, orig.artifactId, "")
        }
    }


    private val blackoutDurationSeconds =
        TimeUnit.MINUTES.toSeconds(1) // production value

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var blackoutStopTime: LocalDateTime = LocalDateTime.now().minusMonths(3)

    private var stateHasSpringBootModulesWithoutObservability = AtomicBoolean(false)

    init {
        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {
            while (isActive) {
                try {

                    delay(TimeUnit.MINUTES.toMillis(1))
                    if (isActive) {
                        periodicAction()
                    }

                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "Exception in periodicAction")
                    ErrorReporter.getInstance().reportError(project, "SpringBootMicrometerConfigureDepsService.periodicAction", e)
                }
            }
        }
    }

    override fun dispose() {
        //nothing to do , used as disposable parent
    }

    private fun periodicAction() {

        if (project.isDisposed) return

        val modulesDepsService = ModulesDepsService.getInstance(project)

        val springBootModulesWithoutObservability = modulesDepsService.getSpringBootModulesWithoutObservabilityDeps()
        if (springBootModulesWithoutObservability.isNotEmpty()) {
            stateHasSpringBootModulesWithoutObservability.set(true)
        } else {
            stateHasSpringBootModulesWithoutObservability.set(false)
        }

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }

    private fun addMissingDependenciesToRelevantModules() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val springBootModules = modulesDepsService.getSpringBootModulesWithoutObservabilityDeps()

        springBootModules.forEach {
            addMissingDependenciesForSpringBootObservability(it)
        }
    }

    fun addMissingDependenciesForSpringBootObservability(moduleExt: ModuleExt) {
        val module = moduleExt.module
        val moduleBuildSystem = determineBuildSystem(module)

        val uniDeps = mutableSetOf<UnifiedDependency>()
        if (!moduleExt.metadata.hasSpringBootStarterActuator) {
            uniDeps.add(getSpringBootStarterActuatorDependency(moduleBuildSystem, moduleExt.metadata.springBootVersion!!))
        }
        if (!moduleExt.metadata.hasSpringBootStarterAop) {
            uniDeps.add(getSpringBootStarterAopDependency(moduleBuildSystem, moduleExt.metadata.springBootVersion!!))
        }
        if (!moduleExt.metadata.hasMicrometerTracingBridgeOtel) {
            uniDeps.add(buildUnifiedDependency(MicrometerTracingBridgeOtelCoordinates, moduleBuildSystem))
        }
        if (!moduleExt.metadata.hasOtelExporterOtlp) {
            uniDeps.add(buildUnifiedDependency(OtelExporterOtlpCoordinates, moduleBuildSystem))
        }
        if (!moduleExt.metadata.hasDigmaSpringBootMicrometerAutoconf) {
            uniDeps.add(buildUnifiedDependency(DigmaSpringBootMicrometerAutoconfCoordinates, moduleBuildSystem, false))
        }


        val dependencyModifierService = DependencyModifierService.getInstance(project)

        uniDeps.forEach {
            dependencyModifierService.addDependency(module, it)
        }
    }

    @VisibleForTesting
    private fun isDuringBlackout(): Boolean {
        val now = LocalDateTime.now()
        return now < blackoutStopTime
    }

    fun shouldDisplayPanel(): Boolean {
        if (isDuringBlackout()) return false
        if (!isSpringBootWithMicrometer()) return false

        return stateHasSpringBootModulesWithoutObservability.get()
    }

    fun isSpringBootWithMicrometer(): Boolean =
        SettingsState.getInstance().springBootObservabilityMode == SpringBootObservabilityMode.Micrometer

    fun buttonClicked() {
        // start blackout time that panel won't be display
        blackoutStopTime = LocalDateTime.now().plusSeconds(blackoutDurationSeconds)

        // making the panel disappear
        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {
            delay(500)
            affectedPanel?.reset()
        }

        // give some time for the user/system to make the desired update, and only then run the periodicAction
        @Suppress("UnstableApiUsage")
        DisposingScope(this).launch {
            delay(TimeUnit.SECONDS.toMillis(blackoutDurationSeconds) + 500)
            try {
                periodicAction()
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "Exception in periodicAction")
                ErrorReporter.getInstance().reportError(project, "SpringBootMicrometerConfigureDepsService.periodicAction", e)
            }
        }

        WriteAction.run<Exception> {
            addMissingDependenciesToRelevantModules()
        }

        Backgroundable.executeOnPooledThread {
            ProjectRefreshAction.refreshProject(project)
        }
    }

}