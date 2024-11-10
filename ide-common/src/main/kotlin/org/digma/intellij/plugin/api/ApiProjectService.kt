package org.digma.intellij.plugin.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.AppIcon
import io.netty.handler.codec.http.QueryStringDecoder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.scheduling.blockingOneShotTask
import org.digma.intellij.plugin.ui.ToolWindowShower
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class ApiProjectService(val project: Project) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private var mainAppInitializedTime: Instant? = null
    private val delayAfterInitialize = 1.seconds

    fun setMainAppInitialized() {
        if (mainAppInitializedTime == null) {
            Log.log(logger::trace, "main app initialized , thread={}", Thread.currentThread().name)
            mainAppInitializedTime = Clock.System.now()
        }
    }

    fun executeAction(action: String, urlDecoder: QueryStringDecoder) {

        val command = when (action) {
            ACTION_GO_TO_HOME_PARAM_VALUE -> GoToHomeCommand()
            ACTION_GO_TO_SPAN_PARAM_VALUE -> GoToSpanCommand()
            ACTION_OPEN_REPORT_PARAM_VALUE -> OpenReportCommand()
            else -> {
                throw RuntimeException("unknown action $action")
            }
        }

        //open digma tool window and wait for main app to initialize
        openDigma()

        //the command should run after the tool window is initialized
        command.execute(project, urlDecoder)

    }

    private fun openDigma() {

        if (!ToolWindowShower.getInstance(project).isToolWindowVisible) {
            ApplicationManager.getApplication().invokeAndWait {
                Log.log(logger::trace, "showing tool window")
                ToolWindowShower.getInstance(project).showToolWindow()
                Log.log(logger::trace, "tool window shown")
            }
        }

        val result = blockingOneShotTask("ApiProjectService.waitForMainAppInitialize", 10.seconds.inWholeMilliseconds) {
            delayAfterMainAppInitialized()
        }

        if (!result) {
            Log.log(logger::warn, "Digma tool window did not open within 10 seconds, will try to execute the action anyway.")
        }

        ideToFront()

    }


    private fun delayAfterMainAppInitialized() {

        waitForJcefInitialize()

        //it shouldn't happen that timeToCallJcef will be null because we wait for mainAppInitializedTime to be non-null.
        // unless we got timeout waiting for jcef to initialize
        val timeToCallJcef = mainAppInitializedTime?.plus(delayAfterInitialize) ?: return

        Log.log(logger::trace, "waiting $delayAfterInitialize after jcef initialize")

        while (Clock.System.now() < timeToCallJcef && !Thread.currentThread().isInterrupted) {
            Thread.sleep(100)
        }
        Log.log(logger::trace, "done waiting $delayAfterInitialize after jcef initialize")

    }


    private fun waitForJcefInitialize() {

        if (mainAppInitializedTime != null) {
            return
        }

        Log.log(logger::trace, "waiting for jcef initialize")

        while (mainAppInitializedTime == null && !Thread.currentThread().isInterrupted) {
            Thread.sleep(100)
        }

        Log.log(logger::trace, "done waiting for jcef initialize")
    }



    private fun ideToFront(){
        EDT.ensureEDT {

            AppIcon.getInstance().requestAttention(project, true)

//            WindowManager.getInstance().suggestParentWindow(project)?.requestFocus()
//
//
//            val projectFrame = WindowManager.getInstance().getFrame(project)
//            if (projectFrame != null) {
//                val frameState = projectFrame.extendedState
//                if (BitUtil.isSet(frameState, Frame.ICONIFIED)) {
//                    projectFrame.extendedState = BitUtil.set(frameState, Frame.ICONIFIED, false)
//                }
//                projectFrame.toFront()
//                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
////                    val mostRecentFocusOwner = projectFrame.mostRecentFocusOwner
////                    val mostRecentFocusOwner = ToolWindowManager.getInstance(project).getToolWindow(PluginId.TOOL_WINDOW_ID)?.component
//                    val mostRecentFocusOwner = projectFrame.focusOwner
//                    if (mostRecentFocusOwner != null) {
//                        IdeFocusManager.getGlobalInstance().requestFocus(mostRecentFocusOwner, true)
//                    }
//
//                }
//
//
//                projectFrame.requestFocus()

////                val dialog = DummyDialog(project)
//                val dialog = object: DialogWrapper(project){
//                    override fun createCenterPanel(): JComponent {
//                        return JLabel("aaaaaaaaaa")
//                    }
//                }
//                dialog.show()
//                dialog.toFront()
//                dialog.doCancelAction()
//                dialog.close(1,true)


//                ShowSettingsUtil.getInstance().showSettingsDialog(project, ProjectSettings.DISPLAY_NAME)
//            }
//        WindowManager.getInstance().getFocusedComponent(project)
//            val projectComponent = WindowManager.getInstance().getIdeFrame(project)?.component
//            projectComponent?.let {
//                IdeFocusManager.getGlobalInstance().toFront(it)
//            }
        }
    }


}