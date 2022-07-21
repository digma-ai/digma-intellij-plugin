package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.util.function.Supplier

fun environmentsPanel(envsSupplierSupplier: Supplier<EnvironmentsSupplier>): DialogPanel {

    return panel {
        row {
            cell(
                JPanelHolder()
            ).bind(
                JPanelHolder::getEnvs, JPanelHolder::setEnvs, SingleEnvPanelMutableProperty(envsSupplierSupplier)
            )
        }
    }.andTransparent()

}

private class SingleEnvPanelMutableProperty(val envsSupplierSupplier: Supplier<EnvironmentsSupplier>) :
    MutableProperty<List<SingleEnvPanel>> {

    override fun set(value: List<SingleEnvPanel>) {
        // nothing
    }

    override fun get(): List<SingleEnvPanel> {
        val envsSupplier = envsSupplierSupplier.get()

        val relevantEnvs = buildRelevantSortedEnvironments(envsSupplier)

        val list = ArrayList<SingleEnvPanel>()

        for (currEnv in relevantEnvs) {
            val isSelectedEnv = currEnv.contentEquals(envsSupplier.getCurrent())
            val linkText = buildLinkText(currEnv, isSelectedEnv)
            val link = ActionLink(asHtml(linkText)) {
                envsSupplier.setCurrent(currEnv)
            }
            link.toolTipText = currEnv

            val singlePanel = SingleEnvPanel()
            singlePanel.add(link)

            list.add(singlePanel)
        }

        return list
    }

    fun buildRelevantSortedEnvironments(envsSupplier: EnvironmentsSupplier): List<String> {
        val builtEnvs = ArrayList<String>()

        var mineLocalEnv = ""

        for (currEnv in envsSupplier.getEnvironments()) {
            if (isEnvironmentLocal(currEnv)) {
                if (isLocalEnvironmentMine(currEnv)) {
                    mineLocalEnv = currEnv
                }
                continue
            } else {
                builtEnvs.add(currEnv)
            }
        }

        builtEnvs.sortWith(String.CASE_INSENSITIVE_ORDER)
        if (mineLocalEnv.isNotBlank()) {
            builtEnvs.add(0, mineLocalEnv)
        }
        return builtEnvs
    }

    fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv)) {
            txtValue = "LOCAL"
        }
        if (isSelectedEnv) {
            return spanBold(txtValue)
        }
        return span(txtValue)
    }
}

fun isEnvironmentLocal(environment: String): Boolean {
    return environment.endsWith("[local]", true)
}

fun isLocalEnvironmentMine(environment: String): Boolean {
    val localHostname = CommonUtils.getLocalHostname()
    return environment.startsWith(localHostname, true)
}

class SingleEnvPanel : JBPanel<SingleEnvPanel>() {
    init {
        andTransparent()
    }

}

class JPanelHolder : JBPanel<JPanelHolder>() {

    var envList: List<SingleEnvPanel> = emptyList()

    init {
        andTransparent()
    }

    fun setEnvs(prmEnvList: List<SingleEnvPanel>) {
        envList = prmEnvList
        removeAll()
        for (curr in envList) {
            add(curr)
        }
    }

    fun getEnvs(): List<SingleEnvPanel> {
        return envList
    }

}
