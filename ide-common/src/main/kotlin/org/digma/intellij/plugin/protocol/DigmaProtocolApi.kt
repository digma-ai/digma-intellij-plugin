package org.digma.intellij.plugin.protocol

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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

    private var mainAppInitializedTime: Instant? = null
    private val delayAfterInitialize = 1.seconds

    fun setMainAppInitialized() {
        if (mainAppInitializedTime == null) {
            Log.log(logger::trace, "main app initialized , thread={}", Thread.currentThread().name)
            mainAppInitializedTime = Clock.System.now()
        }
    }


    /**
     * return null on success.
     * error message on failure
     */
    fun performAction(project: Project, parameters: Map<String, String>): String? {
        try {

            val action = getActionFromParameters(parameters) ?: return "DigmaProtocolCommand no action in request"

            Log.log(logger::trace, "perform action {}, thread {}", action, Thread.currentThread().name)

            return when (action) {
                ACTION_SHOW_ASSET_PARAM_NAME -> {
                    showAsset(project, parameters)
                }

                ACTION_SHOW_ASSETS_TAB_PARAM_NAME -> {
                    showAssetTab(project, action)
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


    private fun showAsset(project: Project, parameters: Map<String, String>): String? {

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

            delayAfterMainAppInitialized()

            val scope = SpanScope(codeObjectId)
            val contextPayload = objectToJsonNode(CustomUrlScopeContextPayload(targetTab))
            val scopeContext = ScopeContext("IDE/CUSTOM_PROTOCOL_LINK_CLICKED", contextPayload)

            Log.log(
                logger::trace,
                "calling ScopeManager.changeScope with scope='{}',scopeContext='{}',environmentId='{}', thread='{}'",
                scope,
                scopeContext,
                environmentId,
                Thread.currentThread().name
            )

            ScopeManager.getInstance(project).changeScope(scope, scopeContext, environmentId)
        }

        return null
    }


    private fun showAssetTab(project: Project, action: String): String? {
        cs.launch {

            delayAfterMainAppInitialized()

            Log.log(
                logger::trace,
                "sending ProtocolCommandEvent with action='{}', thread='{}'",
                action,
                Thread.currentThread().name
            )

            project.messageBus.syncPublisher(ProtocolCommandEvent.PROTOCOL_COMMAND_TOPIC).protocolCommand(action)
        }
        return null
    }


    private suspend fun delayAfterMainAppInitialized() {

        waitForJcefInitialize()

        //it shouldn't happen that timeToCallJcef will be null because we wait for mainAppInitializedTime to be non-null.
        // unless we got timeout waiting for jcef to initialize
        val timeToCallJcef = mainAppInitializedTime?.plus(delayAfterInitialize) ?: return

        Log.log(logger::trace, "waiting $delayAfterInitialize after jcef initialize")

        try {
            withTimeout(10000) {
                while (Clock.System.now() < timeToCallJcef) {
                    delay(100)
                }
            }
        } catch (e: TimeoutCancellationException) {
            //ignore
        }
        Log.log(logger::trace, "done waiting $delayAfterInitialize after jcef initialize")

    }


    private suspend fun waitForJcefInitialize() {

        if (mainAppInitializedTime != null) {
            return
        }

        Log.log(logger::trace, "waiting for jcef initialize")

        try {
            withTimeout(10000) {
                while (mainAppInitializedTime == null) {
                    delay(100)
                }
            }

        } catch (e: TimeoutCancellationException) {
            //ignore
        }

        Log.log(logger::trace, "done waiting for jcef initialize")
    }

}