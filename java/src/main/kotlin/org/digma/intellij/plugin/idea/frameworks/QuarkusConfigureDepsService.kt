package org.digma.intellij.plugin.idea.frameworks

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.buildsystem.model.unified.UnifiedDependency
import com.intellij.externalSystem.DependencyModifierService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectRefreshAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.idea.build.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.build.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModuleExt
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.jetbrains.annotations.VisibleForTesting
import java.time.LocalDateTime
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class QuarkusConfigureDepsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(QuarkusConfigureDepsService::class.java)

        @JvmStatic
        fun getInstance(project: Project): QuarkusConfigureDepsService {
            return project.getService(QuarkusConfigureDepsService::class.java)
        }

        fun getQuarkusOtelDependency(javaBuildSystem: JavaBuildSystem, quarkusVersion: String): UnifiedDependency {
            val quarkusOtelCoordinates = UnifiedCoordinates("io.quarkus", "quarkus-opentelemetry", quarkusVersion)

            val dep: UnifiedDependency =
                when (javaBuildSystem) {
                    JavaBuildSystem.MAVEN -> UnifiedDependency(quarkusOtelCoordinates, null)
                    JavaBuildSystem.GRADLE -> UnifiedDependency(quarkusOtelCoordinates, "implementation")
                    else -> UnifiedDependency(quarkusOtelCoordinates, "compile")
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

    var stateHasQuarkusModulesWithoutOpenTelemetry = AtomicBoolean(false)

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

        val quarkusModulesWithoutOpenTelemetry = modulesDepsService.getQuarkusModulesWithoutOpenTelemetry()
        if (!quarkusModulesWithoutOpenTelemetry.isNullOrEmpty()) {
            stateHasQuarkusModulesWithoutOpenTelemetry.set(true)
        } else {
            stateHasQuarkusModulesWithoutOpenTelemetry.set(false)
        }

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }

    protected fun addDependenciesOfQuarkusOtelToRelevantModules() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val quarkusModulesWithoutOpenTelemetry = modulesDepsService.getQuarkusModulesWithoutOpenTelemetry()

        quarkusModulesWithoutOpenTelemetry.forEach {
            addDependenciesOfQuarkusOtelTo(it)
        }
    }

    protected fun addDependenciesOfQuarkusOtelTo(moduleExt: ModuleExt) {
        val module = moduleExt.module
        val moduleBuildSystem = determineBuildSystem(module)
        val dependencyLib = getQuarkusOtelDependency(moduleBuildSystem, moduleExt.metadata.quarkusVersion!!)

//        println("adding dep to module '${module.name}' quarkusOtel ${dependencyLib}")

        val dependencyModifierService = DependencyModifierService.getInstance(project)
        dependencyModifierService.addDependency(module, dependencyLib)
    }

    @VisibleForTesting
    protected fun isDuringBlackout(): Boolean {
        val now = LocalDateTime.now()
        return now < blackoutStopTime
    }

    fun shouldDisplayPanel(): Boolean {
        if (isDuringBlackout()) return false

        return stateHasQuarkusModulesWithoutOpenTelemetry.get()
    }

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
            addDependenciesOfQuarkusOtelToRelevantModules()
        }

        ReadActions.ensureReadAction {
            ProjectRefreshAction.refreshProject(project)
        }
    }

}