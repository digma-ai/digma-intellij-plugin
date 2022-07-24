package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.panel
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.CommonUtils.prettyTimeOf
import org.digma.intellij.plugin.icons.Icons
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import java.util.function.Supplier
import javax.swing.Icon

fun environmentsPanel(
    envsSupplierSupplier: Supplier<EnvironmentsSupplier>,
    usageStatusResultSupplier: Supplier<UsageStatusResult>,
): DialogPanel {

    return panel {
        row {
            cell(
                JPanelHolder()
            ).bind(
                JPanelHolder::getEnvs,
                JPanelHolder::setEnvs,
                SingleEnvPanelMutableProperty(envsSupplierSupplier, usageStatusResultSupplier)
            )
        }
    }.andTransparent()

}

private class SingleEnvPanelMutableProperty(
    val envsSupplierSupplier: Supplier<EnvironmentsSupplier>,
    val usageStatusResultSupplier: Supplier<UsageStatusResult>,
) : MutableProperty<List<SingleEnvPanel>> {

    override fun set(value: List<SingleEnvPanel>) {
        // nothing
    }

    override fun get(): List<SingleEnvPanel> {
        val envsSupplier = envsSupplierSupplier.get()
        val usageStatusResult = usageStatusResultSupplier.get()

        val envsThatHaveUsageSet: Set<String> = buildEnvironmentWithUsages(usageStatusResult)
        val hasUsageFunction = fun(env: String): Boolean { return envsThatHaveUsageSet.contains(env) }

        val relevantEnvs = buildRelevantSortedEnvironments(envsSupplier, hasUsageFunction)

        val list = ArrayList<SingleEnvPanel>()

        for (currEnv in relevantEnvs) {
            val isSelectedEnv = currEnv.contentEquals(envsSupplier.getCurrent())
            val toolTip = createToolTip(usageStatusResult, currEnv)
            val linkText = buildLinkText(currEnv, isSelectedEnv)
            val link = ActionLink(asHtml(linkText)) {
                envsSupplier.setCurrent(currEnv)
            }
            link.toolTipText = toolTip

            val icon: Icon =
                if (hasUsageFunction(currEnv)) Icons.ENVIRONMENT_HAS_USAGE else Icons.ENVIRONMENT_HAS_NO_USAGE
            val iconComponent = JBLabel(icon)

            val singlePanel = SingleEnvPanel()
            singlePanel.toolTipText = toolTip

            singlePanel.add(iconComponent)
            singlePanel.add(link)

            list.add(singlePanel)
        }

        return list
    }

    fun createToolTip(usageStatusResult: UsageStatusResult, envName: String): String {
        val envUsageStatus = usageStatusResult.environmentStatuses.firstOrNull { it.name.equals(envName) }
        val sb = StringBuilder()
        sb.append(envName)
        sb.append(envName)
        if (envUsageStatus != null) {
            sb.append("<br>")
            sb.append("Last data received: ")
            sb.append(prettyTimeOf(envUsageStatus.environmentLastRecordedTime))
            sb.append("<br>")
            sb.append("First data received: ")
            sb.append(prettyTimeOf(envUsageStatus.environmentFirstRecordedTime))
        }
        return asHtml(sb.toString())
    }

    fun buildEnvironmentWithUsages(usageStatusResult: UsageStatusResult): Set<String> {
        return usageStatusResult.codeObjectStatuses
            .map { it.environment }
            .toSet()
    }

    fun buildRelevantSortedEnvironments(
        envsSupplier: EnvironmentsSupplier,
        hasUsageFun: (String) -> Boolean
    ): List<String> {
        val builtEnvs = ArrayList<String>()
        val envsWithoutUsage = ArrayList<String>()

        var mineLocalEnv = ""

        for (currEnv in envsSupplier.getEnvironments()) {
            if (isEnvironmentLocal(currEnv)) {
                if (isLocalEnvironmentMine(currEnv)) {
                    mineLocalEnv = currEnv
                } else {
                    // skip other local (not mine)
                }
                continue
            } else {
                if (hasUsageFun(currEnv)) {
                    builtEnvs.add(currEnv)
                } else {
                    envsWithoutUsage.add(currEnv)
                }
            }
        }

        builtEnvs.sortWith(String.CASE_INSENSITIVE_ORDER)
        envsWithoutUsage.sortWith(String.CASE_INSENSITIVE_ORDER)
        if (mineLocalEnv.isNotBlank()) {
            builtEnvs.add(0, mineLocalEnv)
        }
        builtEnvs.addAll(envsWithoutUsage)
        return builtEnvs
    }

    fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv)) {
            txtValue = "LOCAL"
        }
        if (isSelectedEnv) {
            return spanBoldUnderLine(txtValue)
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
