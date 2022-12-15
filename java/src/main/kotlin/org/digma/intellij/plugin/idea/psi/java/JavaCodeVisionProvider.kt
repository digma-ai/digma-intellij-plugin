package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.daemon.impl.JavaCodeVisionProviderBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

abstract class JavaCodeVisionProvider: JavaCodeVisionProviderBase() {


    internal val empty: List<Pair<TextRange, CodeVisionEntry>> = listOf()


    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top

    class JavaCodeLens: JavaCodeVisionProvider() {

        companion object {
            const val ID = "DigmaCL"
        }

        override val id: String
            get() = ID

        override val name: String
            get() = "Digma code lens"

        override val groupId: String
            get() = ID

        override val relativeOrderings: List<CodeVisionRelativeOrdering>
            get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)

        override fun computeLenses(editor: Editor, psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {

            if (psiFile.virtualFile == null){
                return empty
            }

            editor.project?.let {
                val javaCodeLensService = editor.project!!.getService(JavaCodeLensService::class.java)
                return javaCodeLensService.getCodeLens(psiFile)
            }
            return emptyList()
        }
    }

}