package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.codespans.Asset
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans
import org.digma.intellij.plugin.model.rest.codespans.ErrorData
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.navigation.model.CodeContextMessage
import org.digma.intellij.plugin.ui.navigation.model.CodeContextMessagePayload
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CodeButtonContextService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    companion object {
        fun getInstance(project: Project): CodeButtonContextService {
            return project.service<CodeButtonContextService>()
        }
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    override fun dispose() {
        jCefComponent = null
    }

    suspend fun contextChanged(languageService: LanguageService?, methodUnderCaret: MethodUnderCaret?, methodInfo: MethodInfo?) {

        EDT.assertNonDispatchThread()
        ReadActions.assertNotInReadAccess()

        Log.log(logger::trace, "contextChanged called for '{}'", methodUnderCaret)

        if (languageService == null || methodUnderCaret == null || methodInfo == null || methodUnderCaret == MethodUnderCaret.EMPTY || methodUnderCaret.id.isBlank()) {
            empty()
            return
        }


        if (!isProjectValid(project)) {
            Log.log(logger::info, "project is disposed in contextChanged for '{}'", methodUnderCaret.id)
            return
        }


        Log.log(logger::trace, "Executing contextChanged in background for {}", methodUnderCaret.id)
        val stopWatch = StopWatch.createStarted()
        try {
            contextChangedImpl(languageService, methodUnderCaret, methodInfo)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in contextChangedImpl")
            ErrorReporter.getInstance().reportError(project, "CodeButtonContextService.contextChanged", e)
        } finally {
            stopWatch.stop()
            Log.log(logger::trace, "contextChangedImpl took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS))
        }
    }

    private suspend fun contextChangedImpl(languageService: LanguageService, methodUnderCaret: MethodUnderCaret, methodInfo: MethodInfo) {

        jCefComponent?.let { jcefComp ->

            val endpointInfo = getFocusedEndpoint(methodUnderCaret, methodInfo)
            //endpoint info is currently only for ktor support
            if (endpointInfo != null) {
                val codeContextSpans = AnalyticsService.getInstance(project).getSpansForCodeLocation(listOf(endpointInfo.idWithType()))
                val displayName = endpointInfo.buildDisplayName()

                val codeContextMessage = CodeContextMessage(
                    CodeContextMessagePayload(
                        displayName,
                        codeContextSpans,
                        true,
                        methodInfo.idWithType()
                    )
                )
                sendCodeContext(jcefComp.jbCefBrowser.cefBrowser, codeContextMessage)

            } else {
                //todo: backend returns error when sending all method ids for python
                //see: https://github.com/digma-ai/digma-collector-backend/issues/3268
                //this should be the correct code
                //val allIds = methodInfo.allIdsWithType().plus(methodInfo.getRelatedCodeObjectIdsWithType())
                //val codeContextSpans = AnalyticsService.getInstance(project).getSpansForCodeLocation(allIds)
                //this is a workaround, call the api multiple times and collect all Assets and merge them
                val assets = mutableSetOf<Asset>()
                var errorData: ErrorData? = null
                methodInfo.allIdsWithType().forEach { idWithType ->
                    val ids = listOf(idWithType).plus(methodInfo.getRelatedCodeObjectIdsWithType())
                    val codeContextSpans = AnalyticsService.getInstance(project).getSpansForCodeLocation(ids)
                    assets.addAll(codeContextSpans.assets)
                    errorData = errorData ?: codeContextSpans.errorData
                }
                //now create a new CodeContextSpans with all the collected Assets
                val codeContextSpans = CodeContextSpans(assets.toList(), errorData)
                //End workaround

                val displayName = methodInfo.buildDisplayName()

                var hasMissingDependency: Boolean? = null
                var canInstrumentMethod: Boolean? = null
                var isInstrumented: Boolean? = true
                val needsObservabilityFix: Boolean = checkObservability(methodInfo, codeContextSpans)
                if (needsObservabilityFix) {
                    isInstrumented = false
                    val observabilityInfo = getMethodObservabilityInfo(languageService, methodInfo.id)
                    hasMissingDependency = observabilityInfo.hasMissingDependency
                    canInstrumentMethod = observabilityInfo.canInstrumentMethod
                }


                val codeContextMessage = CodeContextMessage(
                    CodeContextMessagePayload(
                        displayName,
                        codeContextSpans,
                        isInstrumented,
                        methodInfo.idWithType(),
                        hasMissingDependency,
                        canInstrumentMethod
                    )
                )

                sendCodeContext(jcefComp.jbCefBrowser.cefBrowser, codeContextMessage)

            }
        }
    }


    private fun empty() {
        jCefComponent?.let {
            val codeContextMessage = CodeContextMessage(
                CodeContextMessagePayload(
                    "",
                    CodeContextSpans(listOf(), null)
                )
            )
            sendCodeContext(it.jbCefBrowser.cefBrowser, codeContextMessage)
        }
    }


    private fun getFocusedEndpoint(methodUnderCaret: MethodUnderCaret, methodInfo: MethodInfo): EndpointInfo? {
        return methodInfo.endpoints.firstOrNull { endpointInfo: EndpointInfo ->
            endpointInfo.textRange.contains(methodUnderCaret.caretOffset) &&
                    endpointInfo.framework == EndpointFramework.Ktor
        }
    }


    private suspend fun getMethodObservabilityInfo(languageService: LanguageService, methodId: String): MethodObservabilityInfo {
        val instrumentationProvider = languageService.getInstrumentationProvider()
        return instrumentationProvider.buildMethodObservabilityInfo(methodId)
    }


    private fun checkObservability(methodInfo: MethodInfo, codeContextSpans: CodeContextSpans): Boolean {
        if (!IDEUtilsService.getInstance(project).isJavaProject) return false
        if (methodInfo.hasRelatedCodeObjectIds()) return false
        if (codeContextSpans.assets.isNotEmpty()) return false
        return true
    }

}