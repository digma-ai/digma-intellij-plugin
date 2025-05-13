package org.digma.intellij.plugin.psi

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret

interface LanguageService : OldLanguageService {


    //some language services need the editor, for example CSharpLanguageService needs to take
    // getProjectModelId from the selected editor which is the preferred way to find the IPsiSourceFile in resharper.
    suspend fun detectMethodUnderCaret(project: Project, psiFile: PsiFile, selectedEditor: Editor, caretOffset: Int): MethodUnderCaret

}