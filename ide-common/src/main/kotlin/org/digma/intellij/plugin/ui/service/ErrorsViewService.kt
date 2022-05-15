package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel

class ErrorsViewService(val project: Project) {

    lateinit var panel: DialogPanel
    lateinit var model: ErrorsModel;


    fun updateSelectedMethod(
        methodUnderCaret: MethodUnderCaret,
        methodCodeObjectSummary: MethodCodeObjectSummary?
    ) {
        model.methodName = methodUnderCaret.name
        model.errorsCount = 0;
        methodCodeObjectSummary?.let { it ->
            it.insightsCount.also {
                model.errorsCount = it
            }
        }
        panel.reset()
    }

    fun empty() {

    }


}