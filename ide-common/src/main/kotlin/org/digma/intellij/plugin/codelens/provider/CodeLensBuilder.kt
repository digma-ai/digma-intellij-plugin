package org.digma.intellij.plugin.codelens.provider

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.CodeObjectsUtil
import org.digma.intellij.plugin.common.Unicodes.Companion.LIVE_CIRCLE
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.lens.CodeLens
import org.digma.intellij.plugin.model.rest.codelens.Decorator
import org.digma.intellij.plugin.model.rest.codelens.MethodWithCodeLens
import org.digma.intellij.plugin.model.rest.insights.InsightImportance
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects

internal class CodeLensBuilder(private val project: Project) {

    internal fun buildCodeLens(documentInfo: DocumentInfo): Set<CodeLens> {

        //LinkedHashSet retains insertion order so retains the order from backend
        val codeLensList: MutableSet<CodeLens> = LinkedHashSet()
        val methodsInfos: Collection<MethodInfo> = documentInfo.methods.values
        val methodsWithCodeObjects: MutableList<MethodWithCodeObjects> = mutableListOf()

        methodsInfos.forEach { methodInfo: MethodInfo ->
            val relatedSpansCodeObjectIds = methodInfo.spans.map { it.idWithType() }.toList()
            val relatedEndpointCodeObjectIds = methodInfo.endpoints.map(EndpointInfo::id).toList()
            methodInfo.allIdsWithType().forEach { id: String ->
                methodsWithCodeObjects.add(MethodWithCodeObjects(id, relatedSpansCodeObjectIds, relatedEndpointCodeObjectIds))
            }
        }

        val methodsWithCodeLens = AnalyticsService.getInstance(project).getCodeLensByMethods(methodsWithCodeObjects).methodWithCodeLens

        methodsWithCodeLens.forEach { methodWithCodeLens: MethodWithCodeLens ->

            val codeObjectId = CodeObjectsUtil.stripMethodPrefix(methodWithCodeLens.methodCodeObjectId)
            val decorators: MutableList<Decorator> = methodWithCodeLens.decorators.toMutableList()

            val liveDecorator = decorators.firstOrNull { d: Decorator -> d.title == "Live" }

            liveDecorator?.let {
                val codeLens = buildCodeLensOfActive(codeObjectId, it)
                decorators.remove(it)
                codeLensList.add(codeLens)
            }

            decorators.forEach { decorator: Decorator ->
                val importance = decorator.importance.priority
                val priorityEmoji = if (isImportant(importance)) "❗️" else ""
                val title = priorityEmoji + decorator.title
                //title is used as id of CodeLens
                val codeLens = CodeLens(decorator.title, codeObjectId, decorator.getScopeCodeObjectId(), title, importance)
                codeLens.lensDescription = decorator.description
                codeLens.lensMoreText = "Go to $title"
                codeLensList.add(codeLens)
            }
        }
        return codeLensList
    }


    private fun buildCodeLensOfActive(methodId: String, liveDecorator: Decorator): CodeLens {
        val title = LIVE_CIRCLE
        val codeLens = CodeLens(liveDecorator.title, methodId, liveDecorator.getScopeCodeObjectId(), title, 1)
        codeLens.lensDescription = liveDecorator.description
        return codeLens
    }


    private fun isImportant(importanceLevel: Int): Boolean {
        return importanceLevel <= InsightImportance.HighlyImportant.priority && importanceLevel >= InsightImportance.ShowStopper.priority
    }
}