package rider.model

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.nullable
import com.jetbrains.rd.generator.nova.setting
import com.jetbrains.rd.generator.nova.sink
import com.jetbrains.rider.model.nova.ide.SolutionModel
import rider.model.CodeObjectsModel.RiderCodeLensInfo

object ShowToolWindowModel : Ext(SolutionModel.Solution) {


    init {
        setting(CSharp50Generator.Namespace, "Digma.Rider.Protocol")
        setting(Kotlin11Generator.Namespace, "org.digma.intellij.plugin.rider.protocol")

        sink("showToolWindow", RiderCodeLensInfo.nullable)
    }


}