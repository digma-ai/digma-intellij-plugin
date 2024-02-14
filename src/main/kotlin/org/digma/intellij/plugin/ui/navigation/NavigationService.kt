package org.digma.intellij.plugin.ui.navigation

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.navigation.ViewChangedEvent
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.jcef.JCefComponent
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

    init {
        project.messageBus.connect(this).subscribe(
            ViewChangedEvent.VIEW_CHANGED_TOPIC, ViewChangedEvent { views ->
                jCefComponent?.let {
                    sendCurrentViewsState(it.jbCefBrowser.cefBrowser, views)
                }
            })
    }


    override fun dispose() {
        //nothing to do , used as parent disposable
    }

    fun setJCefComponent(jCefComponent: JCefComponent) {
        this.jCefComponent = jCefComponent
    }

    fun fixMissingDependencies(methodId: String) {

        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
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
                EDT.ensureEDT {
                    NotificationUtil.notifyError(project, "Failed to add dependency after 60 seconds")
                }
            }

            if (isActive) {
                simulateCursorEvent()
            }
        }
    }


    fun addAnnotation(methodId: String) {

        val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodId)
        val instrumentationProvider = languageService.instrumentationProvider
        instrumentationProvider.addObservability(methodId)


        @Suppress("UnstableApiUsage")
        this.disposingScope().launch {

            val startTime = Instant.now()
            var methodInfo = DocumentInfoService.getInstance(project).findMethodInfo(methodId)

            while (isActive &&
                (methodInfo == null || !methodInfo.hasRelatedCodeObjectIds()) &&
                Instant.now().isBefore(startTime.plusSeconds(10))
            ) {

                delay(50)
                methodInfo = DocumentInfoService.getInstance(project).findMethodInfo(methodId)
            }

            if (isActive &&
                (methodInfo == null || !methodInfo.hasRelatedCodeObjectIds())
            ) {

                EDT.ensureEDT {
                    NotificationUtil.notifyError(project, "Failed to add annotation after 10 seconds")
                }
            }

            if (isActive) {
                simulateCursorEvent()
            }
        }

    }


    private fun simulateCursorEvent() {

        EDT.ensureEDT {

            val selectedTextEditor = FileEditorManager.getInstance(project).selectedTextEditor
            selectedTextEditor?.let { textEditor ->
                val document = textEditor.document
                val virtualFile = FileDocumentManager.getInstance().getFile(document)
                virtualFile?.takeIf { isValidVirtualFile(virtualFile) }?.let { vFile ->
                    val psiFile = PsiManager.getInstance(project).findFile(vFile)
                    psiFile?.takeIf { PsiUtils.isValidPsiFile(psiFile) }?.let { pFile ->
                        val languageService: LanguageService = LanguageServiceLocator.getInstance(project).locate(pFile.language)
                        languageService.refreshMethodUnderCaret(project, pFile, textEditor, textEditor.caretModel.offset)
                    }
                }
            }
        }
    }


}