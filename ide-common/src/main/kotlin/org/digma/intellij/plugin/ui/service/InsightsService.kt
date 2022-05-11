package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.digma.intellij.plugin.model.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.MethodCodeObjectSummary
import org.digma.intellij.plugin.ui.model.InsightsModel

class InsightsService(val project: Project) {

    lateinit var panel: DialogPanel
    lateinit var model: InsightsModel;


    fun updateSelectedMethod(
        methodUnderCaret: MethodUnderCaret,
        methodCodeObjectSummary: MethodCodeObjectSummary?
    ) {
        model.methodName = methodUnderCaret.name
        model.insightsCount = 0;
        model.className = methodUnderCaret.className
        methodCodeObjectSummary?.let { it ->
            it.insightsCount.also {
                model.insightsCount = it
            }
        }
        panel.reset()
    }

    fun empty() {

    }


}