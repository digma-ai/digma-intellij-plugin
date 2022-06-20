package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.util.*

class ErrorsViewService(project: Project): AbstractViewService(project) {

    //ErrorsModel is singleton object
    private var model = ErrorsModel


    private val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)



    fun contextChanged(
        methodInfo: MethodInfo
    ) {
        val errorsListContainer = errorsProvider.getErrors(methodInfo)
        model.listViewItems = errorsListContainer.listViewItems
        model.errorDetails = ErrorDetailsModel()
        model.scope = MethodScope(methodInfo)
        model.card = ErrorsTabCard.ERRORS_LIST

        updateUi()
    }


    fun contextChangeNoMethodInfo(dummy: MethodInfo) {
        model.listViewItems = ArrayList()
        model.errorDetails = ErrorDetailsModel()
        model.scope = MethodScope(dummy)
        model.card = ErrorsTabCard.ERRORS_LIST

        updateUi()
    }



    fun showErrorDetails(uid: String) {

        val errorDetails = errorsProvider.getErrorDetails(uid)
        model.errorDetails = errorDetails
        model.card = ErrorsTabCard.ERROR_DETAILS

        updateUi()

    }


    fun closeErrorDetails() {
        model.card = ErrorsTabCard.ERRORS_LIST
        updateUi()
    }

    fun empty() {
        model.listViewItems = Collections.emptyList()
        model.errorDetails = ErrorDetailsModel()
        model.scope = EmptyScope("")
        model.card = ErrorsTabCard.ERRORS_LIST

        updateUi()
    }

}