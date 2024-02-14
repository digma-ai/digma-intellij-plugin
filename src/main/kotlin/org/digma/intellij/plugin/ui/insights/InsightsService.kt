package org.digma.intellij.plugin.ui.insights

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsServiceImpl
import org.digma.intellij.plugin.insights.model.outgoing.SetInsightDataListMessage
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.jcef.common.UserRegistrationEvent
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.settings.SettingsState
import org.digma.intellij.plugin.ui.common.getQueryMapFromPayload
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.createObjectMapper
import org.digma.intellij.plugin.ui.jcef.sendUserEmail
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.tests.TestsService


@Service(Service.Level.PROJECT)
class InsightsService(val project: Project) : InsightsServiceImpl(project) {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsService {
            return project.service<InsightsService>()
        }
    }

    init {
        //TestsService depends on InsightsModelReact.scope so make sure its initialized and listening.
        // It may also be called from TestsPanel, whom even comes first
        project.getService(TestsService::class.java)

        SettingsState.getInstance().addChangeListener({
            jCefComponent?.let {
                JCefBrowserUtil.sendRequestToChangeTraceButtonEnabled(it.jbCefBrowser)
            }
        }, this)


        project.messageBus.connect(this).subscribe(UserRegistrationEvent.USER_REGISTRATION_TOPIC, UserRegistrationEvent { email ->
            jCefComponent?.let {
                sendUserEmail(it.jbCefBrowser.cefBrowser, email)
            }

        })

    }

    fun setJCefComponent(jCefComponent: JCefComponent?) {
        this.jCefComponent = jCefComponent
    }

    fun refreshInsightsList(jsonNode: JsonNode) {
        EDT.assertNonDispatchThread()
        val backendQueryParams: Map<String, Any> = getQueryMapFromPayload(jsonNode)
        try {
            val insightsResponse = AnalyticsService.getInstance(project).getInsights(backendQueryParams)
            val msg: SetInsightDataListMessage = SetInsightDataListMessage(insightsResponse)
            jCefComponent?.let {
                serializeAndExecuteWindowPostMessageJavaScript(it.jbCefBrowser.cefBrowser, msg)
            }

        } catch (e: AnalyticsServiceException) {
            Log.log(logger::debug, "AnalyticsServiceException for refreshInsights for {}", e.message)
            ErrorReporter.getInstance().reportError(project, "InsightsServiceImpl.refreshInsights", e)
        } catch (e: JsonMappingException) {
            Log.log(logger::error, "Failed to map params", e.message)
        } catch (e: JsonProcessingException) {
            Log.log(logger::error, "Failed to read json response", e.message)
        }
    }
}
