package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.Alarm
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.CommonUtils
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.LOCAL_ENV
import org.digma.intellij.plugin.common.LOCAL_TESTS_ENV
import org.digma.intellij.plugin.common.isEnvironmentLocal
import org.digma.intellij.plugin.common.isEnvironmentLocalTests
import org.digma.intellij.plugin.common.isLocalEnvironmentMine
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.Cursor
import java.awt.event.ItemListener
import javax.swing.JList

class EnvironmentsCombo(val project: Project, navigationPanel: NavigationPanel) : ComboBox<EnvironmentsCombo.EnvItem>(CollectionComboBoxModel()) {

    private val myParentDisposable = project.service<AnalyticsService>()
    private val selectionAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, myParentDisposable)

    init {

        background = Laf.Colors.EDITOR_BACKGROUND
        isOpaque = false
        renderer = MyRenderer()
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        isSwingPopup = false

        buildAndUpdateModel()



        this.addItemListener(ItemListener {

            if (it.stateChange == java.awt.event.ItemEvent.DESELECTED) {
                return@ItemListener
            }

            it.item?.let { item ->
                val selected = (item as EnvItem).text
                val currentEnv = project.service<AnalyticsService>().environment.getCurrent()
                if (!StringUtils.equals(currentEnv, selected)) {
                    selectionAlarm.cancelAllRequests()
                    selectionAlarm.addRequest({
                        //actually setting environment here will cause an environmentChanged which is
                        // handled also here by this combo, and it should be the trigger to change back the cursor
                        // and enable
                        cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        navigationPanel.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        isEnabled = false
                        project.service<AnalyticsService>().environment.setCurrent(selected)
                    }, 100)
                }
            }
        })


        project.messageBus.connect(myParentDisposable).subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
            override fun environmentChanged(newEnv: String?, refreshInsightsView: Boolean) {

                cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                navigationPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                isEnabled = true

                if (selectedItem == null || newEnv == null) {
                    return
                }

                //otherwise we have a stack overflow because selection will change environment too
                if ((selectedItem as? EnvItem)?.text == newEnv) {
                    return
                }
                EDT.ensureEDT {
                    model.selectedItem =
                        (model as CollectionComboBoxModel).items.find { envItem -> envItem.text == newEnv }
                            ?: (model as CollectionComboBoxModel).items.firstOrNull()
                }
            }

            override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                buildAndUpdateModel()
            }
        })


        project.messageBus.connect(myParentDisposable).subscribe(
            ModelChangeListener.MODEL_CHANGED_TOPIC, ModelChangeListener {
                if (isPopupVisible) return@ModelChangeListener
                buildAndUpdateModel()
            })

    }


    private fun buildAndUpdateModel() {
        val envs = buildEnvsList()
        EDT.ensureEDT {
            (model as CollectionComboBoxModel).removeAll()
            (model as CollectionComboBoxModel).add(envs)
            model.selectedItem = envs.find { envItem -> envItem.isSelected } ?: envs.firstOrNull()
        }
    }


    private fun buildEnvsList(): List<EnvItem> {
        val environmentsSupplier: EnvironmentsSupplier = project.service<AnalyticsService>().environment

        if (environmentsSupplier.getEnvironments().isEmpty()) {
            return listOf()
        }

        val usageStatusesOfInsights = project.service<InsightsViewService>().model.usageStatusResult
        val usageStatusesOfErrors = project.service<ErrorsViewService>().model.usageStatusResult

        val envsThatHaveUsageSet = getEnvsWithUsages(usageStatusesOfInsights, usageStatusesOfErrors)

        return buildRelevantSortedEnvironments(environmentsSupplier) { env: String -> envsThatHaveUsageSet.contains(env) }
    }

    private fun buildRelevantSortedEnvironments(environmentsSupplier: EnvironmentsSupplier, hasUsageFunction: (String) -> Boolean): List<EnvItem> {

        val localHostName = CommonUtils.getLocalHostname()

        val envs = mutableListOf<EnvItem>()

        //value of the environments is never changed, only rendering changes
        environmentsSupplier.getEnvironments().forEach { env ->

            val isSelected = environmentsSupplier.getCurrent() == env
            val isMine = isLocalEnvironmentMine(env, localHostName)

            val isLocalEnv = isEnvironmentLocal(env)
            val isLocalEnvMine = isLocalEnv && isMine

            val isLocalTestsEnv = isEnvironmentLocalTests(env)
            val isLocalTestsEnvMine = isLocalTestsEnv && isMine

            if ((!isLocalEnv && !isLocalTestsEnv) || isLocalEnvMine || isLocalTestsEnvMine) {
                envs.add(EnvItem(env, isSelected, hasUsageFunction(env), isLocalEnvMine, isLocalTestsEnvMine))
            }
        }

        return envs.sortedWith(EnvItemComparator())
    }


    private fun getEnvsWithUsages(usageStatusesOfInsights: UsageStatusResult, usageStatusesOfErrors: UsageStatusResult): Set<String> {
        val envsWithUsages = mutableSetOf<String>()
        usageStatusesOfInsights.codeObjectStatuses.forEach { codeObjectUsageStatus -> envsWithUsages.add(codeObjectUsageStatus.environment) }
        usageStatusesOfErrors.codeObjectStatuses.forEach { codeObjectUsageStatus -> envsWithUsages.add(codeObjectUsageStatus.environment) }
        return envsWithUsages
    }


    class EnvItem(
        val text: String, val isSelected: Boolean = false, val hasUsage: Boolean = false,
        val isMine: Boolean = false, val isMineTests: Boolean = false,
    )

    private class EnvItemComparator : Comparator<EnvItem> {
        override fun compare(env1: EnvItem?, env2: EnvItem?): Int {
            if (env1 == null && env2 == null) {
                return 0
            }
            if (env1 == null) {
                return -1
            }
            if (env2 == null) {
                return 1
            }

            if (env1.isMine) {
                return -1
            }
            if (env2.isMine) {
                return 1
            }
            if (env1.isMineTests) {
                return -1
            }
            if (env2.isMineTests) {
                return 1
            }

            if (env1.hasUsage && env2.hasUsage) {
                return String.CASE_INSENSITIVE_ORDER.compare(env1.text, env2.text)
            }
            if (env1.hasUsage) {
                return -1
            }
            if (env2.hasUsage) {
                return 1
            }
            return 0
        }

    }


    private class MyRenderer : SimpleListCellRenderer<EnvItem>() {

        @Suppress("DialogTitleCapitalization")
        override fun customize(list: JList<out EnvItem>, envItem: EnvItem?, index: Int, selected: Boolean, hasFocus: Boolean) {

            this.iconTextGap = 20

            if (envItem == null) {
                text = "No Environments"
                icon = null
            } else {
                text = evalText(envItem)
                icon = if (envItem.hasUsage) Laf.Icons.Environment.ENVIRONMENT_HAS_USAGE else Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE
                this.toolTipText = envItem.text + if (envItem.isMine) "(my)" else ""
            }
        }

        fun evalText(envItem: EnvItem): String {
            if (envItem.isMine)
                return LOCAL_ENV

            if (envItem.isMineTests)
                return LOCAL_TESTS_ENV

            return envItem.text
        }

    }

}
