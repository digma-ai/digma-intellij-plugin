package org.digma.intellij.plugin.ui.service

import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard

abstract class AbstractViewService {

    fun isErrorDetailsDialog(): Boolean {
        return ErrorsModel.card == ErrorsTabCard.ERROR_DETAILS
    }


    abstract fun updateUi()

    fun maybeUpdateUi(){
        if (isErrorDetailsDialog()){
            return
        }
        updateUi()
    }
}