package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.model.MethodUnderCaret
import org.digma.intellij.plugin.ui.MethodContextUpdater

class ElementUnderCaretDetector(private val project: Project) {

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel


    @Suppress("NAME_SHADOWING")
    fun start(methodContextUpdated: MethodContextUpdater) {

        //check is elementUnderCaret already has value and update tool window.
        //start is invoked only when the tool window opens and maybe there is already something in the protocol.
        val elementUnderCaret = model.elementUnderCaret.valueOrNull
        if (elementUnderCaret != null){
            notifyMethodUnderCaret(elementUnderCaret,methodContextUpdated)
        }

        model.refresh.advise(project.lifetime) {
            val elementUnderCaret: ElementUnderCaret? = model.elementUnderCaret.valueOrNull

            notifyMethodUnderCaret(elementUnderCaret,methodContextUpdated)

        }
    }

    private fun notifyMethodUnderCaret(
        elementUnderCaret: ElementUnderCaret?,
        methodContextUpdated: MethodContextUpdater
    ) {
        if (elementUnderCaret == null || elementUnderCaret.fqn.isEmpty()) methodContextUpdated.clearViewContent() else
            methodContextUpdated.updateViewContent(elementUnderCaret.toModel())

    }


    private fun ElementUnderCaret.toModel() = MethodUnderCaret(
        id = fqn,
        className = className,
        filePath = filePath)
}