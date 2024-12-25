package org.digma.intellij.plugin.docker

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.BackendInfoHolder
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.File
import java.nio.file.Files
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@Service(Service.Level.APP)
class DockerComposePersistenceFeatureService {

    private val logger = Logger.getInstance(this::class.java)
    private val myLock = ReentrantLock(true)

    var updateInProgress = AtomicBooleanProperty(false)


    fun migrateToNewComposeFileLocation(project: Project) {

        myLock.withLock {

            val isFirstRunAfterPersistDockerCompose = PersistenceService.getInstance().isFirstRunAfterPersistDockerCompose()
            if (isFirstRunAfterPersistDockerCompose) {
                Log.log(logger::info, "first run after persist docker compose")
                PersistenceService.getInstance().setIsFirstRunAfterPersistDockerComposeDone()

                //nothing to do if local engine is not installed
                if (!PersistenceService.getInstance().isLocalEngineInstalled()) {
                    Log.log(logger::info, "local engine not installed, nothing to do")
                    return
                }


                //run with background task so user will see a status notification when that happens
                Backgroundable.runInNewBackgroundThread(project, "updating digma engine") {

                    try {
                        updateInProgress.set(true)

                        val eventProperties = mutableMapOf<String, Any>()

                        val runningEngine = discoverActualRunningEngine(project)
                        eventProperties["local engine installed"] = runningEngine.runningDigmaInstances.contains(DigmaInstallationType.localEngine)
                        Log.log(logger::info, "discovered running engine: {}", runningEngine)
                        val dockerComposeCmd = getDockerComposeCommand() ?: return@runInNewBackgroundThread
                        var oldComposeFileFound = false
                        val engine = Engine()

                        val oldDockerComposeDir = File(System.getProperty("java.io.tmpdir"), COMPOSE_FILE_DIR_NAME)
                        val oldDockerComposeFile = File(oldDockerComposeDir, COMPOSE_FILE_NAME)
                        if (oldDockerComposeFile.exists()) {
                            oldComposeFileFound = true
                            eventProperties["old compose file found"] = true
                            Log.log(logger::info, "old docker compose found")
                            Log.log(logger::info, "removing engine with old compose file")
                            engine.remove(project, oldDockerComposeFile, dockerComposeCmd, false)
                            Log.log(logger::info, "deleting old compose file")
                            Files.deleteIfExists(oldDockerComposeFile.toPath())
                            Files.deleteIfExists(oldDockerComposeDir.toPath())
                            waitForNoConnection(project)
                        } else {
                            eventProperties["old compose file found"] = false
                            Log.log(logger::info, "old docker compose not found")
                        }



                        if (runningEngine.runningDigmaInstances.contains(DigmaInstallationType.localEngine)) {
                            Log.log(logger::info, "local engine was running, starting it again with new compose file")

                            val newComposeFile = ComposeFileProvider().getComposeFile()

                            if (oldComposeFileFound) {
                                Log.log(logger::info, "running engine.up with new compose file")
                                engine.up(project, newComposeFile, dockerComposeCmd)
                                waitForConnection(project)
                            } else {
                                Log.log(logger::info, "running engine.down with new compose file")
                                engine.down(project, newComposeFile, dockerComposeCmd)
                                waitForNoConnection(project)
                                Log.log(logger::info, "running engine.up with new compose file")
                                engine.up(project, newComposeFile, dockerComposeCmd)
                                waitForConnection(project)
                            }
                        }

                        BackendInfoHolder.getInstance(project).updateOnCurrentThread()
                        ActivityMonitor.getInstance(project)
                            .registerCustomEvent("digma engine updated after docker compose persist feature", eventProperties)
                    }finally {
                        updateInProgress.set(false)
                    }
                }
            }
        }
    }

    private fun waitForNoConnection(project: Project) {

        repeat(24) { count ->
            if (BackendConnectionMonitor.getInstance(project).isConnectionError()) {
                return@repeat
            }
            try {
                Log.log(logger::warn, "waiting for no-connection {}", count)
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                //ignore
            }

            try {
                AnalyticsService.getInstance(project).environments
            } catch (e: Throwable) {
                //Log.warnWithException(logger, e, "error in waitForConnection")
                //ignore
            }
        }

        if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
            Log.log(logger::warn, "connection status still ok")
        } else {
            Log.log(logger::warn, "status is no connection")
        }
    }


    //run this to try and update the connection status as soon as possible, call any api that will succeed and update the status
    private fun waitForConnection(project: Project) {

        repeat(24) { count ->
            if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                return@repeat
            }

            try {
                Log.log(logger::warn, "waiting for connection {}", count)
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                //ignore
            }

            try {
                AnalyticsService.getInstance(project).environments
            } catch (e: Throwable) {
                //ignore
            }
        }


        if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
            Log.log(logger::warn, "status changed to connection ok")
        } else {
            Log.log(logger::warn, "still no connection status")
        }
    }

}