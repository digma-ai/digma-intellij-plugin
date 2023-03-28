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
import org.apache.commons.lang3.StringUtils
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.PanelModel
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
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JList
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicComboPopup

const val NO_ENVIRONMENTS_MESSAGE: String = "No Environments"

class EnvironmentsDropdownPanel(
        project: Project,
        private val model: PanelModel,
        private val environmentsSupplier: EnvironmentsSupplier, // assuming its a singleton
        localHostname: String
) : DigmaResettablePanel(), Disposable {
    private val logger: Logger = Logger.getInstance(EnvironmentsDropdownPanel::class.java)

    private val messageBusConnection: MessageBusConnection = project.messageBus.connect()
    private val backendConnectionMonitor: BackendConnectionMonitor
    private val project: Project
    private val rebuildPanelLock = ReentrantLock()
    private val localHostname: String
    private val changeEnvAlarm: Alarm
    private val popupMenuOpened: AtomicBoolean = AtomicBoolean(false)
    private val wasNotInitializedYet: AtomicBoolean = AtomicBoolean(true)
    private val comboBox = ComboBox<String>()

    init {
        this.project = project
        this.localHostname = localHostname
        this.changeEnvAlarm = AlarmFactory.getInstance().create()
        layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        border = JBUI.Borders.empty(2, 6)
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        rebuildInBackground()

        messageBusConnection.subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,
                object : EnvironmentChanged {
                    //there are few instances of EnvironmentsPanel, if a button is clicked in the insights tab the selected button
                    //need to change also in the errors tab, and vice versa.
                    override fun environmentChanged(newEnv: String?) {
                        EDT.ensureEDT { select(newEnv) }
                    }

                    override fun environmentsListChanged(newEnvironments: MutableList<String>?) {
                        EDT.ensureEDT { rebuildInBackground() }
                    }
                })
        backendConnectionMonitor = project.getService(BackendConnectionMonitor::class.java)
    }

    override fun reset() {
        rebuildInBackground()
    }
    private var isDisposed = false;
    override fun dispose() {
        if(isDisposed) return;
        messageBusConnection.dispose()
        isDisposed = true;
    }

    private fun rebuildInBackground() {
        if (!popupMenuOpened.get() || wasNotInitializedYet.get() || backendConnectionMonitor.isConnectionError()) {
            val lifetimeDefinition = LifetimeDefinition()
            lifetimeDefinition.lifetime.launchBackground {
                rebuildPanelLock.lock()
                Log.log(logger::debug, "Lock acquired for rebuild EnvironmentsDropdownPanel process.")
                try {
                    rebuild()
                } finally {
                    rebuildPanelLock.unlock()
                    Log.log(logger::debug, "Lock released for rebuild EnvironmentsDropdownPanel process.")
                    lifetimeDefinition.terminate()
                }
            }
        }
    }

    private fun rebuild() {
        val environmentsInfo: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
        val usageStatusResult = model.getUsageStatus()
        val envsThatHaveUsageSet: Set<String> = buildEnvironmentWithUsages(usageStatusResult)
        val hasUsageFunction = fun(env: String): Boolean { return envsThatHaveUsageSet.contains(env) }
        val relevantEnvs = buildRelevantSortedEnvironments(environmentsSupplier, hasUsageFunction)
        for (currEnv in relevantEnvs) {
            val currentButtonInfo: MutableMap<String, Any> = HashMap()

            val isSelectedEnv: Boolean = currEnv.contentEquals(environmentsSupplier.getCurrent())
            val linkText = buildLinkText(currEnv)
            currentButtonInfo["isSelectedEnv"] = isSelectedEnv
            currentButtonInfo["linkText"] = linkText
            environmentsInfo[currEnv] = currentButtonInfo
        }
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildEnvironmentsDropdownPanelComponents(environmentsInfo, hasUsageFunction)
            revalidate()
        }
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
        val itemsWithIcons = mutableMapOf<String, Icon>()

        for (envInfo in environmentsInfo) {
            val buttonData = envInfo.value
            val currEnv = envInfo.key
            val linkText = buttonData.getValue("linkText").toString()
            val icon: Icon = if (hasUsageFunction(currEnv)) Laf.Icons.Environment.ENVIRONMENT_HAS_USAGE else Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE
            itemsWithIcons[linkText] = icon
        }

        // initialize comboBox with items list
        comboBox.model = DefaultComboBoxModel(itemsWithIcons.keys.toTypedArray())

        if (itemsWithIcons.keys.size > 0) {
            // this flag fixes initial load issue
            wasNotInitializedYet.set(false)

            comboBox.renderer = object : SimpleListCellRenderer<String>() {
                override fun customize(list: JList<out String>, value: String, index: Int, selected: Boolean, hasFocus: Boolean) {
                    text = value
                    icon = if (itemsWithIcons.size >= index && index >= 0) {
                        itemsWithIcons.values.elementAt(index)
                    } else {
                        null
                    }
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
                    val popupWidth = itemsWithIcons.keys.maxOfOrNull { comboBox.getFontMetrics(comboBox.font).stringWidth(it) }
                            ?: 0
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
            val cb = event.source as ComboBox<String>
            var selectedEnv = cb.selectedItem as String ?  // can be null if connection error and all item being removed from list (current logic)
            if(selectedEnv == null || selectedEnv == NO_ENVIRONMENTS_MESSAGE) return@addActionListener
            selectedEnv = adjustBackEnvNameIfNeeded(selectedEnv)
            val currEnv : String ? = environmentsSupplier.getCurrent()

            if (!StringUtils.equals(selectedEnv, currEnv)) {
                changeEnvAlarm.cancelAllRequests()
                changeEnvAlarm.addRequest({
                    environmentsSupplier.setCurrent(selectedEnv)
                }, 100)
            }

        }

        // Remove the border around the ComboBox
        comboBox.background = Laf.Colors.EDITOR_BACKGROUND
        comboBox.isOpaque = false
        // Set a fixed width for the closed ComboBox
        comboBox.preferredSize = Dimension(30, comboBox.preferredSize.height)
        val currentEnv = environmentsSupplier.getCurrent()
        if(currentEnv != null) comboBox.selectedItem = buildLinkText(currentEnv)

        comboBox.isEditable = false
        if (backendConnectionMonitor.isConnectionError()) {
            comboBox.removeAllItems()
            comboBox.addItem(NO_ENVIRONMENTS_MESSAGE)
        }
        else{
            if (comboBox.itemCount == 0) {
                // display default value
                comboBox.addItem(NO_ENVIRONMENTS_MESSAGE)
            }
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

    private fun select(newSelectedEnv: String?) {
        val currentSelected: String = comboBox.selectedItem as String
        //both panels will catch the event,the one that generated the event will be ignored and not changed.
        if (Objects.equals(currentSelected, newSelectedEnv)) {
            return
        }

        if (newSelectedEnv == null) {
            return
        }
        comboBox.selectedItem = buildLinkText(newSelectedEnv)
    }

    private fun buildLinkText(currEnv: String): String {
        var txtValue = currEnv
        if (isLocalEnvironmentMine(currEnv, localHostname)) {
            txtValue = LOCAL_ENV
        }
        return txtValue
    }
}