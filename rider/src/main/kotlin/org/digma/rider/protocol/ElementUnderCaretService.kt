package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.psi.MethodIdentifier
import org.digma.intellij.plugin.ui.MethodContextUpdater

class ElementUnderCaretService(private val project: Project) {

    private val model: ElementUnderCaretModel = project.solution.elementUnderCaretModel


    fun start(methodContextUpdated: MethodContextUpdater) {

        //todo: check is elementUnderCaret has value and update tool window,
        // same as org.digma.intellij.plugin.editor.EditorListener.installOnCurrentlyOpenedEditors

        model.refresh.advise(project.lifetime) {
            var elementUnderCaret = model.elementUnderCaret.valueOrNull

            if (elementUnderCaret == null || elementUnderCaret.fqn.isEmpty()) methodContextUpdated.clearViewContent() else
                        methodContextUpdated.updateViewContent(MethodIdentifier(elementUnderCaret.fqn, elementUnderCaret.filePath))
        }
    }


}