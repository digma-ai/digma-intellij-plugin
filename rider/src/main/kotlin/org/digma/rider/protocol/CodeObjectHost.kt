package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rd.util.reactive.IMutableViewableMap
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.model.CodeLens
import org.digma.intellij.plugin.model.DocumentInfo
import org.digma.intellij.plugin.model.MethodInfo
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectHost(project: Project) {

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    fun getDocument(psiFile: PsiFile): DocumentInfo? {
        val path: String = psiFileToPath(psiFile)
        val document: Document? = this.model.documents[path]
        return toModel(document)
    }

    private fun toModel(document: Document?): DocumentInfo? {
        return document?.toDocumentInfo()
    }



    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {
        if (codeLenses.isEmpty()) return

        codeLenses.forEach(Consumer { codeLens ->
            model.codeLens[codeLens.codeObjectId] = codeLens.toRiderCodeLensInfo()
        })
        model.reanalyze.fire(psiFileToPath(psiFile))
    }


    private fun Document.toDocumentInfo() = DocumentInfo(
        path = path,
        methods = toMethodInfoMap(methods)

    )

    private fun toMethodInfoMap(methods: IMutableViewableMap<String, RiderMethodInfo>): MutableMap<String, MethodInfo> {
        val modelMethods: HashMap<String, MethodInfo> = HashMap()
        methods.forEach { (k, v) ->
            modelMethods[k] = v.toMethodInfo()
        }
        return modelMethods
    }


    private fun RiderMethodInfo.toMethodInfo() = MethodInfo(
        id = id,
        name = name,
        containingClass = containingClass,
        containingNamespace = containingNamespace,
        containingFile = containingFile
    )


    private fun CodeLens.toRiderCodeLensInfo() = RiderCodeLensInfo(
        codeObjectId = codeObjectId,
        lensText = lensText,
        lensTooltip = lensTooltipText,
        moreText = lensMoreText,
        anchor = anchor
    )


}