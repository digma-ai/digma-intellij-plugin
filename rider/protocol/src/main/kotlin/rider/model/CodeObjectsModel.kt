package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

//todo: create root, see ExampleModel
@Suppress("unused")
object CodeObjectsModel : Ext(SolutionModel.Solution) {

    //todo: fix the signals, see unity

    private val RiderMethodInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingClass", PredefinedType.string)
        field("containingNamespace", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
        field("offsetAtFileUri", PredefinedType.int)
        field("spans", immutableList(RiderSpanInfo))
    }

    private val RiderSpanInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingMethod", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
        field("offset", PredefinedType.int)
    }


    private val RiderCodeLensInfo = structdef {
        field("codeObjectId", PredefinedType.string)
        field("lensText", PredefinedType.string.nullable)
        field("lensTooltip", PredefinedType.string.nullable)
        field("moreText", PredefinedType.string.nullable)
        field("anchor", PredefinedType.string.nullable)
        field("documentProtocolKey", PredefinedType.string)
    }


    private val CodeObjectIdUriPair = structdef{
        field("codeObjectId", PredefinedType.string)
        field("workspaceUri", PredefinedType.string)
    }

    private val CodeObjectIdUriOffsetTrouple = structdef {
        field("codeObjectId", PredefinedType.string)
        field("workspaceUri", PredefinedType.string)
        field("offset", PredefinedType.int)
    }


    init {
        call(
            "getWorkspaceUris",
            immutableList(PredefinedType.string), immutableList(CodeObjectIdUriPair)
        ).async

        call(
            "getSpansWorkspaceUris",
            immutableList(PredefinedType.string), immutableList(CodeObjectIdUriOffsetTrouple)
        ).async

        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")

        signal("reanalyze", PredefinedType.string)
        signal("documentAnalyzed", PredefinedType.string)
        signal("refreshIncompleteDocuments", PredefinedType.void)

        //key: document file path, value: Document
        map("documents",
            PredefinedType.string,
            classdef("Document") {
                field("isComplete",PredefinedType.bool)
                field("fileUri",PredefinedType.string)
                //map<codeObjectId,MethodInfo>
                map("methods", PredefinedType.string, RiderMethodInfo)
            }
        )

        map("codeLens",PredefinedType.string,RiderCodeLensInfo)

    }

}