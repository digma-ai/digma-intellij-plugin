package org.digma.intellij.plugin.ui.troubleshooting

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler

class TroubleshootingMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "troubleshooting"
    }

    override suspend fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {
        when (action) {

            "TROUBLESHOOTING/INITIALIZE" -> {
                doCommonInitialize(browser)
            }

            "TROUBLESHOOTING/CLOSE" -> {
                EDT.ensureEDT {
                    MainToolWindowCardsController.getInstance(project).troubleshootingFinished()
                }
            }

            else -> return false
        }

        return true
    }
}