package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.util.function.Supplier
import javax.swing.JLabel

fun environmentsPanel(envsSupplierSupplier: Supplier<EnvironmentsSupplier>): DialogPanel {

    return panel {
        row(asHtml(spanGrayed("Environments: "))) {

        }

        row {
            cell(
                JPanelHolder()
            ).bind(
                JPanelHolder::getLabelz, JPanelHolder::setLabelz, LabelsMutableProperty(envsSupplierSupplier)
            )
        }
    }.andTransparent()

}

private class LabelsMutableProperty(val envsSupplierSupplier: Supplier<EnvironmentsSupplier>) :
    MutableProperty<List<JLabel>> {

    override fun set(value: List<JLabel>) {
        // nothing
    }

    override fun get(): List<JLabel> {
        val envsSupplier = envsSupplierSupplier.get()

        val list = ArrayList<JLabel>()
        for (currEnv in envsSupplier.getEnvironments()) {
            val isSelectedEnv = currEnv.contentEquals(envsSupplier.getCurrent())
            val linkText = buildLinkText(currEnv, isSelectedEnv)
            val jLabel = JLabel(asHtml(linkText))
            list.add(jLabel)
        }

        return list
    }

    fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        if (isSelectedEnv) {
            return spanBold(currEnv)
        }
        return span(currEnv)
    }
}

class JPanelHolder : JBPanel<JPanelHolder>() {

    var labels: List<JLabel> = emptyList()

    init {
        andTransparent()
    }

    fun setLabelz(labelList: List<JLabel>) {
        labels = labelList
        removeAll()
        for (curr in labels) {
            add(curr)
        }
    }

    fun getLabelz(): List<JLabel> {
        return labels
    }

}
