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
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.build.BuildSystemChecker.Companion.determineBuildSystem
import org.digma.intellij.plugin.idea.build.JavaBuildSystem
import org.digma.intellij.plugin.idea.deps.ModuleExt
import org.digma.intellij.plugin.idea.deps.ModulesDepsService
import org.digma.intellij.plugin.log.Log
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


    private val blackoutDurationSeconds =
        TimeUnit.MINUTES.toSeconds(1) // production value
//        TimeUnit.SECONDS.toSeconds(12) // use short period (few seconds) when debugging

    private val delayMilliseconds = TimeUnit.SECONDS.toMillis(5)

    private val periodMilliseconds =
        TimeUnit.MINUTES.toMillis(1) // production value is 5 minutes
//        TimeUnit.SECONDS.toMillis(12) // use short period (few seconds) when debugging

    private val timer = Timer()

    var affectedPanel: DigmaResettablePanel? = null // late init

    private var blackoutStopTime: LocalDateTime = LocalDateTime.now().minusMonths(3)

    private var stateHasQuarkusModulesWithoutOpenTelemetry = AtomicBoolean(false)

    init {
        val fetchTask = object : TimerTask() {
            override fun run() {
                try {
                    periodicAction()
                } catch (e: Exception) {
                    Log.warnWithException(logger, e, "Exception in periodicAction")
                    ErrorReporter.getInstance().reportError(project, "QuarkusConfigureDepsService.periodicAction", e)
                }
            }
        }

        timer.schedule(
            fetchTask, delayMilliseconds, periodMilliseconds
        )
    }

    override fun dispose() {
        timer.cancel()
    }

    fun periodicAction() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val quarkusModulesWithoutOpenTelemetry = modulesDepsService.getQuarkusModulesWithoutOpenTelemetry()
        if (quarkusModulesWithoutOpenTelemetry.isNotEmpty()) {
            stateHasQuarkusModulesWithoutOpenTelemetry.set(true)
        } else {
            stateHasQuarkusModulesWithoutOpenTelemetry.set(false)
        }

        EDT.ensureEDT {
            affectedPanel?.reset()
        }
    }

    private fun addDependenciesOfQuarkusOtelToRelevantModules() {
        val modulesDepsService = ModulesDepsService.getInstance(project)

        val quarkusModulesWithoutOpenTelemetry = modulesDepsService.getQuarkusModulesWithoutOpenTelemetry()

        quarkusModulesWithoutOpenTelemetry.forEach {
            addDependenciesOfQuarkusOtelTo(it)
        }
    }

    private fun addDependenciesOfQuarkusOtelTo(moduleExt: ModuleExt) {
        val module = moduleExt.module
        val moduleBuildSystem = determineBuildSystem(module)
        val dependencyLib = getQuarkusOtelDependency(moduleBuildSystem, moduleExt.metadata.quarkusVersion!!)

        val dependencyModifierService = DependencyModifierService.getInstance(project)
        dependencyModifierService.addDependency(module, dependencyLib)
    }

    @VisibleForTesting
    private fun isDuringBlackout(): Boolean {
        val now = LocalDateTime.now()
        return now < blackoutStopTime
    }

    fun shouldDisplayPanel(): Boolean {
        if (isDuringBlackout()) return false

        return stateHasQuarkusModulesWithoutOpenTelemetry.get()
    }

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

            periodicAction()
        }

        WriteAction.run<Exception> {
            addDependenciesOfQuarkusOtelToRelevantModules()
        }

        Backgroundable.executeOnPooledThread {
            ProjectRefreshAction.refreshProject(project)
        }
    }

}