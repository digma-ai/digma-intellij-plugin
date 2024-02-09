package org.digma.intellij.plugin.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.navigation.ErrorsDetailsHelper
import org.digma.intellij.plugin.navigation.MainContentViewSwitcher
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


@Service(Service.Level.PROJECT)
class ErrorsViewOrchestrator(val project: Project) {

    private val insightsViewService: InsightsViewService = InsightsViewService.getInstance(project)
    private val errorsViewService: ErrorsViewService = ErrorsViewService.getInstance(project)
    private val errorsDetailsHelper: ErrorsDetailsHelper = project.service<ErrorsDetailsHelper>()


    fun showErrorDetails(errorUid: String) {
        showErrorDetailsImpl(errorUid)
    }

    fun showErrorDetails(codeObjectError: CodeObjectError) {
        showErrorDetailsImpl(codeObjectError.uid)
    }


    private fun showErrorDetailsImpl(uid: String) {

        errorsDetailsHelper.errorDetailsOff()
        errorsDetailsHelper.markCurrentView()
        MainContentViewSwitcher.getInstance(project).showErrorDetails()

        Backgroundable.ensurePooledThread {
            val errorsProvider = project.service<ErrorsProvider>()
            errorsViewService.showErrorDetails(uid, errorsProvider)
            //this is necessary so the scope line will update with the error scope
            insightsViewService.notifyModelChangedAndUpdateUi()
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
        insightsViewService.updateUi()
    }
}