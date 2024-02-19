package org.digma.intellij.plugin.ui.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.tests.FilterForLatestTests
import org.digma.intellij.plugin.model.rest.tests.TestsScopeRequest
import org.digma.intellij.plugin.scope.SpanScope
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.createObjectMapper
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.model.Payload
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.tests.model.SET_LATEST_TESTS_MESSAGE_NAME
import org.digma.intellij.plugin.ui.tests.model.SetLatestTestsMessage


@Service(Service.Level.PROJECT)
class TestsUpdater(private val project: Project) {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    private val objectMapper: ObjectMapper = createObjectMapper()

    private var lastKnownFilterForLatestTests: FilterForLatestTests = FilterForLatestTests(emptySet())

    //should be called when the panel is initialized
    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    /*fun updateTestsData(scopeRequest: TestsScopeRequest) {
        //This method is called from outside, when the scope is changed,
        // so need to reset pageNumber to 1
        lastKnownFilterForLatestTests.pageNumber = 1
        updateTestsData(scopeRequest, lastKnownFilterForLatestTests)
    }*/

    fun updateTestsData(scope: SpanScope ?, filter: FilterForLatestTests) {
        //keep the last filter for next use when calling updateTestsData(scopeRequest: ScopeRequest)
        this.lastKnownFilterForLatestTests = filter

        val cefBrowser = jCefComponent?.jbCefBrowser?.cefBrowser
        if (cefBrowser == null) {
            Log.log(logger::warn, "updateTestsData was called but cefBrowser is null")
            return
        }
        if(scope == null) {
            serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, SetLatestTestsMessage(Payload(null)))
            return
        }

        try {
            var spanCodeObjectIds: Set<String> = setOf()
            if(scope.spanCodeObjectId.isNotEmpty())
                spanCodeObjectIds = setOf(scope.spanCodeObjectId)

            //todo: should check if scopeRequest.isEmpty() and return json representing empty state in case of Document scope, no need to call the backend
            val testsOfSpanJson = project.service<TestsService>().getLatestTestsOfSpan(TestsScopeRequest(spanCodeObjectIds, scope.methodId, null), lastKnownFilterForLatestTests)
            Log.log(logger::trace, project, "got tests of span {}", testsOfSpanJson)
            val payload = objectMapper.readTree(testsOfSpanJson)
            val message = SetLatestTestsMessage(Payload(payload))
            Log.log(logger::trace, project, "sending {} message", SET_LATEST_TESTS_MESSAGE_NAME)

            serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, message)

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "error setting tests of span data")
            var errorDescription = e.toString()
            if (e is AnalyticsServiceException) {
                errorDescription = e.getMeaningfulMessage()
            }
            val message = SetLatestTestsMessage(
                Payload(null, ErrorPayload(errorDescription))
            )
            Log.log(logger::trace, project, "sending {} message with error", SET_LATEST_TESTS_MESSAGE_NAME)
            serializeAndExecuteWindowPostMessageJavaScript(cefBrowser, message)
            ErrorReporter.getInstance().reportError(project, "TestsUpdater.updateTestsData", e)
        }
    }
}