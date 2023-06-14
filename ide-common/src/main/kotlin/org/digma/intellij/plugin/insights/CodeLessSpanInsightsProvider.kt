package org.digma.intellij.plugin.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo

class CodeLessSpanInsightsProvider(private val codeLessSpan: CodeLessSpan, project: Project): AbstractInsightsProvider(project) {


    override fun getObject(): Any {
        return codeLessSpan
    }


    override fun getObjectIdWithType(): String {
        return codeLessSpan.spanId
    }

    override fun getNonNulMethodInfo(): MethodInfo {
        //this is actually a fake MethodInfo needed by AbstractInsightsProvider to call insightsViewBuilder.build.
        // if methodId is real then maybe it will help insightsViewBuilder.build find code locations
        // and if not it will probably do nothing with it as nothing will be found.
        return MethodInfo("","","","","",0,listOf())
    }



}