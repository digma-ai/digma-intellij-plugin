package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

//todo: create root, see ExampleModel
@Suppress("unused")
object LanguageServiceModel : Ext(SolutionModel.Solution) {




    private val MethodUnderCaretRequest = structdef {
        field("psiId", CodeObjectsModel.PsiFileID)
        field("offset", PredefinedType.int)
    }


    private val RiderMethodUnderCaret = structdef {
        field("fqn", PredefinedType.string)
        field("name", PredefinedType.string)
        field("className", PredefinedType.string)
        field("fileUri", PredefinedType.string)
        field("isSupportedFile", PredefinedType.bool).documentation = "Used when a source file is not applicable for code objects, for example classes from external assemblies"
    }


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")

        call(
            "getDocumentInfo",
            CodeObjectsModel.PsiFileID, CodeObjectsModel.RiderDocumentInfo.nullable
        ).async


        call(
            "detectMethodUnderCaret",
            MethodUnderCaretRequest, RiderMethodUnderCaret.nullable
        ).async

        call(
            "getWorkspaceUrisForErrorStackTrace",
            immutableList(PredefinedType.string), immutableList(CodeObjectsModel.CodeObjectIdUriPair)
        ).async

        call(
            "getWorkspaceUrisForMethodCodeObjectIds",
            immutableList(PredefinedType.string), immutableList(CodeObjectsModel.CodeObjectIdUriOffsetTrouple)
        ).async

        call(
            "getSpansWorkspaceUris",
            immutableList(PredefinedType.string), immutableList(CodeObjectsModel.CodeObjectIdUriOffsetTrouple)
        ).async

        call("isCsharpMethod", PredefinedType.string, PredefinedType.bool).async

        source("navigateToMethod", PredefinedType.string)
    }

}