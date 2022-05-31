package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

//todo: create root, see ExampleModel
class CodeObjectsModel : Ext(SolutionModel.Solution) {


    private val RiderMethodInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingClass", PredefinedType.string)
        field("containingNamespace", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
        field("spans", immutableList(RiderSpanInfo))
    }

    private val RiderSpanInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingMethod", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
    }


    private val RiderCodeLensInfo = structdef {
        field("codeObjectId", PredefinedType.string)
        field("lensText", PredefinedType.string.nullable)
        field("lensTooltip", PredefinedType.string.nullable)
        field("moreText", PredefinedType.string.nullable)
        field("anchor", PredefinedType.string.nullable)
    }



    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")

        signal("reanalyze", PredefinedType.string)
        signal("reanalyzeAll", PredefinedType.void)
        signal("documentAnalyzed", PredefinedType.string)

        //key: document file path, value: Document
        map("documents",
            PredefinedType.string,
            classdef("Document") {
                field("fileUri",PredefinedType.string)
                //map<codeObjectId,MethodInfo>
                map("methods", PredefinedType.string, RiderMethodInfo)
            }
        )

        map("codeLens",PredefinedType.string,RiderCodeLensInfo)

    }

}