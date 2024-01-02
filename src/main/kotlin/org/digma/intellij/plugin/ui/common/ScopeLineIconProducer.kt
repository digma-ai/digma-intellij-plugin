package org.digma.intellij.plugin.ui.common

import com.intellij.util.Producer
import org.digma.intellij.plugin.ui.model.PanelModel
import javax.swing.Icon

class ScopeLineIconProducer(val model: PanelModel): Producer<Icon> {

    //currently supports only method
    override fun produce(): Icon {
        if (model.isMethodScope()){
            return Laf.Icons.Insight.METHOD
        }else if (model.isDocumentScope()){
            return Laf.Icons.Insight.FILE
        }else if (model.isCodeLessSpanScope()){
            return Laf.Icons.Insight.TELESCOPE
        } else if (model.isEndpointScope()) {
            return Laf.Icons.Insight.INTERFACE
        }
        return Laf.Icons.Insight.EMPTY
    }
}