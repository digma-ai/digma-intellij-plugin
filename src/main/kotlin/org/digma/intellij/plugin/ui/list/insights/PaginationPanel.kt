package org.digma.intellij.plugin.ui.list.insights

import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.ui.common.asHtml
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

const val RECORDS_PER_PAGE = 3

fun rebuildPaginationPanel(paginationPanel: JPanel, currPageNum: Int, lastPageNum: Int, prev: ActionLink, next: ActionLink) {
    if (lastPageNum > 1) {
        paginationPanel.removeAll()
        buildPaginationPanel(paginationPanel, currPageNum, lastPageNum, prev, next)
    }
}

fun buildPaginationRowPanel(
        currPageNum: Int,
        lastPageNum: Int,
        paginationPanel: JPanel,
        prev: ActionLink,
        next: ActionLink
): JPanel? {
    if (lastPageNum < 2) {
        return null
    }
    buildPaginationPanel(paginationPanel, currPageNum, lastPageNum, prev, next)
    return paginationPanel
}

fun buildPaginationPanel(paginationPanel: JPanel, currPageNum: Int, lastPageNum: Int, prev: ActionLink, next: ActionLink) {
    paginationPanel.layout = BorderLayout()
    paginationPanel.border = JBUI.Borders.empty()
    paginationPanel.isOpaque = false

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