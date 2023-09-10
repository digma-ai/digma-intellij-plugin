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
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService


@Service(Service.Level.PROJECT)
class ErrorsViewOrchestrator(val project: Project) {

    private val insightsViewService: InsightsViewService = project.service<InsightsViewService>()
    private val errorsViewService: ErrorsViewService = project.service<ErrorsViewService>()
    private val insightsAndErrorsTabsHelper: InsightsAndErrorsTabsHelper = project.service<InsightsAndErrorsTabsHelper>()

    private var scopeBeforeErrorDetails: Scope? = null
    private var statusBeforeErrorDetails: UIInsightsStatus? = null


    fun showErrorDetails(error: ErrorInsightNamedError) {
        showErrorDetails(error.uid, false)
    }

    fun showErrorDetails(errorUid: String) {
        showErrorDetails(errorUid, false)
    }

    fun showErrorDetails(codeObjectError: CodeObjectError) {
        showErrorDetails(codeObjectError.uid, false)
    }

    fun showErrorDetailsFromDashboard(uid: String) {
        showErrorDetails(uid, true)
    }

    /*
    rememberCurrentScope is necessary when clicking an error in the dashboard. if the errors tab currently
    showing errors of some method then the scope will be replaced to the error scope that is going to show,
    and the previous scope restored when error details is closed.
     */
    private fun showErrorDetails(uid: String, rememberCurrentScope: Boolean) {

        //maybe there is already an error showing, must set to off before updating the model \
        // because AbstractViewService.canUpdateUI will not let update if errorDetailsOn
        insightsAndErrorsTabsHelper.errorDetailsOffNoTitleChange()
        insightsAndErrorsTabsHelper.rememberCurrentTab()
        insightsAndErrorsTabsHelper.switchToErrorsTab()

        var replaceScope = false
        if (rememberCurrentScope || insightsViewService.model.scope is EmptyScope) {
            scopeBeforeErrorDetails = insightsViewService.model.scope
            statusBeforeErrorDetails = insightsViewService.model.status
            replaceScope = true
        } else {
            scopeBeforeErrorDetails = null
            statusBeforeErrorDetails = null
        }

        val finalReplaceScope = replaceScope
        Backgroundable.ensurePooledThread {
            val errorsProvider = project.service<ErrorsProvider>()
            errorsViewService.showErrorDetails(uid, errorsProvider, finalReplaceScope)
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
        if (scopeBeforeErrorDetails != null && statusBeforeErrorDetails != null) {
            insightsViewService.model.scope = scopeBeforeErrorDetails!!
            insightsViewService.model.status = statusBeforeErrorDetails!!
            insightsViewService.notifyModelChangedAndUpdateUi()
            scopeBeforeErrorDetails = null
            statusBeforeErrorDetails = null
        }
        errorsViewService.updateUi()
        insightsViewService.updateUi()
    }


}