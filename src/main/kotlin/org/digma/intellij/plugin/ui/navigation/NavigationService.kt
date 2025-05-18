package org.digma.intellij.plugin.ui.navigation

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.document.findMethodInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.psi.OldLanguageService
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.navigation.model.InstrumentationResult
import java.time.Instant

@Service(Service.Level.PROJECT)
class NavigationService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(this::class.java)

    private var jCefComponent: JCefComponent? = null


    companion object {
        @JvmStatic
        fun getInstance(project: Project): NavigationService {
            return project.service<NavigationService>()
        }
    }


    override fun dispose() {
        jCefComponent = null
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    fun fixMissingDependencies(methodId: String) {

        val languageService = OldLanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        val instrumentationProvider = languageService.instrumentationProvider
        instrumentationProvider.addObservabilityDependency(methodId)

        @Suppress("UnstableApiUsage")
        this.disposingScope().launch {

            val startTime = Instant.now()
            var observabilityInfo = instrumentationProvider.buildMethodObservabilityInfo(methodId)
            while (isActive && observabilityInfo.hasMissingDependency &&
                Instant.now().isBefore(startTime.plusSeconds(60))
            ) {
                delay(50)
                observabilityInfo = instrumentationProvider.buildMethodObservabilityInfo(methodId)
            }

            if (isActive && observabilityInfo.hasMissingDependency) {
                jCefComponent?.let {
                    sendAutoFixResultMessage(it.jbCefBrowser.cefBrowser, InstrumentationResult.failure, "Failed to add dependency")
                }
                EDT.ensureEDT {
                    NotificationUtil.notifyFadingError(project, "Failed to add dependency, Please try again")
                }
            }
        }
    }


    fun addAnnotation(methodId: String) {

        val languageService = OldLanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        val instrumentationProvider = languageService.instrumentationProvider
        instrumentationProvider.addObservability(methodId)


        @Suppress("UnstableApiUsage")
        this.disposingScope().launch {

            val startTime = Instant.now()
            var methodInfo = findMethodInfo(project,methodId)

            while (isActive &&
                (methodInfo == null || !methodInfo.hasRelatedCodeObjectIds()) &&
                Instant.now().isBefore(startTime.plusSeconds(10))
            ) {

                delay(50)
                methodInfo = findMethodInfo(project,methodId)
            }

            if (isActive &&
                (methodInfo == null || !methodInfo.hasRelatedCodeObjectIds())
            ) {
                jCefComponent?.let {
                    sendAddAnnotationResultMessage(it.jbCefBrowser.cefBrowser, InstrumentationResult.failure, "Failed to add annotation")
                }
                EDT.ensureEDT {
                    NotificationUtil.notifyFadingError(project, "Failed to add annotation, Please try again")
                }
            }
        }
    }

    fun navigateToCode(codeObjectId: String?) {
        codeObjectId?.let {
            val success = if (it.startsWith("method:")) {
                CodeNavigator.getInstance(project).maybeNavigateToMethod(it)
            } else if (it.startsWith("span:")) {
                CodeNavigator.getInstance(project).maybeNavigateToSpan(it)
            } else {
                CodeNavigator.getInstance(project).maybeNavigateToSpanOrMethod(it, it)
            }

            if (!success) {
                Log.log(logger::warn, "can't navigate to code object {}", it)
            }
        }
    }


}