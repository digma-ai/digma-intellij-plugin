package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.util.function.Supplier

fun environmentsPanel(envsSupplierSupplier: Supplier<EnvironmentsSupplier>): DialogPanel {

    return panel {
        row(asHtml(spanGrayed("Environments: "))) {

        }

        row {
            cell(
                CopyableLabel("")
            ).bind(
                CopyableLabel::getText, CopyableLabel::setText, PanelMutableProperty(envsSupplierSupplier)
            )
        }
    }.andTransparent()

}

private class PanelMutableProperty(val envsSupplierSupplier: Supplier<EnvironmentsSupplier>) : MutableProperty<String> {

    override fun set(value: String) {
        // do nothing
    }

    override fun get(): String {
        val envsSupplier = envsSupplierSupplier.get()

        val sb = StringBuilder()
        var firstIteration = true

        for (currEnv in envsSupplier.getEnvironments()) {
            val isSelectedEnv = currEnv.contentEquals(envsSupplier.getCurrent())
            val linkText = buildLinkText(currEnv, isSelectedEnv)
            if (!firstIteration) {
                sb.append(", ")
            }
            sb.append(linkText)
            firstIteration = false
        }

        return asHtml(sb.toString())
    }

    fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        if (isSelectedEnv) {
            return spanBold(currEnv)
        }
        return span(currEnv)
    }
}
