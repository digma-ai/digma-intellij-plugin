package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.usageStatusChange.UsageStatusChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JList
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboPopup

const val NO_ENVIRONMENTS_MESSAGE: String = "No Environments"

class EnvironmentsDropdownPanel(
        project: Project,
        usageStatusResult: UsageStatusResult,
        private val environmentsSupplier: EnvironmentsSupplier, // assuming its a singleton
        localHostname: String
) : DigmaResettablePanel(), Disposable {
    private val logger: Logger = Logger.getInstance(EnvironmentsDropdownPanel::class.java)

    private val usageStatusChangeConnection: MessageBusConnection = project.messageBus.connect()
    private val backendConnectionMonitor: BackendConnectionMonitor
    private var usageStatusResult: UsageStatusResult
    private val project: Project
    private val rebuildPanelLock = ReentrantLock()
    private var selectedItem: String
    private val localHostname: String
    private val changeEnvAlarm: Alarm
    private val popupMenuOpened: AtomicBoolean = AtomicBoolean(false)
    private val wasNotInitializedYet: AtomicBoolean = AtomicBoolean(true)

    init {
        usageStatusChangeConnection.subscribe(
                UsageStatusChangeListener.USAGE_STATUS_CHANGED_TOPIC,
                UsageStatusChangeListener { newUsageStatus -> rebuildInBackground(newUsageStatus) }
        )
        this.usageStatusResult = usageStatusResult
        this.project = project
        this.localHostname = localHostname
        this.changeEnvAlarm = AlarmFactory.getInstance().create()
        layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        border = JBUI.Borders.empty(2, 6)
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        rebuildInBackground(usageStatusResult)
        selectedItem = getSelected()

        project.messageBus.connect(project.getService(AnalyticsService::class.java))
                .subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC, object : EnvironmentChanged {
                    //there are few instances of EnvironmentsPanel, if a button is clicked in the insights tab the selected button
                    //need to change also in the errors tab, and vice versa.
                    override fun environmentChanged(newEnv: String?) {
                        EDT.ensureEDT{select(newEnv)}
                    }

                    override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                        EDT.ensureEDT{rebuildInBackground(usageStatusResult)}
                    }
                })
        backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
    }

    override fun reset() {
        rebuildInBackground(usageStatusResult)
    }

    override fun dispose() {
        usageStatusChangeConnection.dispose()
    }

    private fun rebuildInBackground(newUsageStatus: UsageStatusResult) {
        if (!popupMenuOpened.get()) {
            if (newUsageStatus.environmentStatuses.size != usageStatusResult.environmentStatuses.size
                    || (usageStatusResult.codeObjectStatuses.isEmpty() && newUsageStatus.environmentStatuses.isNotEmpty())
                    || wasNotInitializedYet.get()
                    || backendConnectionMonitor.isConnectionError()) {
                usageStatusResult = newUsageStatus

                val lifetimeDefinition = LifetimeDefinition()
                lifetimeDefinition.lifetime.launchBackground {
                    rebuildPanelLock.lock()
                    Log.log(logger::debug, "Lock acquired for rebuild EnvironmentsDropdownPanel process.")
                    try {
                        rebuild(newUsageStatus)
                    } finally {
                        rebuildPanelLock.unlock()
                        Log.log(logger::debug, "Lock released for rebuild EnvironmentsDropdownPanel process.")
                        lifetimeDefinition.terminate()
                    }
                }
            }
        }
    }

    private fun rebuild(usageStatus: UsageStatusResult) {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildEnvironmentsComponent(usageStatus)
            revalidate()
        }
    }

    private fun buildEnvironmentsComponent(usageStatus: UsageStatusResult) {
        val environmentsInfo: MutableMap<String, MutableMap<String,Any>> = mutableMapOf()
        val envsThatHaveUsageSet: Set<String> = buildEnvironmentWithUsages(usageStatus)
        val hasUsageFunction = fun(env: String): Boolean { return envsThatHaveUsageSet.contains(env) }
        val relevantEnvs = buildRelevantSortedEnvironments(environmentsSupplier, hasUsageFunction)
        for (currEnv in relevantEnvs) {
            val currentButtonInfo: MutableMap<String,Any> = HashMap()

            val isSelectedEnv: Boolean = currEnv.contentEquals(environmentsSupplier.getCurrent())
            val linkText = buildLinkText(currEnv)
            if (isSelectedEnv) {
                selectedItem = linkText
            }

            currentButtonInfo["isSelectedEnv"] = isSelectedEnv
            currentButtonInfo["linkText"] = linkText
            environmentsInfo[currEnv] = currentButtonInfo
        }
        buildEnvironmentsDropdownPanelComponents(environmentsInfo, hasUsageFunction)
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
                if (isLocalEnvironmentMine(currEnv, localHostname)) {
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

    private fun buildEnvironmentsDropdownPanelComponents(
            environmentsInfo: MutableMap<String, MutableMap<String, Any>>,
            hasUsageFunction: (String) -> Boolean
    ) {
        val items = mutableListOf<String>()
        val icons = mutableListOf<Icon>()

        for (envInfo in environmentsInfo) {
            val buttonData = envInfo.value
            val currEnv = envInfo.key
            val linkText = buttonData.getValue("linkText").toString()
            val icon: Icon = if (hasUsageFunction(currEnv)) Laf.Icons.Environment.ENVIRONMENT_HAS_USAGE else Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE
            items.add(linkText)
            icons.add(icon)
        }

        val comboBox = ComboBox(items.toTypedArray())
        if (items.size > 0) {
            // this flag fixes initial load issue
            wasNotInitializedYet.set(false)

            comboBox.renderer = object : SimpleListCellRenderer<String>() {
                override fun customize(list: JList<out String>, value: String, index: Int, selected: Boolean, hasFocus: Boolean) {
                    text = value
                    icon = icons.getOrElse(index) { null }
                    foreground = if (selected) JBColor.WHITE else JBColor.BLACK
                    background = if (selected) JBColor.BLUE else JBColor.WHITE
                }
            }
        }

        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                popupMenuOpened.set(true)
                // Set the width of the ComboBox to the width of the widest item when the popup menu is opened
                val popup = comboBox.getUI().getAccessibleChild(comboBox, 0)
                val popupList = (popup as? BasicComboPopup)?.list
                if (popupList != null) {
                    val popupWidth = items.maxOfOrNull { comboBox.getFontMetrics(comboBox.font).stringWidth(it) } ?: 0
                    comboBox.preferredSize = Dimension(popupWidth + 40, comboBox.preferredSize.height)
                }
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                // Reset the width of the ComboBox to the fixed width when the popup menu is closed
                comboBox.preferredSize = Dimension(30, comboBox.preferredSize.height)
                popupMenuOpened.set(false)
            }

            override fun popupMenuCanceled(e: PopupMenuEvent?) {
                // Reset the width of the ComboBox to the fixed width when the popup menu is canceled
                comboBox.preferredSize = Dimension(30, comboBox.preferredSize.height)
            }
        })

        comboBox.addActionListener { event ->
            selectedItem = comboBox.selectedItem?.toString() ?: "" // Update the selected item

            val currentSelected: String = getSelected()

            if (currentSelected === event.source) {
                return@addActionListener
            }

            changeEnvAlarm.cancelAllRequests()
            changeEnvAlarm.addRequest({
                environmentsSupplier.setCurrent(adjustBackEnvNameIfNeeded(selectedItem))
            }, 100)
        }

        // Remove the border around the ComboBox
        comboBox.background = Laf.Colors.EDITOR_BACKGROUND
        comboBox.isOpaque = false
        // Set a fixed width for the closed ComboBox
        comboBox.preferredSize = Dimension(30, comboBox.preferredSize.height)
        comboBox.selectedItem = getSelected()
        comboBox.isEditable = false

        if (comboBox.itemCount == 0) {
            // display default value
            comboBox.addItem(NO_ENVIRONMENTS_MESSAGE)
        }
        if (backendConnectionMonitor.isConnectionError()) {
            comboBox.removeAllItems()
            comboBox.addItem(NO_ENVIRONMENTS_MESSAGE)
        }

        this.add(comboBox)

        // Add ComponentListener to detect resizing of parent component
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                comboBox.hidePopup()
            }
        })

        // Add MouseListener to detect click outside of ComboBox
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!comboBox.bounds.contains(e.point)) {
                    comboBox.hidePopup()
                }
            }
        })
    }

    private fun adjustBackEnvNameIfNeeded(environment: String): String {
        return if (environment.equals(LOCAL_ENV, ignoreCase = true)) {
            (localHostname + SUFFIX_OF_LOCAL).uppercase(Locale.getDefault())
        } else environment
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun getSelected(): String {
        return selectedItem
    }

    private fun select(newSelectedEnv: String?) {
        val currentSelected: String = getSelected()
        //both panels will catch the event,the one that generated the event will be ignored and not changed.
        if (Objects.equals(currentSelected, newSelectedEnv)) {
            return
        }

        if (newSelectedEnv == null) {
            return
        }

        buildLinkText(newSelectedEnv)
    }

    private fun buildLinkText(currEnv: String): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv, localHostname)) {
            txtValue = LOCAL_ENV
        }
        return txtValue
    }
}