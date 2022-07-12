package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.throttleLast
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.ui.CaretContextService
import java.time.Duration

class ElementUnderCaretDetector(private val project: Project) {

    private val LOGGER = Logger.getInstance(ElementUnderCaretDetector::class.java)

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel

    @Suppress("NAME_SHADOWING")
    fun start(caretContextService: CaretContextService) {

        Log.log(LOGGER::info, "ElementUnderCaretDetector waiting for solution startup..")
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            Log.log(LOGGER::info, "Starting ElementUnderCaretDetector")


            //maybe there is already a MethodUnderCaretEvent in the protocol. the backend may fire events
            //even before the frontend solution is fully started.
            var methodUnderCaret = model.elementUnderCaret.valueOrNull
            if (methodUnderCaret != null) {
                Log.log(LOGGER::debug, "MethodUnderCaretEvent exists,notifying {}", methodUnderCaret)
                notifyElementUnderCaret(methodUnderCaret, caretContextService)
            }


            // the listener starts when the tool windows is opened, by then there may be many notifyElementUnderCaret events
            // waiting and all will be processed. throttleLast makes sure to process only the last one.
            model.notifyElementUnderCaret.throttleLast(Duration.ofMillis(100),
                SingleThreadScheduler(project.lifetime, "notifyElementUnderCaret")).advise(project.lifetime){
                methodUnderCaret = model.elementUnderCaret.valueOrNull
                Log.log(LOGGER::debug, "Got MethodUnderCaretEvent signal: {}", methodUnderCaret)
                notifyElementUnderCaret(methodUnderCaret, caretContextService)
            }
        }
    }


    fun emptyModel() {
        model.protocol.scheduler.invokeOrQueue {
            model.elementUnderCaret.set(MethodUnderCaretEvent("", "", "", ""))
        }
    }


    //calling refresh for element under caret in case the current element under caret has non-complete method names.
    //that may happen for the same reason we have incomplete documents. usually happens on startup and when resharper
    //caches are not ready. refresh will fix it. and will maybe fix other misses on element under caret.
    //it is called when new code objects are received, when that happens we want to update the ui with the
    //current context. refresh is an easy way to cause a methodUnderCaret event.
    fun refresh() {
        model.protocol.scheduler.invokeOrQueue {
            model.refresh.fire(Unit)
        }
    }


    private fun notifyElementUnderCaret(
        elementUnderCaret: MethodUnderCaretEvent?,
        caretContextService: CaretContextService
    ) {
        if (elementUnderCaret == null) {
            caretContextService.contextEmpty()
        } else {
            caretContextService.contextChanged(elementUnderCaret.toModel())
        }
    }



    private fun MethodUnderCaretEvent.toModel() = MethodUnderCaret(
        id = fqn,
        name = name,
        className = className,
        fileUri = fileUri
    )


}