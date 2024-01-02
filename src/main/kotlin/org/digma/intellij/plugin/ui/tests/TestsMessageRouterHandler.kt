package org.digma.intellij.plugin.ui.tests

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.common.ObservabilityUtil
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.RegistrationEventHandler
import org.digma.intellij.plugin.ui.jcef.updateDigmaEngineStatus
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityUpdater

class TestsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {
    override fun getOriginForTroubleshootingEvent(): String {
        return "tests"
    }

    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {
        when (action) {

            "TESTS/INITIALIZE" -> {
                updateDigmaEngineStatus(project, browser)
                //...
            }


            JCefMessagesUtils.GLOBAL_REGISTER -> {
                RegistrationEventHandler.getInstance(project).register(requestJsonNode)
            }

            JCefMessagesUtils.GLOBAL_SET_OBSERVABILITY -> {
                val isEnabledObservability =
                    objectMapper.readTree(requestJsonNode.get("payload").toString()).get("isObservabilityEnabled").asBoolean()
                Log.log(logger::trace, "updateSetObservability(Boolean) called")
                ObservabilityUtil.updateObservabilityValue(project, isEnabledObservability)

                project.service<RecentActivityUpdater>().updateSetObservability(isEnabledObservability)
            }

        }
    }

}