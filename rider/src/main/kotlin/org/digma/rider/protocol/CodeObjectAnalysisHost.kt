package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.model.CodeLens
import org.digma.intellij.plugin.model.MethodInfo
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

class CodeObjectAnalysisHost(private val project: Project) {

    private var model: CodeObjectsModel = project.solution.codeObjectsModel


    fun getMethodsForFile(psiFile: PsiFile): List<MethodInfo> {
        val filePath: String = psiFileToPath(psiFile)
        val document: Document? = this.model.documents[filePath]
        val riderMethodInfos: MutableCollection<RiderMethodInfo>? = document?.methods?.values
        val methodInfos: List<MethodInfo> = toModel(riderMethodInfos);
        //todo?? clear the methods from the document, don't need it there anymore, but keep the document? maybe remove the document too?
        //todo:?? document?.methods?.clear();
        return methodInfos
    }


    @NotNull
    private fun toModel(riderMethodInfos: MutableCollection<RiderMethodInfo>?): List<MethodInfo> {
        val modelMethodInfos: MutableList<MethodInfo> = ArrayList()
        riderMethodInfos?.forEach {
            modelMethodInfos.add(it.toMethodInfo())
        }
        return modelMethodInfos
    }


    fun installCodeLens(@NotNull psiFile: PsiFile, @NotNull codeLenses: MutableList<CodeLens>) {
        if (codeLenses.isEmpty()) return

        codeLenses.forEach(Consumer {codeLens ->
            model.codeLens.put(codeLens.codeObjectId,codeLens.toRiderCodeLensInfo())
        })
        model.reanalyze.fire(psiFileToPath(psiFile))
    }


    private fun RiderMethodInfo.toMethodInfo() = MethodInfo(
        id = id,
        name = name,
        displayName = displayName,
        containingClass = containingClass,
        containingNamespace = containingNamespace,
        containingFile = containingFile,
        containingFileDisplayName = containingFileDisplayName
    )


    private fun CodeLens.toRiderCodeLensInfo() = RiderCodeLensInfo(
        codeObjectId = codeObjectId,
        lensText = lensText,
        lensTooltip = lensTooltipText,
        moreText = lensMoreText,
        anchor = anchor
    )

}