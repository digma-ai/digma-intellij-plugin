package org.digma.intellij.plugin.insights

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.model.discovery.CodeLessSpan

class CodeLessSpanInsightsProvider(private val codeLessSpan: CodeLessSpan, project: Project): AbstractInsightsProvider(project) {

    override fun getObject(): Any {
        return codeLessSpan
    }


    override fun getObjectIdWithType(): String {
        return codeLessSpan.spanId
    }

}