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

        distinctForPython(documentInfo, methodsWithCodeLens).forEach { methodWithCodeLens: MethodWithCodeLens ->

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
                val codeLens = CodeLens(
                    decorator.title,
                    codeObjectId,
                    decorator.getScopeCodeObjectId(),
                    title,
                    importance,
                    decorator.description,
                    "Go to $title"
                )
                codeLensList.add(codeLens)
            }
        }
        return codeLensList
    }


    private fun buildCodeLensOfActive(methodId: String, liveDecorator: Decorator): CodeLens {
        val title = LIVE_CIRCLE
        val codeLens =
            CodeLens(
                liveDecorator.title,
                methodId,
                liveDecorator.getScopeCodeObjectId(),
                title,
                1,
                liveDecorator.description,
                "Go to $title"
            )
        return codeLens
    }


    private fun isImportant(importanceLevel: Int): Boolean {
        return importanceLevel <= InsightImportance.HighlyImportant.priority && importanceLevel >= InsightImportance.ShowStopper.priority
    }


    /*
    in python we send multiple ids for a method, we may get multiple MethodWithCodeLens for the same method, for each id or some of the ids.
    we want to keep only one MethodWithCodeLens for a method with the decorators from all the MethodWithCodeLens belonging to the same method.
     */
    private fun distinctForPython(documentInfo: DocumentInfo, methodsWithCodeLens: List<MethodWithCodeLens>): List<MethodWithCodeLens> {
        //don't need to do it if it's not python
        if (documentInfo.languageId.lowercase() != "python") {
            return methodsWithCodeLens
        }


        val methodWithCodeLensByMethodIds = methodsWithCodeLens.associateBy { it.methodCodeObjectId }
        val newMethodsWithCodeLens = mutableMapOf<String, MutableSet<Decorator>>()
        documentInfo.methods.forEach { (methodId, methodInfo) ->
            methodInfo.allIdsWithType().forEach { id: String ->
                val methodWithCodeLens = methodWithCodeLensByMethodIds[id]
                if (methodWithCodeLens != null) {
                    val newDecorators = newMethodsWithCodeLens.computeIfAbsent(methodId) { mutableSetOf() }
                    //map the decorators to have the same method id
                    val decorators = methodWithCodeLens.decorators.map { Decorator(it.title,it.description,methodId,it.spanCodeObjectId,it.importance) }.toSet()
                    //relying on that Decorator is a data class with correct equals and hashCode so this will merge and ignore duplicates.
                    newDecorators.addAll(decorators)
                }
            }
        }

        return newMethodsWithCodeLens.map { (id, decorators) -> MethodWithCodeLens(id, decorators.toList()) }.toList()
    }
}