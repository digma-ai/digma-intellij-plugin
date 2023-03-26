package org.digma.intellij.plugin.ui.list.insights

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import io.ktor.util.reflect.*
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.model.insights.NoObservability
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.swing.*
import kotlin.math.max

private const val RECALCULATE = "Recalculate"
private const val REFRESH = "Refresh"

fun insightTitlePanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = empty(0, 5)
    return panel
}

fun insightItemPanel(panel: JPanel): JPanel {
    return commonListItemPanel(panel)
}

fun createInsightPanel(
        insight: CodeObjectInsight?,
        project: Project,
        title: String,
        description: String,
        iconsList: List<Icon>?,
        bodyPanel: JComponent?,
        buttons: List<JButton?>?,
        paginationComponent: JComponent?,
): JPanel {

    // .-----------------------------------.
    // | title                     | icons |
    // | description               |       |
    // |-----------------------------------|
    // | timeInfoMessagePanel              |
    // | bodyPanel                         |
    // | paginationPanel                   |
    // |-----------------------------------|
    // |                           buttons |
    // '-----------------------------------'

    val resultInsightPanel = buildInsightPanel(
            insight = insight,
            project = project,
            title = title,
            description = description,
            iconsList = iconsList,
            bodyPanel = bodyPanel,
            buttons = buttons,
            paginationComponent = paginationComponent,
    )

    return insightItemPanel(resultInsightPanel as DigmaResettablePanel)
}

private fun buildInsightPanel(
        insight: CodeObjectInsight?,
        project: Project,
        title: String,
        description: String,
        iconsList: List<Icon>?,
        bodyPanel: JComponent?,
        buttons: List<JButton?>?,
        paginationComponent: JComponent?,
): JPanel {
    val insightPanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildPanel(
                    insightPanel = this,
                    insight = insight,
                    project = project,
                    title = title,
                    description = description,
                    iconsList = iconsList,
                    bodyPanel = bodyPanel,
                    buttons = buttons,
                    paginationComponent = paginationComponent,
                    isRecalculateButtonPressed = true
            )
        }
    }
    return rebuildPanel(
            insightPanel = insightPanel,
            insight = insight,
            project = project,
            title = title,
            description = description,
            iconsList = iconsList,
            bodyPanel = bodyPanel,
            buttons = buttons,
            paginationComponent = paginationComponent,
            isRecalculateButtonPressed = false
    )
}

private fun rebuildPanel(
        insightPanel: JComponent?,
        insight: CodeObjectInsight?,
        project: Project,
        title: String,
        description: String,
        iconsList: List<Icon>?,
        bodyPanel: JComponent?,
        buttons: List<JButton?>?,
        paginationComponent: JComponent?,
        isRecalculateButtonPressed: Boolean
): JPanel {
    insightPanel!!.layout = BorderLayout()
    insightPanel.add(getMessageLabel(title, description), BorderLayout.WEST)
    insightPanel.add(getIconsListPanel(
            insight,
            project,
            iconsList,
            insightPanel as DigmaResettablePanel
    ), BorderLayout.EAST)

    if (bodyPanel != null || buttons != null) {
        val bodyWrapper = createDefaultBoxLayoutYAxisPanel()
        bodyWrapper.isOpaque = false

        if (insight != null && (insight.customStartTime != null || isRecalculateButtonPressed))
            bodyWrapper.add(getTimeInfoMessagePanel(
                    customStartTime = insight.customStartTime,
                    actualStartTime = insight.actualStartTime,
                    isRecalculateButtonPressed = isRecalculateButtonPressed,
                    project = project
            ))

        if (bodyPanel != null)
            bodyWrapper.add(bodyPanel)

        if (buttons != null) {
            val buttonsListPanel = getBasicEmptyListPanel()
            buttonsListPanel.border = JBUI.Borders.emptyTop(5)
            buttons.filterNotNull().forEach {
                buttonsListPanel.add(Box.createHorizontalStrut(5))
                buttonsListPanel.add(it)
            }
            bodyWrapper.add(buttonsListPanel)
        }

        if (paginationComponent != null) {
            bodyWrapper.add(getPaginationPanel(paginationComponent))
        }

        insightPanel.add(bodyWrapper, BorderLayout.SOUTH)
    }
    return insightPanel
}

