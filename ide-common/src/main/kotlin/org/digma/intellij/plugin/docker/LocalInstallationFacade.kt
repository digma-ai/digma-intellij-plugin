package org.digma.intellij.plugin.docker

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
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

    private enum class OP { INSTALL, UPGRADE, STOP, START, REMOVE }

    private val myLock = ReentrantLock(true)
    private val operationInProgress = AtomicReference<OP>(null)

    private val myResultTask = MyResultTask()

    /*
        for any operation that is requested the flow is:
        * if nothing is running start the operation and add resultTask as consumer to the result.
        * if the same operation is requested just add resultTask as another result consumer, the caller will receive the same
            result as the caller that started the operation.
        * if an operation is running and another operation is requested reject the requested operation.
            this should actually never happen if UI behaves correctly.
     */


    fun installEngine(project: Project, resultTask: Consumer<String>) {

        if (DockerService.getInstance().isEngineRunning(project)) {
            resultTask.accept("install engine rejected because local engine is installed and running, please remove before reinstall")
            return
        }

        doOperation(OP.INSTALL, resultTask) {
            DockerService.getInstance().installEngine(project, myResultTask)
        }
    }

    fun upgradeEngine(project: Project, resultTask: Consumer<String>) {

        doOperation(OP.UPGRADE, resultTask) {
            DockerService.getInstance().upgradeEngine(project, myResultTask)
        }
    }

    fun stopEngine(project: Project, resultTask: Consumer<String>) {

        doOperation(OP.STOP, resultTask) {
            DockerService.getInstance().stopEngine(project, myResultTask)
        }
    }

    fun startEngine(project: Project, resultTask: Consumer<String>) {

        if (DockerService.getInstance().isEngineRunning(project)) {
            resultTask.accept("start engine rejected because local engine is installed and running, please remove stop before start")
            return
        }

        doOperation(OP.START, resultTask) {
            DockerService.getInstance().startEngine(project, myResultTask)
        }
    }

    fun removeEngine(project: Project, resultTask: Consumer<String>) {

        doOperation(OP.REMOVE, resultTask) {
            DockerService.getInstance().removeEngine(project, myResultTask)
        }
    }


    private fun doOperation(op: OP, resultTask: Consumer<String>, block: () -> Unit) {
        myLock.withLock {
            when (operationInProgress.get()) {
                null -> {
                    operationInProgress.set(op)
                    myResultTask.addConsumer(resultTask)
                    block.invoke()
                }

                op -> myResultTask.addConsumer(resultTask)
                else -> {
                    //reject
                    resultTask.accept("$op engine rejected because ${operationInProgress.get()} is in progress")
                }
            }
        }
    }


    private inner class MyResultTask : Consumer<String> {

        private val myConsumers = mutableListOf<Consumer<String>>()

        fun addConsumer(consumer: Consumer<String>) {
            myConsumers.add(consumer)
        }

        override fun accept(result: String) {
            myLock.withLock {
                operationInProgress.set(null)
                myConsumers.forEach {
                    it.accept(result)
                }
            }
        }
    }


}