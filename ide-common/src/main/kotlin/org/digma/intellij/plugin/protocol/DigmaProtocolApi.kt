package org.digma.intellij.plugin.protocol

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log

const val ACTION_ASSETS = "assets"

@Service(Service.Level.PROJECT)
class DigmaProtocolApi(val cs: CoroutineScope) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)

    fun performAction(project: Project, action: String, waitForJcef: Boolean) {
        try {

            Log.log(logger::trace, "perform action {}, thread {}", action, Thread.currentThread().name)

            cs.launch {
                if (waitForJcef) {
                    delay(5000)
                }
                project.messageBus.syncPublisher(ProtocolCommandEvent.PROTOCOL_COMMAND_TOPIC).protocolCommand(action)
            }

        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("DigmaProtocolApi.performAction", e)
        }
    }
}