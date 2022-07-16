package rider.model

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.setting
import com.jetbrains.rd.generator.nova.signal
import com.jetbrains.rider.model.nova.ide.SolutionModel

object ShowToolWindowModel : Ext(SolutionModel.Solution) {


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")

        signal("showToolWindow", PredefinedType.void)
    }


}