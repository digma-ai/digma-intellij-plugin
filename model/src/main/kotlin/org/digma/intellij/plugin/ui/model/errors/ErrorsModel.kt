package org.digma.intellij.plugin.ui.model.errors

import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.util.Collections

enum class ErrorsTabCard {
    ERRORS_LIST, ERROR_DETAILS,
}

class ErrorsModel {

    var errorsCount: Int = 0
    var listViewItems: List<ListViewItem<CodeObjectError>> = Collections.emptyList()
    var errorDetails: ErrorDetailsModel = ErrorDetailsModel()
    var card: ErrorsTabCard = ErrorsTabCard.ERRORS_LIST

}