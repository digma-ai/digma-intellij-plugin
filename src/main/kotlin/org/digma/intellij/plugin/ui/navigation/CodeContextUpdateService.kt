package org.digma.intellij.plugin.ui.navigation

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.psi.PsiDocumentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.digma.intellij.plugin.PluginId
import org.digma.intellij.plugin.analytics.AnalyticsServiceConnectionEvent
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.isCentralized
import org.digma.intellij.plugin.auth.account.DigmaDefaultAccountHolder
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.LanguageServiceLocator
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class CodeContextUpdateService(private val project: Project, private val cs: CoroutineScope) : Disposable {

    private val logger = thisLogger()

    companion object {
        fun getInstance(project: Project) {
            project.service<CodeContextUpdateService>()
        }

        fun isOurToolWindowVisible(project: Project): Boolean {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PluginId.TOOL_WINDOW_ID)
            return toolWindow?.isVisible == true
        }
    }

    private var job: Job? = null
    private var latestMethodUnderCaret: MethodUnderCaret? = null
    private var latestMethodInfo: MethodInfo? = null

    init {

        //start the task on startup because we may miss the other events
        //first start the task, then register to events. if there is a connectionLost event, it will stop the task.
        if (isOurToolWindowVisible(project) && BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
            startTask()
        }


        project.messageBus.connect(this).subscribe(
            ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
                override fun toolWindowShown(toolWindow: ToolWindow) {
                    if (toolWindow.id == PluginId.TOOL_WINDOW_ID) {
                        if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                            startTask()
                        }
                    }
                }

                @Suppress("UnstableApiUsage")
                override fun stateChanged(
                    toolWindowManager: ToolWindowManager,
                    toolWindow: ToolWindow,
                    changeType: ToolWindowManagerListener.ToolWindowManagerEventType
                ) {
                    if (toolWindow.id == PluginId.TOOL_WINDOW_ID) {
                        if (changeType == ToolWindowManagerListener.ToolWindowManagerEventType.HideToolWindow) {
                            stopTask()
                        }
                    }
                }
            }
        )


        project.messageBus.connect(this).subscribe(
            AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC, object : AnalyticsServiceConnectionEvent {
                override fun connectionLost() {
                    stopTask()
                }

                override fun connectionGained() {
                    if (isOurToolWindowVisible(project)) {
                        startTask()
                    }
                }
            })


    }


    private fun startTask() {
        if (job?.isActive == true) return
        job = cs.launch {
            while (isActive) {

                //check if the user is logged in, otherwise there is no point to update the context.
                if (shouldRun()) {

                    try {
                        val (editor, psiFile,caretOffset) = readAction {
                            val editor = FileEditorManager.getInstance(project).selectedTextEditor
                            val document = editor?.document
                            val file = document?.let { PsiDocumentManager.getInstance(project).getPsiFile(document) }
                            if (file?.isValid == true) {
                                Triple(editor, file,editor.caretModel.offset)
                            } else {
                                Triple(null, null,null)
                            }
                        }

                        if (editor != null && psiFile != null && caretOffset != null) {

                            val languageService = LanguageServiceLocator.getInstance(project).locate(psiFile.language)
                            val methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, editor, caretOffset)
                            val methodInfo = DocumentInfoService.getInstance(project).getMethodInfo(methodUnderCaret)
                            //it may be that methodInfo is not found because it is not ready yet, usually it will happen if a document was just opened
                            // and code discovery didn't finish. in that case methodUnderCaret will be found but methodInfo not. methodInfo will be
                            // created, and it will be found in one of the next iterations.
                            //so if methodUnderCaret equals latestMethodUnderCaret, and latestMethodInfo is null, but current methodInfo is not null, it
                            // means that methodInfo was just discovered, and we want to update the context.
                            if (methodUnderCaret != latestMethodUnderCaret ||
                                (latestMethodInfo == null && methodInfo != null)) {
                                latestMethodUnderCaret = methodUnderCaret
                                latestMethodInfo = methodInfo
                                Log.log(logger::debug, project, "CodeContextUpdateService: method under caret changed {}", methodUnderCaret)
                                CodeButtonContextService.getInstance(project).contextChanged(methodUnderCaret,methodInfo)
                            }
                        } else {
                            if (latestMethodUnderCaret != null) {
                                Log.log(logger::debug, project, "CodeContextUpdateService: no editor or no psiFile")
                                latestMethodUnderCaret = null
                                latestMethodInfo = null
                                CodeButtonContextService.getInstance(project).contextChanged(null, null)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e // 🔁 RE-THROW cancellation to properly cancel the coroutine
                    } catch (t: Throwable) {
                        // Log error but keep coroutine alive
                        Log.warnWithException(logger, project, t, "Error updating code context")
                        ErrorReporter.getInstance().reportError(project, "CodeContextUpdateService.startTask", t)
                        latestMethodUnderCaret = null
                        latestMethodInfo = null
                        CodeButtonContextService.getInstance(project).contextChanged(null, null)
                    }
                }

                delay(1000) // for example
            }
        }
    }

    private fun stopTask() {
        job?.cancel()
        latestMethodUnderCaret = null
        job = null
    }


    private fun shouldRun(): Boolean {
        return !(isCentralized() && DigmaDefaultAccountHolder.getInstance().account == null)
    }

    override fun dispose() {
        stopTask()
        Log.log(logger::debug, project, "CodeContextUpdateService disposed")
    }

}