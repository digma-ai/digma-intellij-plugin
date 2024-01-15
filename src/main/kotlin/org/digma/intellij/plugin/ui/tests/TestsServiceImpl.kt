package org.digma.intellij.plugin.ui.tests

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.LOCAL_ENV
import org.digma.intellij.plugin.common.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.common.isEnvironmentLocal
import org.digma.intellij.plugin.common.isEnvironmentLocalTests
import org.digma.intellij.plugin.common.isLocalEnvironmentMine
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.insights.InsightsModelReactHolder
import org.digma.intellij.plugin.jcef.common.JCefMessagesUtils
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.jcef.serializeAndExecuteWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.service.FillerOfLatestTests
import org.digma.intellij.plugin.ui.service.FilterForLatestTests
import org.digma.intellij.plugin.ui.service.ScopeRequest
import org.digma.intellij.plugin.ui.service.TestsService
import org.digma.intellij.plugin.ui.tests.model.EnvironmentEntity
import org.digma.intellij.plugin.ui.tests.model.SetEnvironmentsMessage
import org.digma.intellij.plugin.ui.tests.model.SetEnvironmentsMessagePayload

class TestsServiceImpl(val project: Project) : TestsService {

    private val logger = Logger.getInstance(this::class.java)

    private var cefBrowser: CefBrowser? = null
    private var fillerOfLatestTests: FillerOfLatestTests? = null
    private var pageSize: Int = 20

    init {

        project.messageBus.connect(this).subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
            override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {
                // dp nothing
            }

            override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                sendOperativeEnvironments()
            }
        })

    }

    override fun dispose() {
        //nothing to do
    }

    override fun initWith(cefBrowser: CefBrowser, fillerOfLatestTests: FillerOfLatestTests, pageSize: Int) {
        this.cefBrowser = cefBrowser
        this.fillerOfLatestTests = fillerOfLatestTests
        this.pageSize = pageSize
    }

    private fun scope(): Scope {
        return project.service<InsightsModelReactHolder>().model.scope
    }

    override fun refresh() {
        Backgroundable.ensurePooledThread {
            val scopeRequest = getScopeRequest()
            scopeRequest?.let {
                fillerOfLatestTests!!.fillDataOfTests(cefBrowser!!, it)
            }
        }
    }

    override fun getScopeRequest(): ScopeRequest? {
        val scope = scope()

        val spans: MutableSet<String> = mutableSetOf()
        var methodCodeObjectId: String? = null
        var endpointCodeObjectId: String? = null

        when (scope) {
            is MethodScope -> {
                val methodInfo = scope.getMethodInfo()
                methodCodeObjectId = methodInfo.idWithType()
                if (methodInfo.hasRelatedCodeObjectIds()) {
                    spans.addAll(methodInfo.spans.map { it.idWithType() })

                    endpointCodeObjectId = methodInfo.endpoints.firstOrNull()?.idWithType()
                }
            }

            is CodeLessSpanScope -> {
                spans.add(CodeObjectsUtil.addSpanTypeToId(scope.getSpan().spanId))
            }

            is EndpointScope -> {
                endpointCodeObjectId = CodeObjectsUtil.addEndpointTypeToId(scope.getEndpoint().id)
            }

            else -> {
                return null
            }
        }

        val scopeRequest = ScopeRequest(spans, methodCodeObjectId, endpointCodeObjectId)
        //println("DBG: scopeRequest=$scopeRequest")
        return scopeRequest
    }

    // return JSON as string (type SetEnvironmentsMessage)
    override fun sendOperativeEnvironments() {
        if (cefBrowser == null) return

        val envs = getOperativeEnvironmentEntities()

        val setEnvironmentsMessage = SetEnvironmentsMessage(
            JCefMessagesUtils.REQUEST_MESSAGE_TYPE,
            JCefMessagesUtils.GLOBAL_SET_ENVIRONMENTS,
            SetEnvironmentsMessagePayload(
                envs
            )
        )

        serializeAndExecuteWindowPostMessageJavaScript(cefBrowser!!, setEnvironmentsMessage)
    }

    private fun getOperativeEnvironmentEntities(): List<EnvironmentEntity> {
        val environmentsHolder = project.service<AnalyticsService>().environment
        val environments = environmentsHolder.getEnvironments()

        val hostname = CommonUtils.getLocalHostname()

        val list = environments.map { env ->
            val displayName = if (isEnvironmentLocal(env) && isLocalEnvironmentMine(env, hostname)) {
                LOCAL_ENV
            } else if (isEnvironmentLocalTests(env) && isLocalEnvironmentMine(env, hostname)) {
                LOCAL_TESTS_ENV
            } else {
                env
            }

            EnvironmentEntity(displayName, env)
        }.toList()
        return list
    }

    // return JSON as string (type LatestTestsOfSpanResponse)
    override fun getLatestTestsOfSpan(scopeRequest: ScopeRequest, filter: FilterForLatestTests): String {
        try {
            val json = project.service<AnalyticsService>().getLatestTestsOfSpan(scopeRequest, filter, pageSize)
            return json
        } catch (e: AnalyticsServiceException) {
            Log.warnWithException(logger, project, e, "exception in getLatestTestsOfSpan")
            ErrorReporter.getInstance().reportError(project, "TestsService.getLatestTestsOfSpan", e)
            throw e
        }
    }

}