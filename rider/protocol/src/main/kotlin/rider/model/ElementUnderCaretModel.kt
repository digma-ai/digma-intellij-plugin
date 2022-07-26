package rider.model

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
object ElementUnderCaretModel : Ext(SolutionModel.Solution) {

    private val MethodUnderCaretEvent = structdef {
        field("fqn", PredefinedType.string)
        field("name", PredefinedType.string)
        field("className", PredefinedType.string)
        field("fileUri", PredefinedType.string)
        field("isSupportedFile", PredefinedType.bool).default(true).documentation = "Used when a source file is not applicable for code objects, for example classes from external assemblies"
    }


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")
        //todo: consider instead of elementUnderCaret property and a notifyElementUnderCaret signal just register to elementUnderCaret change and remove the signal
        property("elementUnderCaret", MethodUnderCaretEvent)
        sink("notifyElementUnderCaret", PredefinedType.void)
        source("refresh", PredefinedType.void)
    }

}