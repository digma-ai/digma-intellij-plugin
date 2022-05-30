package org.digma.intellij.plugin.ui.list.insights

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI.Borders
import org.digma.intellij.plugin.model.rest.insights.ErrorInsight
import org.digma.intellij.plugin.model.rest.insights.ErrorInsightNamedError
import org.digma.intellij.plugin.service.InsightsActionsService
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import java.awt.BorderLayout
import javax.swing.*

fun errorsPanel(project: Project, listViewItem: ListViewItem<ErrorInsight>): JPanel {

    val errorsPanel = panel {

        val errorCount = listViewItem.modelObject.errorCount
        val unhandled = listViewItem.modelObject.unhandledCount
        val unexpected = listViewItem.modelObject.unexpectedCount

        row {
            label("Errors")
                .bold()
                .verticalAlign(VerticalAlign.TOP)
                .horizontalAlign(HorizontalAlign.LEFT)
        }
        row {
            label(asHtml("$errorCount errors($unhandled unhandled, $unexpected unexpected)"))
        }
        row {
            panel {
                listViewItem.modelObject.topErrors.forEach {
                    row {
                        cell(topErrorPanel(it, listViewItem.modelObject))
                            .verticalAlign(VerticalAlign.CENTER)
                            .horizontalAlign(HorizontalAlign.LEFT)
                    }.layout(RowLayout.INDEPENDENT)
                }
            }
        }
        // this row is used so scroll bar would work
        row("") {
        }
    }


    val expandLinkPanel = panel {
        row {
            link("Expand") {
                val actionListener: InsightsActionsService = project.getService(InsightsActionsService::class.java)
                actionListener.errorsExpandButtonClicked(listViewItem.modelObject)
                //action here
            }.horizontalAlign(HorizontalAlign.RIGHT)
        }.layout(RowLayout.INDEPENDENT)
    }
    expandLinkPanel.border = Borders.empty()


    val errorsWrapper = JBPanel<JBPanel<*>>()
    errorsWrapper.layout = BorderLayout()
    errorsWrapper.add(errorsPanel, BorderLayout.CENTER)
    errorsWrapper.border = BorderFactory.createEmptyBorder()

    val scrollPane = JBScrollPane()
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    scrollPane.setViewportView(errorsWrapper)
    scrollPane.border = BorderFactory.createEmptyBorder()

    val expandPanel = JBPanel<JBPanel<*>>()
    expandPanel.layout = BorderLayout()
    expandPanel.add(expandLinkPanel, BorderLayout.SOUTH)


    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(scrollPane)
    result.add(Box.createHorizontalStrut(5))
    result.add(expandPanel)

    return insightItemPanel(result)
}


fun topErrorPanel(error: ErrorInsightNamedError, insight: ErrorInsight): JPanel {

    val result = panel {
        //temporary: need to implement logic
        row {
            link(error.errorType) {
                //error.codeObjectId
                println("In action")
            }
            var from = "From me"
            if (insight.codeObjectId != error.sourceCodeObjectId) {
                from = "From ${error.sourceCodeObjectId.split("\$_\$")[0]}"
            }
            label(from)
        }
    }

    result.border = Borders.empty()
    return result
}