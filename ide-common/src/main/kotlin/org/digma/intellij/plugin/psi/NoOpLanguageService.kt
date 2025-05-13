package org.digma.intellij.plugin.psi

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret

class NoOpLanguageService : OldNoOpLanguageService() {

    override suspend fun detectMethodUnderCaret(
        project: Project,
        psiFile: PsiFile,
        selectedEditor: Editor,
        caretOffset: Int
    ): MethodUnderCaret {
        return MethodUnderCaret("", "", "", "", "", 0, null, false)
    }
}