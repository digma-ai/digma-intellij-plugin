package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

class ElementUnderCaretModel : Ext(SolutionModel.Solution) {

    val MethodUnderCaretEvent = structdef {
        field("fqn", PredefinedType.string)
        field("name", PredefinedType.string)
        field("className", PredefinedType.string)
        field("fileUri", PredefinedType.string)
    }


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")
        property("elementUnderCaret", MethodUnderCaretEvent)
        signal("notifyElementUnderCaret", PredefinedType.void)
    }

}