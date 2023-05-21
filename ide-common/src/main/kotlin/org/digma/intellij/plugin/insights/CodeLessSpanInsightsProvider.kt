package org.digma.intellij.plugin.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo

class CodeLessSpanInsightsProvider(private val codeLessSpan: CodeLessSpan, project: Project): AbstractInsightsProvider(project) {


    override fun getObject(): Any {
        return codeLessSpan
    }


    override fun getObjectIdsWithType(): List<String> {

        val ids = mutableListOf<String>(CodeObjectsUtil.addSpanTypeToId(codeLessSpan.spanId))
        if (codeLessSpan.methodId != null){
            ids.add(CodeObjectsUtil.addMethodTypeToId(codeLessSpan.methodId!!))
        }
        return ids
    }

    override fun getNonNulMethodInfo(): MethodInfo {
        //this is actually a fake MethodInfo needed by AbstractInsightsProvider to call insightsViewBuilder.build.
        // this info arrived from jaeger ui,  if methodId is real then maybe it will help insightsViewBuilder.build find code locations
        // and if not it will probably do nothing with it as nothing will be found.
        return MethodInfo(codeLessSpan.methodId.toString(),codeLessSpan.functionName.toString(),"",codeLessSpan.functionNamespace.toString(),"",0,listOf())
    }



}