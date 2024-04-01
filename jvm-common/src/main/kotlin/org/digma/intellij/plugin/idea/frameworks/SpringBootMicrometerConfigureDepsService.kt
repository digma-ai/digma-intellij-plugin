package org.digma.intellij.plugin.idea.frameworks

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.collaboration.async.disposingScope
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.buildsystem.BuildSystem
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.buildsystem.BuildSystemChecker
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

//Do not change to light service because it will always register.
// we want it to register only in Idea.
// see: org.digma.intellij-with-jvm.xml
@Suppress("LightServiceMigrationCode")
class SpringBootMicrometerConfigureDepsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(SpringBootMicrometerConfigureDepsService::class.java)


        val MicrometerTracingBridgeOtelCoordinates = UnifiedCoordinates("io.micrometer", "micrometer-tracing-bridge-otel", "1.1.2")
        val DatasourceMicrometerSpringBoot = UnifiedCoordinates("net.ttddyy.observation", "datasource-micrometer-spring-boot", "1.0.3")
        val OtelExporterOtlpCoordinates = UnifiedCoordinates("io.opentelemetry", "opentelemetry-exporter-otlp", "1.26.0")
        val DigmaSpringBootMicrometerAutoconfCoordinates =
            UnifiedCoordinates("io.github.digma-ai", "digma-spring-boot-micrometer-tracing-autoconf", "0.7.7")

        @JvmStatic
        fun getInstance(project: Project): SpringBootMicrometerConfigureDepsService {
            return project.getService(SpringBootMicrometerConfigureDepsService::class.java)
        }

        @JvmStatic
        fun isSpringBootWithMicrometer(): Boolean =
            SettingsState.getInstance().springBootObservabilityMode == SpringBootObservabilityMode.Micrometer

        fun getSpringBootStarterActuatorDependency(javaBuildSystem: BuildSystem, springBootVersion: String): UnifiedDependency {
            val libCoordinates = UnifiedCoordinates("org.springframework.boot", "spring-boot-starter-actuator", springBootVersion)

            return buildUnifiedDependency(libCoordinates, javaBuildSystem)
        }

        fun getSpringBootStarterAopDependency(javaBuildSystem: BuildSystem, springBootVersion: String): UnifiedDependency {
            val libCoordinates = UnifiedCoordinates("org.springframework.boot", "spring-boot-starter-aop", springBootVersion)

            return buildUnifiedDependency(libCoordinates, javaBuildSystem)
        }

        fun buildUnifiedDependency(
            libCoordinates: UnifiedCoordinates,
            javaBuildSystem: BuildSystem,
            removeVersion: Boolean = true,
        ): UnifiedDependency {

            val coordinatesWithoutVersionIfNecessary =
                if (removeVersion) {
                    coordinatesWithoutVersion(libCoordinates)
                } else {
                    libCoordinates
                }

            return when (javaBuildSystem) {
                BuildSystem.MAVEN -> UnifiedDependency(coordinatesWithoutVersionIfNecessary, null)
                BuildSystem.GRADLE -> UnifiedDependency(coordinatesWithoutVersionIfNecessary, "implementation")
                //if not maven or gradle always add with version, probably not so relevant coz we will not get here
                // if it's not maven or gradle
                else -> UnifiedDependency(libCoordinates, "compile")
            }
        }

        private fun coordinatesWithoutVersion(orig: UnifiedCoordinates): UnifiedCoordinates {
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
        disposingScope().launch {
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
        try {
            val modulesDepsService = ModulesDepsService.getInstance(project)

            val springBootModules = modulesDepsService.getSpringBootModulesWithoutObservabilityDeps()

            springBootModules.forEach {
                addMissingDependenciesForSpringBootObservability(it)
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SpringBootMicrometerConfigureDepsService.addMissingDependenciesToRelevantModules", e)
        }
    }

    fun addMissingDependenciesForSpringBootObservability(moduleExt: ModuleExt) {
        val module = moduleExt.module
        val moduleBuildSystem = project.service<BuildSystemChecker>().determineBuildSystem(module)

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
        if (!moduleExt.metadata.hasDatasourceMicrometerSpringBoot) {
            uniDeps.add(buildUnifiedDependency(DatasourceMicrometerSpringBoot, moduleBuildSystem, false))
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

    fun buttonClicked() {
        // start blackout time that panel won't be display
        blackoutStopTime = LocalDateTime.now().plusSeconds(blackoutDurationSeconds)

        // making the panel disappear
        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            delay(500)
            affectedPanel?.reset()
        }

        // give some time for the user/system to make the desired update, and only then run the periodicAction
        @Suppress("UnstableApiUsage")
        disposingScope().launch {
            delay(TimeUnit.SECONDS.toMillis(blackoutDurationSeconds) + 500)
            try {
                periodicAction()
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "Exception in periodicAction")
                ErrorReporter.getInstance().reportError(project, "SpringBootMicrometerConfigureDepsService.periodicAction", e)
            }
        }

        try {
            WriteAction.run<Exception> {
                addMissingDependenciesToRelevantModules()
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("SpringBootMicrometerConfigureDepsService.buttonClicked.writeAction", e)
        }

        Backgroundable.executeOnPooledThread {
            ProjectRefreshAction.refreshProject(project)
        }
    }

}