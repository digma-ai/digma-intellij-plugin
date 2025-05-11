package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.project.Project
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.codelens.CodeLensClickHandler
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.ui.ToolWindowShower

@Suppress("LightServiceMigrationCode")
class ShowToolWindowHost(project: Project, private val cs: CoroutineScope) : LifetimedProjectComponent(project) {

    private val model: ShowToolWindowModel = project.solution.showToolWindowModel

    init {
        model.showToolWindow.advise(componentLifetime) { codeLensInfo: RiderCodeLensInfo? ->
            ToolWindowShower.getInstance(project).showToolWindow()
            codeLensInfo?.let {
                val codelens = it.toCodeLens()
                CodeLensClickHandler(project, cs, codelens).handle()
            }
        }
    }

    private fun RiderCodeLensInfo.toCodeLens(): CodeLens {
        return CodeLens(
            id = id,
            codeMethod = methodCodeObjectId,
            scopeCodeObjectId = scopeCodeObjectId,
            lensTitle = lensTitle,
            importance = importance,
            lensDescription = lensDescription,
            lensMoreText = moreText
        )

    }
}