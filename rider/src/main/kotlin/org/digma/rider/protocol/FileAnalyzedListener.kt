package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.psi.PsiFile
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.document.DocumentAnalyzer

class FileAnalyzedListener : ProjectManagerListener {

    private lateinit var documentAnalyzer: DocumentAnalyzer

    override fun projectOpened(project: Project) {
        //todo: check what can be used on solution
        project.solution.solutionLifecycle.fullStartupFinished.advise(project.lifetime) {
            documentAnalyzer = project.getService(DocumentAnalyzer::class.java)
            var model: CodeObjectsModel = project.solution.codeObjectsModel
            model.fileAnalyzed.advise(project.lifetime) { filePath ->
                fileAnalyzed(filePath, project)
            }
        }
    }

    private fun fileAnalyzed(filePath: String, project: Project) {
        var psiFile: PsiFile? = pathToPsiFile(filePath, project)
        if (psiFile != null) { //todo: remove if(), if didn't succeed then we have a problem
            documentAnalyzer.fileOpened(psiFile)
        }
    }
}