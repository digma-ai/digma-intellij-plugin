package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.editor.*
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


fun <T> rebuildPaginationPanel(
        paginationPanel: JPanel,
        lastPageNum: Int,
        entries: List<T>,
        resultInsightBodyPanel: DigmaResettablePanel?,
        entriesToDisplay: ArrayList<T>,
        uniqueInsightId: String,
        recordsPerPage: Int,
        project: Project
) {
    if (lastPageNum > 1) {
        paginationPanel.removeAll()
        buildPaginationPanel(
            paginationPanel, lastPageNum,
            entries, resultInsightBodyPanel, entriesToDisplay, uniqueInsightId, recordsPerPage, project
        )
    }
}

fun <T> buildPaginationRowPanel(
    lastPageNum: Int,
    paginationPanel: JPanel,
    entries: List<T>,
    resultInsightBodyPanel: DigmaResettablePanel,
    entriesToDisplay: ArrayList<T>,
    uniqueInsightId: String,
    recordsPerPage: Int,
    project: Project,
): JPanel? {
    if (lastPageNum < 2) {
        return null
    }
    buildPaginationPanel(paginationPanel, lastPageNum,
            entries, resultInsightBodyPanel, entriesToDisplay, uniqueInsightId, recordsPerPage, project)
    return paginationPanel
}

private fun <T> updateInsightBodyPanelWithItemsToDisplay(
        entries: List<T>,
        entriesToDisplay: ArrayList<T>,
        uniqueInsightId: String,
        currPageNum: Int,
        recordsPerPage: Int
) {
    val focusedDocumentName = getFocusedDocumentName()
    addInsightPaginationInfo(focusedDocumentName, uniqueInsightId, currPageNum)
    updateListOfEntriesToDisplay(entries, entriesToDisplay, currPageNum, recordsPerPage)
}
private fun <T> updateInsightBodyPanelWithItemsToDisplay(
        entries: List<T>,
        resultInsightBodyPanel: DigmaResettablePanel?,
        entriesToDisplay: ArrayList<T>,
        uniqueInsightId: String,
        currPageNum: Int,
        recordsPerPage: Int
) {
    updateInsightBodyPanelWithItemsToDisplay(entries, entriesToDisplay, uniqueInsightId, currPageNum, recordsPerPage)

    resultInsightBodyPanel?.reset()
}

fun <T> buildPaginationPanel(
    paginationPanel: JPanel,
    lastPageNum: Int,
    entries: List<T>,
    resultInsightBodyPanel: DigmaResettablePanel?,
    entriesToDisplay: ArrayList<T>,
    uniqueInsightId: String,
    recordsPerPage: Int,
    project: Project
) {
    val prev = ActionLink("Prev")
    val next = ActionLink("Next")
    paginationPanel.layout = BorderLayout()
    paginationPanel.border = JBUI.Borders.empty()
    paginationPanel.isOpaque = false
    var currPageNum = getCurrentPageNumberForInsight(uniqueInsightId, lastPageNum)

    prev.addActionListener {
        if (--currPageNum <= 0) currPageNum = 1
        updateInsightBodyPanelWithItemsToDisplay(entries, resultInsightBodyPanel, entriesToDisplay, uniqueInsightId, currPageNum, recordsPerPage)
        ActivityMonitor.getInstance( project).registerInsightButtonClicked("previous-page")
    }
    next.addActionListener {
        if (++currPageNum > lastPageNum) currPageNum = lastPageNum
        updateInsightBodyPanelWithItemsToDisplay(entries, resultInsightBodyPanel, entriesToDisplay, uniqueInsightId, currPageNum, recordsPerPage)
        ActivityMonitor.getInstance(project).registerInsightButtonClicked("next-page")
    }

    val paginationLabelText = "$currPageNum of $lastPageNum"
    val paginationLabel = JLabel(asHtml(paginationLabelText), SwingConstants.LEFT)
    paginationLabel.border = JBUI.Borders.emptyLeft(5)

    prev.border = JBUI.Borders.emptyRight(3)
    paginationPanel.add(prev, BorderLayout.WEST)
    paginationPanel.add(next, BorderLayout.CENTER)
    paginationPanel.add(paginationLabel, BorderLayout.EAST)

    val canGoBack = currPageNum > 1
    val canGoFwd = currPageNum != lastPageNum
    prev.isEnabled = canGoBack
    next.isEnabled = canGoFwd
}