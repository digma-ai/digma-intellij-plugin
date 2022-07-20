package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.util.function.Supplier

fun environmentsPanel(envsSupplierSupplier: Supplier<EnvironmentsSupplier>): DialogPanel {

    return panel {
        row(asHtml(spanGrayed("Environments: "))) {

        }.layout(RowLayout.PARENT_GRID)

        row {
            cell(
                CopyableLabel("")
            ).bind(
                CopyableLabel::getText, CopyableLabel::setText, PanelMutableProperty(envsSupplierSupplier)
            ).horizontalAlign(HorizontalAlign.FILL)
        }.layout(RowLayout.PARENT_GRID)
    }.andTransparent()

}

private class PanelMutableProperty(val envsSupplierSupplier: Supplier<EnvironmentsSupplier>) : MutableProperty<String> {

    override fun set(value: String) {
        // do nothing
    }

    override fun get(): String {
        val ensSupplier = envsSupplierSupplier.get()
        return "curr:" + ensSupplier.getCurrent()
    }

}
