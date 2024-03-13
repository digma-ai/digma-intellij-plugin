package org.digma.intellij.plugin.ui.mainapp

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.View
import org.digma.intellij.plugin.ui.assets.AssetsMessageRouterHandler
import org.digma.intellij.plugin.ui.insights.InsightsMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.sendCurrentViewsState
import org.digma.intellij.plugin.ui.tests.TestsMessageRouterHandler

class MainAppMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {


    //using old router handlers instead of rewriting them.
    // their original onQuery will never be called.
    //todo:
    private val handlers = listOf(
        AssetsMessageRouterHandler(project),
        InsightsMessageRouterHandler(project),
        TestsMessageRouterHandler(project)
    )


    override fun getOriginForTroubleshootingEvent(): String {
        return "main"
    }


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String): Boolean {

        when (action) {

            "MAIN/INITIALIZE" -> onInitialize(browser)

            else -> {
                val handled = mutableSetOf<Boolean>()
                handlers.forEach { handler ->
                    //can probably stop at the first true but then must rely on the correctness of the result
                    // from the handlers, which is error-prone. we use this result only for reporting
                    // an unknown action and over time there may be mistakes there. its ok not to report
                    // correctly but not ok to miss a message.
                    //so we just call all handlers and collect their results
                    handled.add(handler.doOnQuery(project, browser, requestJsonNode, rawRequest, action))
                }
                //return false only if no handler handled the message so that BaseMessageRouterHandler will report it
                if (!handled.any { it }) {
                    return false
                }
            }
        }

        return true
    }


    private fun onInitialize(browser: CefBrowser) {
        try {
            doCommonInitialize(browser)
            sendCurrentViewsState(browser, MAIN_SET_VIEWS_ACTION, View.views, false)
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, e, "error getting backend info")
        }
    }

}