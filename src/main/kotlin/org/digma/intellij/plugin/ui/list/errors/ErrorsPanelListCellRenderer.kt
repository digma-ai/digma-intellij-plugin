package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.createScorePanel
import org.digma.intellij.plugin.ui.common.htmlSpanSmoked
import org.digma.intellij.plugin.ui.common.htmlSpanWhite
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.listItemPanel
import org.digma.intellij.plugin.ui.model.listview.ListViewItem
import org.ocpsoft.prettytime.PrettyTime
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.*
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

    val relativeFrom = if (model.startsHere) {
        "me"
    } else {
        extractMethodName(model.sourceCodeObjectId)
    }
    val linkText = "<b>${model.name}<b> ${htmlSpanSmoked()}from ${htmlSpanWhite()}$relativeFrom"
    val link = ActionLink(asHtml(linkText)){
        val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
        actionListener.showErrorDetails(model)
    }

    val firstAndLast = contentOfFirstAndLast(model)
    val contentText = "${htmlSpanWhite()}${model.characteristic}<br> ${firstAndLast}"
    val content = JBLabel(asHtml(contentText))

    link.toolTipText = asHtml("${linkText}<br>${firstAndLast}" )

    val scorePanel = createScorePanel(model)
    val scorePanelWrapper = JPanel()
    scorePanelWrapper.layout = GridBagLayout()
    val constraints = GridBagConstraints()
    constraints.anchor = GridBagConstraints.NORTH
    scorePanelWrapper.add(scorePanel)

    val leftPanel = JBPanel<JBPanel<*>>()
    leftPanel.layout = BorderLayout(0,3)
    leftPanel.add(link,BorderLayout.NORTH)
    leftPanel.add(content,BorderLayout.CENTER)

    val result = JPanel()
    result.layout = BorderLayout()
    result.add(leftPanel,BorderLayout.CENTER)
    result.add(scorePanelWrapper,BorderLayout.EAST)
    return result
}

private fun prettyTimeOf(date: Date): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}

private fun contentOfFirstAndLast(model: CodeObjectError): String {
    return "${htmlSpanSmoked()}Started: ${htmlSpanWhite()}${prettyTimeOf(model.firstOccurenceTime)}" +
                "  ${htmlSpanSmoked()}Last: ${htmlSpanWhite()}${prettyTimeOf(model.lastOccurenceTime)}"
}