private fun getTimeInfoMessagePanel(
        customStartTime: Date?,
        actualStartTime: Date?,
        isRecalculateButtonPressed: Boolean,
        project: Project
): JPanel {
    val formattedActualStartTime = actualStartTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    val diff: Duration = Duration.between(formattedActualStartTime, LocalDateTime.now())

    var formattedStartTime = ""
    if (!diff.isNegative && !diff.isZero) {
        formattedStartTime = getFormattedTimeDifference(diff)
    }

    val identicalStartTimes = customStartTime != null && actualStartTime?.compareTo(customStartTime) == 0

    val timeInfoMessage = if (shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed, identicalStartTimes)) {
        "Applying the new time filter. Wait a few minutes and then refresh."
    } else {
        "Data from: $formattedStartTime ago"
    }

    val timeInfoMessageLabel = JLabel(asHtml(timeInfoMessage))

    val timeInfoMessageLabelPanel = getDefaultSpanOneRecordPanel()
    timeInfoMessageLabelPanel.add(timeInfoMessageLabel, BorderLayout.NORTH)
    if (shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed, identicalStartTimes)) {
        timeInfoMessageLabelPanel.add(getRefreshInsightButton(project), BorderLayout.SOUTH)
    }
    return timeInfoMessageLabelPanel
}

private fun shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed: Boolean, identicalStartTimes: Boolean): Boolean {
    return isRecalculateButtonPressed || !identicalStartTimes
}

private fun getFormattedTimeDifference(diff: Duration): String {
    val builder = StringBuilder()
    if (diff.toDays() > 0) {
        builder.append(diff.toDays(), " days ")
    } else if (diff.toHoursPart() > 0) {
        builder.append(diff.toHoursPart(), " hours ")
    } else if (diff.toMinutesPart() > 0) {
        builder.append(diff.toMinutesPart(), " minutes ")
    } else {
        builder.append(1, " minute ")
    }
    return builder.toString()
}

private fun getBasicEmptyListPanel(): JPanel {
    val listPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
    listPanel.isOpaque = false
    listPanel.border = empty()
    return listPanel
}

private fun getPaginationPanel(paginationComponent: JComponent?): JPanel {
    val paginationPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    paginationPanel.isOpaque = false
    paginationPanel.border = empty()

    paginationPanel.add(Box.createHorizontalStrut(5))
    paginationPanel.add(paginationComponent)
    return paginationPanel
}

private fun getMessageLabel(title: String, description: String): JLabel {
    val messageLabel = JLabel(buildBoldTitleGrayedComment(title, description), SwingConstants.LEFT)
    messageLabel.isOpaque = false
    messageLabel.verticalAlignment = SwingConstants.TOP
    return messageLabel
}

private fun getIconsListPanel(
        insight: CodeObjectInsight?,
        project: Project,
        iconsList: List<Icon>?,
        insightPanel: DigmaResettablePanel
): JPanel {
    val icons = ArrayList<Icon>()
    if (iconsList != null) {
        icons.addAll(iconsList)
    }
    if (insight?.prefixedCodeObjectId != null) {
        icons.add(Laf.Icons.Insight.THREE_DOTS)
    }

    val iconsResultListPanel = getBasicEmptyListPanel()
    icons.forEach {
        iconsResultListPanel.add(Box.createHorizontalStrut(5))
        val iconLabel = JLabel(it, SwingConstants.RIGHT)
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.verticalAlignment = SwingConstants.TOP
        iconLabel.isOpaque = false
        iconLabel.border = empty(2, 2, 2, 4)

        if (it.instanceOf(ThreeDotsIcon::class)) {
            iconLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            iconLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    showHintMessage(
                            threeDotsIcon = iconLabel,
                            insightPanel = insightPanel,
                            codeObjectId = insight!!.prefixedCodeObjectId!!,
                            insightType = insight.type,
                            project = project
                    )
                }

                override fun mouseEntered(e: MouseEvent?) {
                    showHintMessage(
                            threeDotsIcon = iconLabel,
                            insightPanel = insightPanel,
                            codeObjectId = insight!!.prefixedCodeObjectId!!,
                            insightType = insight.type,
                            project = project
                    )
                }

                override fun mouseExited(e: MouseEvent?) {}
                override fun mousePressed(e: MouseEvent?) {}
            })
        }

        iconsResultListPanel.add(iconLabel)
    }
    return iconsResultListPanel
}

private fun showHintMessage(
        threeDotsIcon: JComponent,
        insightPanel: DigmaResettablePanel,
        codeObjectId: String,
        insightType: InsightType,
        project: Project
) {
    val analyticsService = AnalyticsService.getInstance(project)
    val recalculateAction = ActionLink(RECALCULATE)
    recalculateAction.addActionListener {
        analyticsService.setInsightCustomStartTime(codeObjectId, insightType)
        rebuildInsightPanel(insightPanel)
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("recalculate")
    }
    recalculateAction.border = HintUtil.createHintBorder()
    recalculateAction.background = HintUtil.getInformationColor()
    recalculateAction.isOpaque = true
    HintManager.getInstance().showHint(recalculateAction, RelativePoint.getSouthWestOf(threeDotsIcon), HintManager.HIDE_BY_ESCAPE, 2000)
}

