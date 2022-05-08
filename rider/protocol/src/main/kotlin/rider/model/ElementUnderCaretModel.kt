package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

class ElementUnderCaretModel : Ext(SolutionModel.Solution) {

    val ElementUnderCaret = structdef {
        field("fqn", PredefinedType.string)
        field("className", PredefinedType.string)
        field("filePath", PredefinedType.string)
    }


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.rider.protocol")
        property("elementUnderCaret", ElementUnderCaret)
        signal("refresh", PredefinedType.void)
    }

}