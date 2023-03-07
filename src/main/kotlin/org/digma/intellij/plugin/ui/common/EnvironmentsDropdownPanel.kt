package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import com.intellij.util.AlarmFactory
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.analytics.EnvironmentChanged
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.usageStatusChange.UsageStatusChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class EnvironmentsDropdownPanel(
        project: Project,
        usageStatusResult: UsageStatusResult,
        private val environmentsSupplier: EnvironmentsSupplier, // assuming its a singleton
        localHostname: String
) : DigmaResettablePanel(), Disposable {
    private val logger: Logger = Logger.getInstance(EnvironmentsDropdownPanel::class.java)

    private val usageStatusChangeConnection: MessageBusConnection = project.messageBus.connect()
    private val usageStatusResult: UsageStatusResult
    private val project: Project
    private val rebuildPanelLock = ReentrantLock()
    private var selectedItem: String
    private val localHostname: String
    private val changeEnvAlarm: Alarm
    private val popupMenuOpened: AtomicBoolean = AtomicBoolean(false)
    // Determine the background color of the dark theme editor (example value used)
    private val darkThemeEditorBackgroundColor = JBColor.namedColor("Editor.background", Gray._50)
    // Create a new color object using the background color of the dark theme editor
    private val menuItemBackgroundColor = JBColor(darkThemeEditorBackgroundColor, darkThemeEditorBackgroundColor)

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
    }

    override fun reset() {
        rebuildInBackground(usageStatusResult)
    }

    override fun dispose() {
        usageStatusChangeConnection.dispose()
    }

    private fun rebuildInBackground(usageStatus: UsageStatusResult) {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild EnvironmentsDropdownPanel process.")
            try {
                rebuild(usageStatus)
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild EnvironmentsDropdownPanel process.")
                lifetimeDefinition.terminate()
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
        val dropdownLabel = object : JLabel() {
            override fun getPreferredSize(): Dimension {
                val size = super.getPreferredSize()
                size.width += 20 // Add some extra space to fit the "Select an item" text
                return size
            }
        }
        dropdownLabel.icon = getDropDownIcon()
        dropdownLabel.text = selectedItem
        dropdownLabel.horizontalTextPosition = SwingConstants.LEFT
        dropdownLabel.verticalTextPosition = SwingConstants.CENTER
        val popupMenu = JPopupMenu()
        // Add header to the popupMenu
        val header = createHeader("Environment", "Data")
        popupMenu.add(header)

        for (envInfo in environmentsInfo) {
            val buttonData = envInfo.value
            val currEnv = envInfo.key
            val linkText = buttonData.getValue("linkText").toString()
//            val isSelectedEnv = buttonData.getValue("isSelectedEnv") as Boolean

//            val icon: Icon = if (isSelectedEnv) Laf.Icons.Environment.ENVIRONMENT_HAS_USAGE else Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE
            val icon: Icon = if (hasUsageFunction(currEnv)) Laf.Icons.Environment.ENVIRONMENT_HAS_USAGE else Laf.Icons.Environment.ENVIRONMENT_HAS_NO_USAGE

            val menuItem = createMenuItem(linkText, icon)

            menuItem.addActionListener { event ->
                selectedItem = linkText // Update the selected item
                dropdownLabel.text = linkText

                val currentSelected: String = getSelected()

                if (currentSelected === event.source) {
                    return@addActionListener
                }

                changeEnvAlarm.cancelAllRequests()
                changeEnvAlarm.addRequest({
                    environmentsSupplier.setCurrent(currEnv)
                }, 100)
            }
            popupMenu.add(menuItem)
        }
        popupMenu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                dropdownLabel.icon = Laf.Icons.General.ARROW_UP
                popupMenuOpened.set(true)
            }
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                dropdownLabel.icon = Laf.Icons.General.ARROW_DOWN
                popupMenuOpened.set(false)
            }
            override fun popupMenuCanceled(e: PopupMenuEvent?) {
                // Do nothing
            }
        })
        dropdownLabel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                popupMenu.show(dropdownLabel, 0, dropdownLabel.height)
            }
        })

        this.add(dropdownLabel)
    }

    private fun getDropDownIcon(): Icon {
        return if (popupMenuOpened.get()) Laf.Icons.General.ARROW_UP else Laf.Icons.General.ARROW_DOWN
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

    private fun createMenuItem(text: String, icon: Icon): JMenuItem {
        val menuItem = JMenuItem()
        menuItem.layout = BorderLayout()

        // Create a panel for the text and icon labels with a flexible horizontal gap
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.LINE_AXIS)

        val textLabel = JLabel(text)
        textLabel.horizontalAlignment = SwingConstants.LEFT
        textLabel.font = Font("Helvetica", Font.PLAIN, 12)
        textLabel.border = JBUI.Borders.empty(0, 5)

        val iconLabel = JLabel(icon)
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.border = JBUI.Borders.empty(0, 5)

        panel.add(textLabel)
        panel.add(Box.createHorizontalGlue())
        panel.add(iconLabel)

        menuItem.add(panel, BorderLayout.CENTER)

        // Set the background color of the menu item to the dark theme editor background color
        menuItem.background = menuItemBackgroundColor
        panel.background = menuItemBackgroundColor

        // Add a mouse listener to highlight the menu item on mouse over
        menuItem.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                menuItem.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                menuItem.background = Color.DARK_GRAY
                panel.background = Color.DARK_GRAY
            }

            override fun mouseExited(e: MouseEvent) {
                menuItem.background = menuItemBackgroundColor
                panel.background = menuItemBackgroundColor
            }
        })
        // Remove the border around the focused menu item
        menuItem.border = BorderFactory.createEmptyBorder()

        return menuItem
    }

    private fun createHeader(leftText: String, rightText: String): JComponent {
        val headerPanel = JPanel()
        headerPanel.layout = BoxLayout(headerPanel, BoxLayout.LINE_AXIS)

        val leftLabel = JLabel(leftText)
        leftLabel.font = Font("SF Pro Text", Font.TRUETYPE_FONT, 10)
        leftLabel.horizontalAlignment = SwingConstants.LEFT
        leftLabel.border = JBUI.Borders.empty(0, 5)
        leftLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR

        val minGapSize = maxOf(10, SwingUtilities.computeStringWidth(leftLabel.getFontMetrics(leftLabel.font), leftText))

        headerPanel.add(leftLabel)

        headerPanel.add(Box.createHorizontalStrut(minGapSize)) // Add flexible horizontal gap

        val rightLabel = JLabel(rightText)
        rightLabel.font = Font("SF Pro Text", Font.TRUETYPE_FONT, 10)
        rightLabel.horizontalAlignment = SwingConstants.RIGHT
        rightLabel.border = JBUI.Borders.empty(0, 5)
        rightLabel.foreground = Laf.Colors.DROP_DOWN_HEADER_TEXT_COLOR

        headerPanel.add(rightLabel)
        headerPanel.background = menuItemBackgroundColor
        return headerPanel
    }
}