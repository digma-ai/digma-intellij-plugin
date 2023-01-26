package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.containers.stream
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.CommonUtils.prettyTimeOf
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.common.Laf.Icons.Environment.Companion.ENVIRONMENT_HAS_NO_USAGE
import org.digma.intellij.plugin.ui.common.Laf.Icons.Environment.Companion.ENVIRONMENT_HAS_USAGE
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.Dimension
import java.awt.FlowLayout
import java.lang.Integer.max
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingUtilities

//need to remember we have few instances of this panel , one on every main tab.
//all instances need to be in sync with the selected button and the environments list.
class EnvironmentsPanel(
    project: Project,
    private val model: PanelModel,
    private val environmentsSupplier: EnvironmentsSupplier, // assuming its a singleton
) : DigmaResettablePanel() {
    private val logger: Logger = Logger.getInstance(EnvironmentsPanel::class.java)

    private val project: Project
    private val changeEnvAlarm: Alarm
    private val localHostname: String
    private val rebuildPanelLock = ReentrantLock()

    init {
        this.project = project
        changeEnvAlarm = AlarmFactory.getInstance().create()
        localHostname = CommonUtils.getLocalHostname()
        isOpaque = false
        layout = WrapLayout(FlowLayout.LEFT, 2, 0)
        rebuildInBackground()

        project.messageBus.connect(project.getService(AnalyticsService::class.java))
            .subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
                //there are few instances of EnvironmentsPanel, if a button is clicked in the insights tab the selected button
                //need to change also in the errors tab, and vice versa.
                override fun environmentChanged(newEnv: String?) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        select(newEnv)
                    } else {
                        SwingUtilities.invokeLater {
                            select(newEnv)
                        }
                    }
                }

                override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        rebuildInBackground()
                    } else {
                        SwingUtilities.invokeLater {
                            rebuildInBackground()
                        }
                    }
                }
            })
    }


    override fun reset() {
        rebuildInBackground()
    }

    /*
        usually this panel works fine.
        there is one issue: sometimes when the plugin window opens on startup the WrapLayout computes a large
        vertical height and the panel takes too much vertical space.
        Its noticed that when the computed width is a negative number it means the calculation is not good. in that
        case this method will calculate a preferred size based on the buttons size. its usually only on first
        initialization of the tool window.
    */
    override fun getPreferredSize(): Dimension {
        val ps = super.getPreferredSize()
        if (ps != null) {
            return if (ps.width > 0 && ps.height > 0) {
                ps
            } else {
                computePreferredSize()
            }
        }

        return super.getPreferredSize()
    }

    private fun computePreferredSize(): Dimension {

        var tabPanel = parent
        while ((tabPanel != null) && (tabPanel !is DigmaTabPanel)) {
            tabPanel = tabPanel.parent
        }

        if ((tabPanel != null) && (tabPanel.size.width > 0) && components.isNotEmpty()) {
            val width = tabPanel.size.width - (this.insets.left + this.insets.right)
            var componentsWidth = 0
            var componentsHeight = 0
            components.forEach {
                componentsWidth += it.preferredSize.width + (layout as WrapLayout).hgap
                componentsHeight = max(componentsHeight, it.preferredSize.height)
            }

            val lines = (componentsWidth / width) + 1
            val height = lines * (componentsHeight + ((layout as WrapLayout).vgap) * 2)
            return Dimension(width, height)
        }

        return super.getPreferredSize()
    }


    private fun select(newSelectedEnv: String?) {
        val currentSelected: EnvLink? = getSelected()
        if (currentSelected != null) {
            //both panels will catch the event,the one that generated the event will be ignored and not changed.
            if (Objects.equals(currentSelected.env, newSelectedEnv)) {
                return
            }
            currentSelected.deselect { buildLinkText(it, false) }
        }

        if (newSelectedEnv == null) {
            return
        }

        val toSelectPanel: SingleEnvPanel? =
            (components.stream().filter { (it as SingleEnvPanel).myLink.env == newSelectedEnv }.findAny()
                .orElse(null) as SingleEnvPanel?)
        toSelectPanel?.myLink?.select { buildLinkText(it, true) }

    }

    private fun rebuildInBackground() {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild Envs panel process.")
            try {
                rebuild()
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild Envs panel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild() {

        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }

        val usageStatusResult = model.getUsageStatus()

        val envsThatHaveUsageSet: Set<String> = buildEnvironmentWithUsages(usageStatusResult)
        val hasUsageFunction = fun(env: String): Boolean { return envsThatHaveUsageSet.contains(env) }

        val relevantEnvs = buildRelevantSortedEnvironments(environmentsSupplier, hasUsageFunction)


        for (currEnv in relevantEnvs) {
            val isSelectedEnv = currEnv.contentEquals(environmentsSupplier.getCurrent())
            val toolTip = buildToolTip(usageStatusResult, currEnv)
            val linkText = buildLinkText(currEnv, isSelectedEnv)

            SwingUtilities.invokeLater {
                buildEnvironmentsPanelButtons(currEnv, linkText, isSelectedEnv, toolTip, hasUsageFunction)
            }
        }
        revalidate()
    }

    private fun buildEnvironmentsPanelButtons(currEnv: String, linkText: String, isSelectedEnv: Boolean,
                                              toolTip: String, hasUsageFunction: (String) -> Boolean) {
        val envLink = EnvLink(currEnv, linkText, isSelectedEnv)
        envLink.toolTipText = toolTip

        envLink.addActionListener { event ->

            val currentSelected: EnvLink? = getSelected()

            if (currentSelected === event.source) {
                return@addActionListener
            }

            currentSelected?.deselect { buildLinkText(it, false) }

            val clickedLink: EnvLink = event.source as EnvLink
            clickedLink.select { buildLinkText(it, true) }

            changeEnvAlarm.cancelAllRequests()
            changeEnvAlarm.addRequest({
                environmentsSupplier.setCurrent(clickedLink.env)
            }, 100)

        }

        val icon: Icon = if (hasUsageFunction(currEnv)) ENVIRONMENT_HAS_USAGE else ENVIRONMENT_HAS_NO_USAGE
        val iconComponent = JBLabel(icon)

        val singlePanel = SingleEnvPanel(envLink, iconComponent)
        singlePanel.toolTipText = toolTip

        this.add(singlePanel)
    }

    private fun getSelected(): EnvLink? {
        val currentSelectedPanel: SingleEnvPanel? =
            (components.stream().filter { (it as SingleEnvPanel).myLink.isSelectedEnvironment() }.findAny()
                .orElse(null) as SingleEnvPanel?)
        return currentSelectedPanel?.myLink
    }

    private fun buildToolTip(usageStatusResult: UsageStatusResult, envName: String): String {
        val envUsageStatus = usageStatusResult.environmentStatuses.firstOrNull { it.name == envName }
        val sb = StringBuilder()
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

    private fun buildEnvironmentWithUsages(usageStatusResult: UsageStatusResult): Set<String> {
        return usageStatusResult.codeObjectStatuses
            .map { it.environment }
            .toSet()
    }

    private fun buildRelevantSortedEnvironments(
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

    private fun buildLinkText(currEnv: String, isSelectedEnv: Boolean): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv)) {
            txtValue = "LOCAL"
        }
        if (isSelectedEnv) {
            return asHtml(spanBoldUnderLine(txtValue))
        }
        return asHtml(span(txtValue))
    }

    private fun isEnvironmentLocal(environment: String): Boolean {
        return environment.endsWith("[local]", true)
    }

    private fun isLocalEnvironmentMine(environment: String): Boolean {
        return environment.startsWith(localHostname, true)
    }


    //this is the method called by the platform when requesting focus with ContentManager.setSelectedContent
    override fun requestFocus() {
        getSelected()?.requestFocusInWindow()
    }

    override fun requestFocusInWindow(): Boolean {
        requestFocus()
        return true
    }
}


class SingleEnvPanel(val myLink: EnvLink, iconComponent: JComponent) : JBPanel<SingleEnvPanel>() {
    init {
        isOpaque = false
        layout = FlowLayout(FlowLayout.LEFT, 3, 3)
        border = JBUI.Borders.empty(1)

        add(iconComponent)
        add(myLink)
    }
}


class EnvLink(val env: String, text: String, private var isSelectedEnv: Boolean = false) : ActionLink(text) {

    fun select(textSupplier: Function<String, String>) {
        this.text = textSupplier.apply(env)
        this.isSelectedEnv = true
    }

    fun deselect(textSupplier: Function<String, String>) {
        this.text = textSupplier.apply(env)
        this.isSelectedEnv = false
    }

    fun isSelectedEnvironment(): Boolean = isSelectedEnv
}
