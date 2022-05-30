package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.throttleLast
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.CaretContextService
import java.time.Duration

class ElementUnderCaretDetector(private val project: Project) {

    private val LOGGER = Logger.getInstance(ElementUnderCaretDetector::class.java)

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel

    @Suppress("NAME_SHADOWING")
    fun start(caretContextService: CaretContextService) {

        Log.log(LOGGER::info, "ElementUnderCaretDetector waiting for solution startup..")
        //todo: should wait for solution startup? it will impact maybeNotifyElementUnderCaret
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            Log.log(LOGGER::info, "Starting ElementUnderCaretDetector")

            // the listener starts when the tool windows is opened, by then there may be many notifyElementUnderCaret events
            // waiting and all will be processed. throttleLast makes sure to process only the last one.
            model.notifyElementUnderCaret.throttleLast(Duration.ofMillis(300),
                SingleThreadScheduler(project.lifetime, "notifyElementUnderCaret")).advise(project.lifetime) {
                val methodUnderCaret: MethodUnderCaretEvent? = model.elementUnderCaret.valueOrNull
                Log.log(LOGGER::info, "Got MethodUnderCaretEvent signal: {}", methodUnderCaret)
                notifyElementUnderCaret(methodUnderCaret, caretContextService)
            }
        }
    }



    fun emptyModel() {
        model.elementUnderCaret.set(MethodUnderCaretEvent("","","",""))
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


    /*
    maybeNotifyElementUnderCaret is meant to compensate on startup timing issue when a elementUnderCaret event is received
     before the document info is available in the frontend. it will happen when the IDE starts with cache
     of the last caret location and will fire the elementUnderCaret event. usually the document info
     is available just a second after that.
     this method should be called when there is new document info, it will also cover changes in document
     while the caret context is visible ,like the insights list.

     */
    fun maybeNotifyElementUnderCaret(psiFile: PsiFile,
                                     caretContextService: CaretContextService) {
        val methodUnderCaret: MethodUnderCaretEvent? = model.elementUnderCaret.valueOrNull
        Log.log(LOGGER::info, "Current MethodUnderCaret: {}", methodUnderCaret)

        if (methodUnderCaret?.fileUri == null || methodUnderCaret.fileUri.isBlank())
            return

        val psiFileForMethod = PsiUtils.uriToPsiFile(methodUnderCaret.fileUri,project)
        if (psiFile == psiFileForMethod){
            Log.log(LOGGER::info, "Current MethodUnderCaret belongs to psi file: {}", psiFile.virtualFile.path)
            Log.log(LOGGER::info, "Notifying MethodUnderCaret: {}", methodUnderCaret)
            notifyElementUnderCaret(methodUnderCaret, caretContextService)
        }
    }


    private fun MethodUnderCaretEvent.toModel() = MethodUnderCaret(
        id = fqn,
        name = name,
        className = className,
        fileUri = fileUri
    )


}