package org.digma.intellij.plugin.protocol

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope

const val ACTION_PARAM_NAME = "action"
const val ACTION_SHOW_ASSET_PARAM_NAME = "showAsset"
const val CODE_OBJECT_ID_PARAM_NAME = "codeObjectId"
const val ENVIRONMENT_ID_PARAM_NAME = "environmentId"
const val ACTION_SHOW_ASSETS_TAB_PARAM_NAME = "showAssetsTab"

@Service(Service.Level.PROJECT)
class DigmaProtocolApi(val cs: CoroutineScope) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var mainAppInitialized = false

    fun setMainAppInitialized() {
        mainAppInitialized = true
        Log.log(logger::trace, "main app initialized , thread={}", Thread.currentThread().name)
    }


    /**
     * return null on success.
     * error message on failure
     */
    fun performAction(project: Project, parameters: Map<String, String>, waitForJcef: Boolean): String? {
        try {

            val action = getActionFromParameters(parameters) ?: return "DigmaProtocolCommand no action in request"

            Log.log(logger::trace, "perform action {}, thread {}", action, Thread.currentThread().name)

            return when (action) {
                ACTION_SHOW_ASSET_PARAM_NAME -> {
                    showAsset(project, parameters, waitForJcef)
                }

                ACTION_SHOW_ASSETS_TAB_PARAM_NAME -> {
                    showAssetTab(project, action, waitForJcef)
                }

                else -> {
                    "DigmaProtocolCommand unknown action in request $action"
                }
            }


        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("DigmaProtocolApi.performAction", e)
            return "DigmaProtocolCommand error $e"
        }
    }


    private fun showAsset(project: Project, parameters: Map<String, String>, waitForJcef: Boolean): String? {

        val codeObjectId = getCodeObjectIdFromParameters(parameters)
        val environmentId = getEnvironmentIdFromParameters(parameters)

        Log.log(
            logger::trace,
            "showing asset,  codeObjectId='{}', environment='{}', thread='{}'",
            codeObjectId,
            environmentId,
            Thread.currentThread().name
        )

        if (codeObjectId == null) {
            return "DigmaProtocolCommand no code object id in request"
        }

        cs.launch {

            if (waitForJcef) {
                waitForJcef()
            }

            val scope = SpanScope(codeObjectId)

            ScopeManager.getInstance(project).changeScope(scope, null, environmentId)
        }

        return null
    }


    private fun showAssetTab(project: Project, action: String, waitForJcef: Boolean): String? {
        cs.launch {

            if (waitForJcef) {
                waitForJcef()
            }

            project.messageBus.syncPublisher(ProtocolCommandEvent.PROTOCOL_COMMAND_TOPIC).protocolCommand(action)
        }
        return null
    }


    private suspend fun waitForJcef() {

        try {
            withTimeout(5000) {
                while (!mainAppInitialized) {
                    delay(100)
                }
            }

        } catch (e: TimeoutCancellationException) {
            //ignore
        }

        //wait another second , it seems to be necessary to let jcef completely initialize
        delay(1000)
    }

}