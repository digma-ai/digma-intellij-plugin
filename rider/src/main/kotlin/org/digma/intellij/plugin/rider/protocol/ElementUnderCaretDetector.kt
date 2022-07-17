package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.throttleLast
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.ui.CaretContextService
import java.time.Duration

class ElementUnderCaretDetector(project: Project) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(ElementUnderCaretDetector::class.java)

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel

    private val caretContextService: CaretContextService = project.getService(CaretContextService::class.java)

    init {

        Log.log(logger::info,project, "ElementUnderCaretDetector registering for solution startup..")

        //todo: remove
        project.solution.isLoaded.advise(project.lifetime) {
            Log.log(logger::info,"in isLoaded")
        }

        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            Log.log(logger::info, "Starting ElementUnderCaretDetector")

            //maybe there is already a MethodUnderCaretEvent in the protocol. the backend may fire events
            //even before the frontend solution is fully started.
            var methodUnderCaret = model.elementUnderCaret.valueOrNull
            if (methodUnderCaret != null) {
                Log.log(logger::debug, "MethodUnderCaretEvent exists on startup,notifying {}", methodUnderCaret)
                notifyElementUnderCaret(methodUnderCaret)
            }


            // the listener starts when the tool windows is opened, by then there may be many notifyElementUnderCaret events
            // waiting and all will be processed. throttleLast makes sure to process only the last one.
            model.notifyElementUnderCaret.throttleLast(Duration.ofMillis(100),
                SingleThreadScheduler(project.lifetime, "notifyElementUnderCaret")).advise(project.lifetime){
                methodUnderCaret = model.elementUnderCaret.valueOrNull
                Log.log(logger::debug, "Got MethodUnderCaretEvent signal: {}", methodUnderCaret)
                notifyElementUnderCaret(methodUnderCaret)
            }
        }
    }


    fun emptyModel() {
        model.protocol.scheduler.invokeOrQueue {
            WriteAction.run<Exception> {
                model.elementUnderCaret.set(MethodUnderCaretEvent("", "", "", ""))
            }
        }
    }


    //calling refresh for element under caret in case the current element under caret has non-complete method names.
    //that may happen for the same reason we have incomplete documents. usually happens on startup and when resharper
    //caches are not ready. refresh will fix it. and will maybe fix other misses on element under caret.
    //it is called when new code objects are received, when that happens we want to update the ui with the
    //current context. refresh is an easy way to intentionally cause a methodUnderCaret event.
    fun refresh() {
        model.protocol.scheduler.invokeOrQueue {
            WriteAction.run<Exception> {
                model.refresh.fire(Unit)
            }
        }
    }


    private fun notifyElementUnderCaret(elementUnderCaret: MethodUnderCaretEvent?) {
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
        fileUri = fileUri,
        isSupportedFile = isSupportedFile
    )


}