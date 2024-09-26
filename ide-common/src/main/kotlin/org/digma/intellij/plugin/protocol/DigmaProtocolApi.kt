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
import org.digma.intellij.plugin.common.objectToJsonNode
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scope.ScopeContext
import org.digma.intellij.plugin.scope.ScopeManager
import org.digma.intellij.plugin.scope.SpanScope
import kotlin.time.Duration.Companion.seconds

const val ACTION_PARAM_NAME = "action"
const val ACTION_SHOW_ASSET_PARAM_NAME = "showAsset"
const val CODE_OBJECT_ID_PARAM_NAME = "codeObjectId"
const val ENVIRONMENT_ID_PARAM_NAME = "environmentId"
const val ACTION_SHOW_ASSETS_TAB_PARAM_NAME = "showAssetsTab"
const val TARGET_TAB_PARAM_NAME = "targetTab"

@Service(Service.Level.PROJECT)
class DigmaProtocolApi(val cs: CoroutineScope) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var mainAppInitialized = false

    fun setMainAppInitialized() {
        if(!mainAppInitialized) {
            Log.log(logger::trace, "main app initialized , thread={}", Thread.currentThread().name)
            mainAppInitialized = true
        }
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
        val targetTab = getTargetTabFromParameters(parameters)

        Log.log(
            logger::trace,
            "showing asset,  codeObjectId='{}', environment='{}',targetTab='{}' thread='{}'",
            codeObjectId,
            environmentId,
            targetTab,
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
            val contextPayload = objectToJsonNode(CustomUrlScopeContextPayload(targetTab))
            val scopeContext = ScopeContext("IDE/CUSTOM_PROTOCOL_LINK_CLICKED", contextPayload)

            Log.log(
                logger::trace,
                "calling ScopeManager.changeScope with scope='{}',scopeContext='{}',environmentId='{}', thread='{}'",
                scope,
                scopeContext,
                environmentId,
                Thread.currentThread().name)

            ScopeManager.getInstance(project).changeScope(scope, scopeContext, environmentId)
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

        if (mainAppInitialized){
            return
        }

        Log.log(logger::trace,"waiting for jcef")

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
        val extraWaitTime = 5.seconds
        Log.log(logger::trace,"waiting another $extraWaitTime")
        delay(extraWaitTime.inWholeMilliseconds)
    }

}