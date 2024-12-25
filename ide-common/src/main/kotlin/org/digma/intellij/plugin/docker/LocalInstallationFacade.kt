package org.digma.intellij.plugin.docker

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

/**
 * This service manages single access to the local engine and logical operation requests.
 * never call these DockerService operations directly always through this facade.
 */
@Service(Service.Level.APP)
class LocalInstallationFacade {

    private val logger = Logger.getInstance(this::class.java)

    private enum class OP { INSTALL, UPGRADE, STOP, START, REMOVE }

    private val myLock = ReentrantLock(true)
    private val operationInProgress = AtomicReference<OP>(null)

    private val myResultTask = MyResultTask()


    companion object {
        @JvmStatic
        fun getInstance(): LocalInstallationFacade {
            return service<LocalInstallationFacade>()
        }
    }


    /*
        for any operation that is requested the flow is:
        * if nothing is running start the operation and add resultTask as consumer to the result.
        * if the same operation is requested just add resultTask as another result consumer, the caller will receive the same
            result as the caller that started the operation.
        * if an operation is running and another operation is requested reject the requested operation.
            this should actually never happen if UI behaves correctly.
     */


    fun installEngine(project: Project, resultTask: Consumer<String>) {

        if (service<DockerComposePersistenceFeatureService>().updateInProgress.get()) {
            Log.log(logger::trace, "installEngine rejected because DockerComposePersistenceFeature update in progress")
            resultTask.accept("install engine rejected because DockerComposePersistenceFeature update in progress")
            return
        }

        Log.log(logger::trace, "installEngine requested")
        if (isLocalEngineInstalled()) {
            Log.log(logger::trace, "installEngine rejected because already installed")
            resultTask.accept("install engine rejected because local engine is installed, please remove before reinstall")
            return
        }

        doOperation(OP.INSTALL, resultTask) {
            DockerService.getInstance().installEngine(project, myResultTask)
        }
    }

    fun upgradeEngine(project: Project, resultTask: Consumer<String>) {

        Log.log(logger::trace, "upgradeEngine requested")
        doOperation(OP.UPGRADE, resultTask) {
            DockerService.getInstance().upgradeEngine(project, myResultTask)
        }
    }

    fun stopEngine(project: Project, resultTask: Consumer<String>) {

        if (service<DockerComposePersistenceFeatureService>().updateInProgress.get()) {
            Log.log(logger::trace, "stopEngine rejected because DockerComposePersistenceFeature update in progress")
            resultTask.accept("stop engine rejected because DockerComposePersistenceFeature update in progress")
            return
        }

        Log.log(logger::trace, "stopEngine requested")
        doOperation(OP.STOP, resultTask) {
            DockerService.getInstance().stopEngine(project, myResultTask)
        }
    }

    fun startEngine(project: Project, resultTask: Consumer<String>) {

        if (service<DockerComposePersistenceFeatureService>().updateInProgress.get()) {
            Log.log(logger::trace, "startEngine rejected because DockerComposePersistenceFeature update in progress")
            resultTask.accept("start engine rejected because DockerComposePersistenceFeature update in progress")
            return
        }

        Log.log(logger::trace, "startEngine requested")
        if (isLocalEngineRunning(project)) {
            Log.log(logger::trace, "startEngine rejected because already running")
            resultTask.accept("start engine rejected because local engine is installed and running, please remove or stop before start")
            return
        }

        doOperation(OP.START, resultTask) {
            DockerService.getInstance().startEngine(project, myResultTask)
        }
    }

    fun removeEngine(project: Project, resultTask: Consumer<String>) {

        if (service<DockerComposePersistenceFeatureService>().updateInProgress.get()) {
            Log.log(logger::trace, "removeEngine rejected because DockerComposePersistenceFeature update in progress")
            resultTask.accept("remove engine rejected because DockerComposePersistenceFeature update in progress")
            return
        }

        Log.log(logger::trace, "removeEngine requested")
        doOperation(OP.REMOVE, resultTask) {
            DockerService.getInstance().removeEngine(project, myResultTask)
        }
    }


    private fun doOperation(op: OP, resultTask: Consumer<String>, block: () -> Unit) {

        Log.log(logger::trace, "$op requested, locking")
        myLock.withLock {
            when (operationInProgress.get()) {
                null -> {
                    Log.log(logger::trace, "$op requested, nothing is running , starting $op")
                    operationInProgress.set(op)
                    myResultTask.addConsumer(resultTask)
                    block.invoke()
                }

                op -> {
                    Log.log(logger::trace, "$op requested but already running, adding result consumer")
                    myResultTask.addConsumer(resultTask)
                }

                else -> {
                    //reject
                    Log.log(logger::trace, "$op requested but ${operationInProgress.get()} already running, rejecting $op")
                    resultTask.accept("$op engine rejected because ${operationInProgress.get()} is in progress")
                }
            }
        }
    }


    fun isInstallationInProgress(): Boolean {
        return operationInProgress.get() == OP.INSTALL
    }

    fun isAnyOperationRunning(): Boolean {
        return operationInProgress.get() != null
    }


    fun isLocalEngineInstalled(): Boolean {
        if (operationInProgress.get() == OP.INSTALL) {
            return false
        }
        return PersistenceService.getInstance().isLocalEngineInstalled()
    }


    fun isLocalEngineRunning(project: Project): Boolean {
        return isLocalEngineInstalled() && BackendConnectionMonitor.getInstance(project).isConnectionOk()
    }


    fun getDigmaInstallationStatus(project: Project): DigmaInstallationStatus {

        return when (operationInProgress.get()) {
            null -> {
                discoverActualRunningEngine(project)
            }

            OP.INSTALL, OP.START, OP.UPGRADE -> {
                DigmaInstallationStatus.NOT_RUNNING
            }

            else -> {
                discoverActualRunningEngine(project)
            }
        }
    }


    fun getCurrentDigmaInstallationStatusOnConnectionLost(): DigmaInstallationStatus {
        return discoverActualRunningEngine(false)
    }


    fun getCurrentDigmaInstallationStatusOnConnectionGained(): DigmaInstallationStatus {
        return discoverActualRunningEngine(true)
    }


    private inner class MyResultTask : Consumer<String> {

        private val myConsumers = mutableListOf<Consumer<String>>()

        fun addConsumer(consumer: Consumer<String>) {
            myConsumers.add(consumer)
        }

        override fun accept(result: String) {
            myLock.withLock {
                try {
                    Log.log(logger::trace, "got result from ${operationInProgress.get()}, exit value $result")
                    myConsumers.forEach {
                        it.accept(result)
                    }
                } finally {
                    Log.log(logger::trace, "completing operation ${operationInProgress.get()}")
                    operationInProgress.set(null)
                    myConsumers.clear()
                }
            }
        }
    }


}