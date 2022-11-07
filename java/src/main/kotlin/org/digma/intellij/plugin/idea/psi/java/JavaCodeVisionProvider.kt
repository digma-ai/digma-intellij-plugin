package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.daemon.impl.JavaCodeVisionProviderBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

abstract class JavaCodeVisionProvider: JavaCodeVisionProviderBase() {


    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top



    class ErrorHotspot: JavaCodeVisionProvider() {

        companion object {
            const val ID = "DigmaEH"
        }

        override val id: String
            get() = ID

        override val name: String
            get() = "Digma error hotspot"

        override val groupId: String
            get() = ID

        override val relativeOrderings: List<CodeVisionRelativeOrdering>
            get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst)

        override fun computeLenses(editor: Editor, psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
            editor.project?.let {
                val javaCodeLensService = editor.project!!.getService(JavaCodeLensService::class.java)
                return javaCodeLensService.getErrorHotspotCodeLens(psiFile)
            }
            return emptyList()
        }
    }




    class HighUsage: JavaCodeVisionProvider() {

        companion object {
            const val ID = "DigmaHU"
        }

        override val id: String
            get() = ID

        override val name: String
            get() = "Digma high usage"

        override val groupId: String
            get() = ID

        override val relativeOrderings: List<CodeVisionRelativeOrdering>
            get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter(ErrorHotspot.ID))

        override fun computeLenses(editor: Editor, psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
            editor.project?.let {
                val javaCodeLensService = editor.project!!.getService(JavaCodeLensService::class.java)
                return javaCodeLensService.getHighUsageCodeLens(psiFile)
            }
            return emptyList()
        }
    }



    class LowUsage: JavaCodeVisionProvider() {

        companion object {
            const val ID = "DigmaLU"
        }

        override val id: String
            get() = ID

        override val name: String
            get() = "Digma low usage"

        override val groupId: String
            get() = ID

        override val relativeOrderings: List<CodeVisionRelativeOrdering>
            get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter(HighUsage.ID))

        override fun computeLenses(editor: Editor, psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
            editor.project?.let {
                val javaCodeLensService = editor.project!!.getService(JavaCodeLensService::class.java)
                return javaCodeLensService.getLowUsageCodeLens(psiFile)
            }
            return emptyList()
        }
    }



}