private fun getRefreshInsightButton(project: Project): ActionLink {
    val refreshAction = ActionLink(REFRESH)
    refreshAction.addActionListener {
        val refreshService: RefreshService = project.getService(RefreshService::class.java)
        refreshService.refreshAllInBackground()
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("refresh")
    }
    refreshAction.border = empty()
    refreshAction.isOpaque = false
    return refreshAction
}

private fun rebuildInsightPanel(insightPanel: DigmaResettablePanel) {
    insightPanel.removeAll()
    insightPanel.reset()
}

fun genericPanelForSingleInsight(project: Project, modelObject: Any?): JPanel {

    return createInsightPanel(
            project = project,
            insight = modelObject as CodeObjectInsight,
            title = "Generic insight panel",
            description = "Insight named ${modelObject.javaClass.simpleName}",
            iconsList = listOf(Laf.Icons.Insight.QUESTION_MARK),
            bodyPanel = null,
            buttons = null,
            paginationComponent = null
    )
}


internal fun getInsightIconPanelRightBorderSize(): Int {
    return 5
}

internal fun getCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper, width: Int): Int {
    //this method should never return null and never throw NPE
    val currentLargest: Int =
            (layoutHelper.getObjectAttribute("insightsIconPanelBorder", "largestWidth") ?: 0) as Int
    return max(width, currentLargest)
}

internal fun addCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper, width: Int) {
    //this method should never throw NPE
    val currentLargest: Int =
            (layoutHelper.getObjectAttribute("insightsIconPanelBorder", "largestWidth") ?: 0) as Int
    layoutHelper.addObjectAttribute("insightsIconPanelBorder", "largestWidth",
            max(currentLargest, width))
}

private const val NoDataYetDescription = "No data received yet for this span, please trigger some actions using this code to see more insights."

fun noDataYetInsightPanel(): JPanel {

    val thePanel = object : DigmaResettablePanel() {
        override fun reset() {
        }
    }
    thePanel.layout = BorderLayout()
    thePanel.add(getMessageLabel("No Data Yet", ""), BorderLayout.WEST)
    thePanel.add(JLabel(asHtml(NoDataYetDescription)), BorderLayout.SOUTH)

    return insightItemPanel(thePanel as DigmaResettablePanel)
}

fun noObservabilityInsightPanel(project: Project, insight: NoObservability): JPanel {

    val methodId = insight.methodId
    val model = MethodInstrumentationPresenter(project)
    model.update(methodId)

    val body = panel {
        row {
            label(asHtml(NO_OBSERVABILITY_MISSING_DEPENDENCY_DESCRIPTION))
        }
        row {
            val textArea = textArea().text(model.missingDependency ?: "")
            textArea.component.isEditable = false
            textArea.component.background = Laf.Colors.EDITOR_BACKGROUND
            textArea.component.lineWrap = true
            textArea.horizontalAlign(HorizontalAlign.FILL)
        }
    }
    body.isOpaque = false

    val addAnnotationButton = ListItemActionButton("Add Annotation")
    addAnnotationButton.addActionListener{
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("add-annotation")
        val succeeded =  model.instrumentMethod()
        if(!succeeded){
            NotificationUtil.notifyError(project, "Failed to add annotation")
        }
    }

    if(model.canInstrumentMethod){
        addAnnotationButton.isEnabled = true
        body.isVisible = false
    }
    else {
        addAnnotationButton.isEnabled = false
        body.isVisible = model.cannotBecauseMissingDependency
    }

    return createInsightPanel(
        project = project,
        insight = null,
        title = "No Observability",
        description = NO_OBSERVABILITY_DETAIL_DESCRIPTION,
        iconsList = emptyList(),
        bodyPanel = body,
        buttons = listOf(addAnnotationButton),
        paginationComponent = null
    )
}

class InsightAlignedPanel(private val layoutHelper: PanelsLayoutHelper) : JPanel() {

    init {
        border = JBUI.Borders.emptyRight(getInsightIconPanelRightBorderSize())
    }

    override fun getPreferredSize(): Dimension {
        val ps = super.getPreferredSize()
        if (ps == null) {
            return ps
        }
        val h = ps.height
        val w = ps.width
        addCurrentLargestWidthIconPanel(layoutHelper, w)
        return Dimension(getCurrentLargestWidthIconPanel(layoutHelper, w), h)
    }
}