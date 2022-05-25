package org.digma.intellij.plugin.ui.common

import com.intellij.util.Producer
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.ui.model.PanelModel
import javax.swing.Icon

class ScopeLineIconProducer(val model: PanelModel): Producer<Icon> {

    //currently supports only method
    override fun produce(): Icon {
        if (model.isMethodScope()){
            return Icons.INSIGHT_METHOD_SCOPE
        }else if (model.isDocumentScope()){
            return Icons.INSIGHT_DOCUMENT_SCOPE
        }
        return Icons.INSIGHT_EMPTY_SCOPE
    }
}