package org.digma.intellij.plugin.ui.navigation

import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
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
import org.digma.intellij.plugin.common.runInReadAccess
import org.digma.intellij.plugin.instrumentation.MethodInstrumentationPresenter
import org.digma.intellij.plugin.navigation.ViewChangedEvent
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.jcef.JCefComponent

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

        val methodInstrumentationPresenter = MethodInstrumentationPresenter(project)
        runInReadAccess {
            methodInstrumentationPresenter.update(methodId)
        }
        val hasMissingDependency = methodInstrumentationPresenter.cannotBecauseMissingDependency
        if (hasMissingDependency) {
            EDT.ensureEDT {
                WriteAction.run<RuntimeException> { methodInstrumentationPresenter.addDependencyToOtelLibAndRefresh() }
            }
        }

        @Suppress("UnstableApiUsage")
        this.disposingScope().launch {

            var canInstrument = methodInstrumentationPresenter.canInstrumentMethod
            while (isActive && !canInstrument) {

                delay(50)
                runInReadAccess {
                    methodInstrumentationPresenter.update(methodInstrumentationPresenter.selectedMethodId)
                }
                canInstrument = methodInstrumentationPresenter.canInstrumentMethod
            }

            if (isActive) {
                simulateCursorEvent()
            }
        }
    }


    fun addAnnotation(methodId: String) {
        val methodInstrumentationPresenter = MethodInstrumentationPresenter(project)
        runInReadAccess {
            methodInstrumentationPresenter.update(methodId)
        }
        EDT.ensureEDT {
            val succeeded =
                WriteAction.compute<Boolean, java.lang.RuntimeException> { methodInstrumentationPresenter.instrumentMethod() }
            if (succeeded) {
                simulateCursorEvent()
            } else {
                NotificationUtil.notifyError(project, "Failed to add annotation")
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