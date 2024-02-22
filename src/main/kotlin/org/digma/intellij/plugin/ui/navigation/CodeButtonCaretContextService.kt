package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.time.StopWatch
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CaretContextService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.navigation.model.CodeContextMessage
import org.digma.intellij.plugin.ui.navigation.model.CodeContextMessagePayload
import java.util.concurrent.TimeUnit

//this is the only CaretContextService registered
class CodeButtonCaretContextService(private val project: Project) : CaretContextService {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null

    companion object {
        fun getInstance(project: Project): CodeButtonCaretContextService {
            //this unchecked casting must succeed because this is the registered CaretContextService
            return CaretContextService.getInstance(project) as CodeButtonCaretContextService
        }
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    override fun dispose() {
        //nothing to do
    }

    override fun contextChanged(methodUnderCaret: MethodUnderCaret) {

        Log.log(logger::trace, "contextChanged called for '{}'", methodUnderCaret)

        if (!isProjectValid(project)) {
            Log.log(logger::info, "project is disposed in contextChanged for '{}'", methodUnderCaret.id)
            return
        }


        Backgroundable.ensurePooledThread {
            Log.log(logger::trace, "Executing contextChanged in background for {}", methodUnderCaret.id)
            val stopWatch = StopWatch.createStarted()
            try {
                contextChangedImpl(methodUnderCaret)
            } catch (e: Throwable) {
                Log.warnWithException(logger, project, e, "error in contextChangedImpl")
                ErrorReporter.getInstance().reportError(project, "CodeButtonCaretContextService.contextChanged", e)
            } finally {
                stopWatch.stop()
                Log.log(logger::trace, "contextChangedImpl time took {} milliseconds", stopWatch.getTime(TimeUnit.MILLISECONDS))
            }
        }
    }

    private fun contextChangedImpl(methodUnderCaret: MethodUnderCaret) {

        if (methodUnderCaret.id.isBlank()) {
            empty()
            return
        }

        jCefComponent?.let { jcefComp ->


            val documentInfoService = DocumentInfoService.getInstance(project)
            val methodInfo = documentInfoService.getMethodInfo(methodUnderCaret)
            methodInfo?.let { method ->

                val endpointInfo = getFocusedEndpoint(methodUnderCaret, method)
                //endpoint info is currently only for ktor support
                if (endpointInfo != null) {
                    val codeContextSpans = AnalyticsService.getInstance(project).getSpansForCodeLocation(listOf(endpointInfo.idWithType()))
                    val displayName = endpointInfo.buildDisplayName()

                    val codeContextMessage = CodeContextMessage(
                        CodeContextMessagePayload(
                            displayName,
                            codeContextSpans,
                            true,
                            method.idWithType()
                        )
                    )
                    sendCodeContext(jcefComp.jbCefBrowser.cefBrowser, codeContextMessage)

                } else {
                    val allIds = listOf(method.idWithType()).plus(method.getRelatedCodeObjectIdsWithType())
                    val codeContextSpans = AnalyticsService.getInstance(project).getSpansForCodeLocation(allIds)
                    val displayName = method.buildDisplayName()

                    var hasMissingDependency: Boolean? = null
                    var canInstrumentMethod: Boolean? = null
                    var isInstrumented: Boolean? = true
                    val needsObservabilityFix: Boolean = checkObservability(methodInfo, codeContextSpans)
                    if (needsObservabilityFix) {
                        isInstrumented = false
                        val observabilityInfo = getMethodObservabilityInfo(method.id)
                        hasMissingDependency = observabilityInfo.hasMissingDependency
                        canInstrumentMethod = observabilityInfo.canInstrumentMethod
                    }


                    val codeContextMessage = CodeContextMessage(
                        CodeContextMessagePayload(
                            displayName,
                            codeContextSpans,
                            isInstrumented,
                            method.idWithType(),
                            hasMissingDependency,
                            canInstrumentMethod
                        )
                    )

                    sendCodeContext(jcefComp.jbCefBrowser.cefBrowser, codeContextMessage)

                }

            } ?: empty()

        }
    }

    override fun contextEmpty() {
        empty()
    }

    override fun contextEmptyNonSupportedFile(fileUri: String) {
        empty()
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
            endpointInfo.textRange?.contains(methodUnderCaret.caretOffset) ?: false &&
                    endpointInfo.framework == EndpointFramework.Ktor
        }
    }


    private fun getMethodObservabilityInfo(methodId: String): MethodObservabilityInfo {
        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        val instrumentationProvider = languageService.instrumentationProvider
        return instrumentationProvider.buildMethodObservabilityInfo(methodId)
    }


    private fun checkObservability(methodInfo: MethodInfo, codeContextSpans: CodeContextSpans): Boolean {
        if (!IDEUtilsService.getInstance(project).isJavaProject) return false
        if (methodInfo.hasRelatedCodeObjectIds()) return false
        if (codeContextSpans.assets.isNotEmpty()) return false
        return true
    }

}