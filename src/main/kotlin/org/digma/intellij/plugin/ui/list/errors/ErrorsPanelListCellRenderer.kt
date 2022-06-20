package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.createScorePanel
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.listItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel


class ErrorsPanelListCellRenderer : AbstractPanelListCellRenderer() {


    @Suppress("UNCHECKED_CAST")
    override fun createPanel(project: Project, value: ListViewItem<*>, index: Int): JPanel {
        return getOrCreatePanel(project,value as ListViewItem<CodeObjectError>)
    }

    private fun getOrCreatePanel(project: Project,value: ListViewItem<CodeObjectError>): JPanel {

        val model = value.modelObject

        return listItemPanel(createSingleErrorPanel(project,model))
    }

}

private fun createSingleErrorPanel(project: Project,model: CodeObjectError): JPanel {
    val contents = panel {
        row {
            link(asHtml(model.name)) {
                val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
                actionListener.showErrorDetails(model)
            }.verticalAlign(VerticalAlign.TOP)

            val relativeFrom: String
            if (model.startsHere) {
                relativeFrom = "me"
            } else {
                relativeFrom = extractMethodName(model.sourceCodeObjectId)
            }
            label(asHtml(" from $relativeFrom"))
        }
        row {
            label(model.characteristic)
                .bold()
        }
        row {
            label(contentAsHtmlOfFirstAndLast(model))
        }
    }
    contents.border = JBUI.Borders.empty(0)

    val scorePanel = createScorePanel(model)

    val result = JBPanel<JBPanel<*>>()
    result.layout = BoxLayout(result, BoxLayout.X_AXIS)
    result.add(contents)
    result.add(Box.createHorizontalStrut(5))
    result.add(scorePanel)

    return result
}

private fun prettyTimeOf(date: Date): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}

private fun contentAsHtmlOfFirstAndLast(model: CodeObjectError): String {
    return asHtml(
        "Started: <b>${prettyTimeOf(model.firstOccurenceTime)}</b>" +
                "  Last: <b>${prettyTimeOf(model.lastOccurenceTime)}<b>"
    )
}


