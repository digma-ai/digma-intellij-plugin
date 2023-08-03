package org.digma.intellij.plugin.idea.frameworks

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.EdtInvocationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.idea.build.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.build.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModuleExt
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.settings.SpringBootObservabilityMode
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SpringBootMicrometerConfigureDepsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(SpringBootMicrometerConfigureDepsService::class.java)


        val MicrometerTracingBridgeOtelCoordinates = UnifiedCoordinates("io.micrometer", "micrometer-tracing-bridge-otel", "1.1.2")
        val OtelExporterOtlpCoordinates = UnifiedCoordinates("io.opentelemetry", "opentelemetry-exporter-otlp", "1.26.0")

        @JvmStatic
        fun getInstance(project: Project): SpringBootMicrometerConfigureDepsService {
            logger.warn("Getting instance of ${SpringBootMicrometerConfigureDepsService::class.simpleName}")
            return project.getService(SpringBootMicrometerConfigureDepsService::class.java)
        }

        fun getSpringBootStarterActuatorDependency(javaBuildSystem: JavaBuildSystem, springBootVersion: String): UnifiedDependency {
            val libCoordinates = UnifiedCoordinates("org.springframework.boot", "spring-boot-starter-actuator", springBootVersion)

            return buildUnifiedDependency(libCoordinates, javaBuildSystem)
        }

        fun buildUnifiedDependency(libCoordinates: UnifiedCoordinates, javaBuildSystem: JavaBuildSystem): UnifiedDependency {
            val dep: UnifiedDependency =
                when (javaBuildSystem) {
                    JavaBuildSystem.MAVEN -> UnifiedDependency(libCoordinates, null)
                    JavaBuildSystem.GRADLE -> UnifiedDependency(libCoordinates, "implementation")
                    else -> UnifiedDependency(libCoordinates, "compile")
                }

            return dep
        }
    }


    private val BlackoutDurationSeconds =
        TimeUnit.MINUTES.toSeconds(1) // production value
//        TimeUnit.SECONDS.toSeconds(12) // use short period (few seconds) when debugging

    private val DelayMilliseconds = TimeUnit.SECONDS.toMillis(5)

    private val PeriodMilliseconds =
        TimeUnit.MINUTES.toMillis(1) // production value is 5 minutes
//        TimeUnit.SECONDS.toMillis(12) // use short period (few seconds) when debugging

    private val timer = Timer()

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var blackoutStopTime: LocalDateTime = LocalDateTime.now().minusMonths(3)

    var stateHasSpringBootModulesWithoutObservability = AtomicBoolean(false)

    init {
        val fetchTask = object : TimerTask() {
            override fun run() {
                periodicAction()
            }
        }

        timer.schedule(
            fetchTask, DelayMilliseconds, PeriodMilliseconds
        )
    }

    override fun dispose() {
        timer.cancel()
    }

    fun periodicAction() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val springBootModulesWithoutObservability = modulesDepsService.getSpringBootModulesWithoutObservabilityDeps()
        if (!springBootModulesWithoutObservability.isNullOrEmpty()) {
            stateHasSpringBootModulesWithoutObservability.set(true)
        } else {
            stateHasSpringBootModulesWithoutObservability.set(false)
        }

        EdtInvocationManager.getInstance().invokeLater {
            affectedPanel!!.reset()
        }
    }

    protected fun addMissingDependenciesToRelevantModules() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val springBootModules = modulesDepsService.getSpringBootModulesWithoutObservabilityDeps()

        springBootModules.forEach {
            addMissingDependenciesForSpringBootObservability(it)
        }
    }

    protected fun addMissingDependenciesForSpringBootObservability(moduleExt: ModuleExt) {
        val module = moduleExt.module
        val moduleBuildSystem = determineBuildSystem(module)

        val uniDeps = mutableSetOf<UnifiedDependency>()
        if (!moduleExt.metadata.hasSpringBootStarterActuator) {
            uniDeps.add(getSpringBootStarterActuatorDependency(moduleBuildSystem, moduleExt.metadata.springBootVersion!!))
        }
        if (!moduleExt.metadata.hasMicrometerTracingBridgeOtel) {
            uniDeps.add(buildUnifiedDependency(MicrometerTracingBridgeOtelCoordinates, moduleBuildSystem))
        }
        if (!moduleExt.metadata.hasOtelExporterOtlp) {
            uniDeps.add(buildUnifiedDependency(OtelExporterOtlpCoordinates, moduleBuildSystem))
        }

        println("adding spring boot deps to module '${module.name}' deps: ${uniDeps}")

        val dependencyModifierService = DependencyModifierService.getInstance(project)

        uniDeps.forEach {
            dependencyModifierService.addDependency(module, it)
        }
    }

    @VisibleForTesting
    protected fun isDuringBlackout(): Boolean {
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
        blackoutStopTime = LocalDateTime.now().plusSeconds(BlackoutDurationSeconds)

        // making the panel disappear
        GlobalScope.launch {
            delay(500)
            affectedPanel?.reset()
        }

        // give some time for the user/system to make the desired update, and only then run the periodicAction
        GlobalScope.launch {
            delay(TimeUnit.SECONDS.toMillis(BlackoutDurationSeconds) + 500)

            periodicAction()
        }

        WriteAction.run<Exception> {
            addMissingDependenciesToRelevantModules()
        }

        ReadActions.ensureReadAction {
            ProjectRefreshAction.refreshProject(project)
        }
    }

}