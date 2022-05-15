package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.ui.model.insights.InsightsModel

class InsightsViewService(val project: Project) {

    lateinit var panel: DialogPanel
    lateinit var model: InsightsModel;
    private val insightsProvider: InsightsProvider = project.getService(InsightsProvider::class.java)
    private val documentInfoService: DocumentInfoService = project.getService(DocumentInfoService::class.java)


    fun contextChanged(
        elementUnderCaret: MethodUnderCaret
    ) {

        var methodInfo: MethodInfo? = documentInfoService.getMethodInfo(elementUnderCaret);
        val insightsListContainer: InsightsListContainer = insightsProvider.getInsights(elementUnderCaret)

        model.listViewItems = insightsListContainer.listViewItems


        if (methodInfo != null) {
            model.methodName = methodInfo.name
            model.className = methodInfo.containingClass
        }else{
            model.methodName = elementUnderCaret.name
            model.className = elementUnderCaret.className
        }

        model.insightsCount = insightsListContainer.count;

        panel.reset()
    }

    fun empty() {
        //todo: implement
    }


}