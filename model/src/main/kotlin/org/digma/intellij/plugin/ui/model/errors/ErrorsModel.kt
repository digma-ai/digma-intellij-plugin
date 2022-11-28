package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.lang.Integer.max
import java.util.Collections

class ErrorsModel : PanelModel {

    var errorsCount: Int = 0
    var listViewItems: List<ListViewItem<CodeObjectError>> = Collections.emptyList()
    var usageStatusResult: UsageStatusResult = EmptyUsageStatusResult
    var scope: Scope = EmptyScope("")
    var errorDetails: ErrorDetailsModel = ErrorDetailsModel()
    var card: ErrorsTabCard = ErrorsTabCard.ERRORS_LIST


    override fun count(): String {
        return max(listViewItems.size, errorsCount).toString()
    }

    override fun isMethodScope(): Boolean {
        return scope is MethodScope
    }

    override fun isDocumentScope(): Boolean {
        return scope is DocumentScope
    }

    override fun getScope(): String {
        return scope.getScope()
    }

    override fun getScopeTooltip(): String {
        return scope.getScopeTooltip()
    }

    override fun getUsageStatus(): UsageStatusResult {
        return usageStatusResult
    }

}


enum class ErrorsTabCard {
    ERRORS_LIST, ERROR_DETAILS
}