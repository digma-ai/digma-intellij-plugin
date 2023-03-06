package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

//todo: create root, see ExampleModel
@Suppress("unused")
object CodeObjectsModel : Ext(SolutionModel.Solution) {


    val RiderDocumentInfo = structdef {
        field("isComplete", PredefinedType.bool)
        field("fileUri", PredefinedType.string)
        field("methods", immutableList(RiderMethodInfo))
    }


    val RiderMethodInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingClass", PredefinedType.string)
        field("containingNamespace", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
        field("offsetAtFileUri", PredefinedType.int)
        field("spans", immutableList(RiderSpanInfo))
    }

    val RiderSpanInfo = structdef {
        field("id", PredefinedType.string)
        field("name", PredefinedType.string)
        field("containingMethod", PredefinedType.string)
        field("containingFileUri", PredefinedType.string)
        field("offset", PredefinedType.int)
    }

    val RiderCodeLensInfo = structdef {
        field("codeObjectId", PredefinedType.string)
        field("lensTitle", PredefinedType.string.nullable)
        field("lensDescription", PredefinedType.string.nullable)
        field("moreText", PredefinedType.string.nullable)
        field("anchor", PredefinedType.string.nullable)
        field("psiUri", PredefinedType.string)
    }


    val CodeObjectIdUriPair = structdef{
        field("codeObjectId", PredefinedType.string)
        field("workspaceUri", PredefinedType.string)
    }

    val CodeObjectIdUriOffsetTrouple = structdef {
        field("codeObjectId", PredefinedType.string)
        field("workspaceUri", PredefinedType.string)
        field("offset", PredefinedType.int)
    }


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")
        source("reanalyze", PredefinedType.string)
        map("codeLens", PredefinedType.string,
            classdef("lensPerObjectId") {
                list("lens", RiderCodeLensInfo)
            })

    }

}