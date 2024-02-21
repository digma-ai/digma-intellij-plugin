package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.navigation.ErrorsDetailsHelper
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher


@Service(Service.Level.PROJECT)
class ErrorsViewOrchestrator(val project: Project) {

    private val errorsViewService: ErrorsViewService = ErrorsViewService.getInstance(project)
    private val errorsDetailsHelper: ErrorsDetailsHelper = project.service<ErrorsDetailsHelper>()


    fun showErrorDetails(codeObjectError: CodeObjectError) {
        showErrorDetailsImpl(codeObjectError.uid)
    }


    private fun showErrorDetailsImpl(uid: String) {

        errorsDetailsHelper.errorDetailsOff()
        errorsDetailsHelper.markCurrentView()
        MainContentViewSwitcher.getInstance(project).showErrorDetails()

        Backgroundable.ensurePooledThread {
            errorsViewService.showErrorDetails(uid)
            errorsViewService.updateUi()
            errorsDetailsHelper.errorDetailsOn()
        }
    }

    fun closeErrorDetailsBackButton() {
        if (errorsDetailsHelper.isErrorDetailsOn()) {
            closeErrorDetails(true)
        }
    }

    fun closeErrorDetails() {
        if (errorsDetailsHelper.isErrorDetailsOn()) {
            closeErrorDetails(false)
        }
    }


    private fun closeErrorDetails(switchToPreviousTab: Boolean) {
        errorsDetailsHelper.errorDetailsOff()
        errorsViewService.closeErrorDetails()
        errorsDetailsHelper.errorDetailsClosed(switchToPreviousTab)
        errorsViewService.updateUi()
    }
}