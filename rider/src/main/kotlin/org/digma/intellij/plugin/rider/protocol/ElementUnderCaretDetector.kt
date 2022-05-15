package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.throttleLast
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.ui.CaretContextService
import java.time.Duration

class ElementUnderCaretDetector(private val project: Project) {

    private val LOGGER = Logger.getInstance(ElementUnderCaretDetector::class.java)

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel

    //todo: SOLVED: with throttleLast.
    // listener starts when tool windows id opened, by then there may be many notifyElementUnderCaret events
    // waiting and all will be processed. maybe the backend ElementUnderCaretHost should start sending events
    // when the tool window is opened, send a special 'start' signal.
    // or start this service on startup and control the events with a start flag?
    @Suppress("NAME_SHADOWING")
    fun start(caretContextService: CaretContextService) {

        //check is elementUnderCaret already has value and update tool window.
        //start is invoked only when the tool window opens and maybe there is already something in the protocol.
//        val elementUnderCaret = model.elementUnderCaret.valueOrNull
//        if (elementUnderCaret != null) {
//            notifyElementUnderCaret(elementUnderCaret, caretContextService)
//        }

        Log.log(LOGGER::info, "Starting ElementUnderCaretDetector")
        model.notifyElementUnderCaret.throttleLast(Duration.ofMillis(500),SingleThreadScheduler(project.lifetime,"notifyElementUnderCaret")).
                advise(project.lifetime) {
                    val elementUnderCaret: ElementUnderCaret? = model.elementUnderCaret.valueOrNull
                    Log.log(LOGGER::info, "Got ElementUnderCaret signal: {}",elementUnderCaret)
                    notifyElementUnderCaret(elementUnderCaret, caretContextService)
                }
//        val mergeQueue: MergingUpdateQueue = MergingUpdateQueue("notifyElementUnderCaret",
//            500,
//            true,
//            null,project)
//        mergeQueue.activate()
//        model.notifyElementUnderCaret.grouppedFire(mergeQueue)
//        model.notifyElementUnderCaret.advise(project.lifetime) {
//            val elementUnderCaret: ElementUnderCaret? = model.elementUnderCaret.valueOrNull
//            notifyElementUnderCaret(elementUnderCaret, caretContextService)
//        }
    }

    private fun notifyElementUnderCaret(
        elementUnderCaret: ElementUnderCaret?,
        methodContextUpdated: CaretContextService
    ) {
        if (elementUnderCaret == null || elementUnderCaret.fqn.isEmpty()) {
            methodContextUpdated.contextEmpty()
        } else {
            methodContextUpdated.contextChanged(elementUnderCaret.toModel())
        }

    }


    private fun ElementUnderCaret.toModel() = MethodUnderCaret(
        id = fqn,
        name = name,
        className = className,
        fileUri = fileUri
    )
}