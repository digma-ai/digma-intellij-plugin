package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.ui.model.insights.InsightsModel

class InsightsViewService(val project: Project) {

    lateinit var panel: DialogPanel
    lateinit var model: InsightsModel
    private val insightsProvider: InsightsProvider = project.getService(InsightsProvider::class.java)


    fun contextChanged(
        methodInfo: MethodInfo
    ) {

        val insightsListContainer: InsightsListContainer = insightsProvider.getInsights(methodInfo)

        model.listViewItems = insightsListContainer.listViewItems
        model.methodName = methodInfo.name
        model.className = methodInfo.containingClass
        model.insightsCount = insightsListContainer.count

        panel.reset()
    }

    fun empty() {
        //todo: implement
    }


}