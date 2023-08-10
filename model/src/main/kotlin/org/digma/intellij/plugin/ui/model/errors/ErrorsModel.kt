package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.Scope
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.lang.Integer.max
import java.util.Collections

class ErrorsPreviewListItem(val name: String, val hasErrors: Boolean, val hasRelatedCodeObjects: Boolean)

enum class ErrorsTabCard {
    ERRORS_LIST, ERROR_DETAILS, PREVIEW_LIST
}

class ErrorsModel : PanelModel {

    var errorsCount: Int = 0
    var listViewItems: List<ListViewItem<CodeObjectError>> = Collections.emptyList()
    var previewListViewItems: List<ErrorsPreviewListItem> = Collections.emptyList()
    var usageStatusResult: UsageStatusResult = EmptyUsageStatusResult
    var scope: Scope = EmptyScope("Nothing here")
    var errorDetails: ErrorDetailsModel = ErrorDetailsModel()
    var card: ErrorsTabCard = ErrorsTabCard.ERRORS_LIST


    fun getMethodNamesWithErrors(): List<ListViewItem<String>> {
        val methodsWithErrors = mutableListOf<ListViewItem<String>>()

        var index = 0
        previewListViewItems.filter { o -> o.hasErrors }.forEach {
            methodsWithErrors.add(ListViewItem(it.name, index++))
        }
        return methodsWithErrors
    }

    fun hasErrors(): Boolean {
        return previewListViewItems.any { o -> o.hasErrors }
    }

    fun hasDiscoverableCodeObjects(): Boolean {
        return previewListViewItems.any { o -> o.hasRelatedCodeObjects }
    }


    override fun count(): String {
        return max(listViewItems.size, errorsCount).toString()
    }

    override fun getTheScope(): Scope {
        return scope
    }

    override fun isMethodScope(): Boolean {
        return scope is MethodScope
    }

    override fun isDocumentScope(): Boolean {
        return scope is DocumentScope
    }

    override fun isCodeLessSpanScope(): Boolean {
        return scope is CodeLessSpanScope
    }

    override fun getScopeString(): String {
        return scope.getScope()
    }

    override fun getScopeTooltip(): String {
        return scope.getScopeTooltip()
    }

    override fun getUsageStatus(): UsageStatusResult {
        return usageStatusResult
    }

}