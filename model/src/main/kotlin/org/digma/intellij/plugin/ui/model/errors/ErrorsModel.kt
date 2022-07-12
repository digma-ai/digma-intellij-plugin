package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.ui.model.*
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.lang.Integer.max
import java.util.*

object ErrorsModel : PanelModel {

    var errorsCount: Int = 0
    var listViewItems: List<ListViewItem<CodeObjectError>> = Collections.emptyList()
    var scope: Scope = EmptyScope("")
    var errorDetails: ErrorDetailsModel = ErrorDetailsModel()
    var card: ErrorsTabCard = ErrorsTabCard.ERRORS_LIST


    override fun count(): String {
        return  max(listViewItems.size, errorsCount).toString()
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

}


enum class ErrorsTabCard{
    ERRORS_LIST,ERROR_DETAILS
}