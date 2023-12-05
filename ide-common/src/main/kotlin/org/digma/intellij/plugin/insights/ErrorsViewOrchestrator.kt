package org.digma.intellij.plugin.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


@Service(Service.Level.PROJECT)
class ErrorsViewOrchestrator(val project: Project) {

    private val insightsViewService: InsightsViewService = project.service<InsightsViewService>()
    private val errorsViewService: ErrorsViewService = project.service<ErrorsViewService>()
    private val insightsAndErrorsTabsHelper: InsightsAndErrorsTabsHelper = project.service<InsightsAndErrorsTabsHelper>()


    fun showErrorDetails(error: ErrorInsightNamedError) {
        showErrorDetailsImpl(error.uid)
    }

    fun showErrorDetails(errorUid: String) {
        showErrorDetailsImpl(errorUid)
    }

    fun showErrorDetails(codeObjectError: CodeObjectError) {
        showErrorDetailsImpl(codeObjectError.uid)
    }


    private fun showErrorDetailsImpl(uid: String) {

        //maybe there is already an error showing, must set to off before updating the model \
        // because AbstractViewService.canUpdateUI will not let update if errorDetailsOn
        insightsAndErrorsTabsHelper.errorDetailsOffNoTitleChange()
        insightsAndErrorsTabsHelper.rememberCurrentTab()
        insightsAndErrorsTabsHelper.switchToErrorsTab()

        Backgroundable.ensurePooledThread {
            val errorsProvider = project.service<ErrorsProvider>()
            errorsViewService.showErrorDetails(uid, errorsProvider)
            //this is necessary so the scope line will update with the error scope
            insightsViewService.notifyModelChangedAndUpdateUi()
            EDT.ensureEDT { insightsAndErrorsTabsHelper.errorDetailsOn() }
        }
    }

    fun closeErrorDetailsBackButton() {
        if (insightsAndErrorsTabsHelper.isErrorDetailsOn()) {
            closeErrorDetails(true)
        }
    }

    fun closeErrorDetailsInsightsTabClicked() {
        if (insightsAndErrorsTabsHelper.isErrorDetailsOn()) {
            closeErrorDetails(false)
        }
    }

    private fun closeErrorDetails(switchToPreviousTab: Boolean) {
        insightsAndErrorsTabsHelper.errorDetailsOff()
        errorsViewService.closeErrorDetails()
        insightsAndErrorsTabsHelper.errorDetailsClosed(switchToPreviousTab)
        errorsViewService.updateUi()
        insightsViewService.updateUi()
    }


}