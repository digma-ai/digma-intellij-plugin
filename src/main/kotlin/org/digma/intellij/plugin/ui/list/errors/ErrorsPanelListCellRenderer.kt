package org.digma.intellij.plugin.ui.list.errors

import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.model.discovery.CodeObjectInfo.Companion.extractMethodName
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.common.*
import org.digma.intellij.plugin.ui.list.AbstractPanelListCellRenderer
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
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
    override fun createPanel(project: Project,
                             value: ListViewItem<*>,
                             index: Int,
                             panelsLayoutHelper: PanelsLayoutHelper): JPanel {
        return getOrCreatePanel(project,value as ListViewItem<CodeObjectError>)
    }

    private fun getOrCreatePanel(project: Project,value: ListViewItem<CodeObjectError>): JPanel {
        val model = value.modelObject
        return listItemPanel(createSingleErrorPanel(project,model))
    }

}

private fun createSingleErrorPanel(project: Project, model: CodeObjectError ): JPanel {

    val relativeFrom = if (model.startsHere) {
        "me"
    } else {
        extractMethodName(model.sourceCodeObjectId)
    }

    val linkText = buildLinkTextWithGrayedAndDefaultLabelColorPart(model.name,"from",relativeFrom)
    val link = ActionLink(asHtml(linkText)){
        val actionListener: ErrorsActionsService = project.getService(ErrorsActionsService::class.java)
        actionListener.showErrorDetails(model)
    }

    val firstAndLast = contentOfFirstAndLast(model)

    link.toolTipText = asHtml("${linkText}<br>${firstAndLast}" )

    val contentText = "${span(model.characteristic)}<br> $firstAndLast"
    val content = CopyableLabelHtml(asHtml(contentText))


    val scorePanel = createScorePanelNoArrows(model)
    val scorePanelWrapper = JPanel()
    scorePanelWrapper.border = JBUI.Borders.empty(0,0,0,5)
    scorePanelWrapper.isOpaque = false
    scorePanelWrapper.layout = GridBagLayout()
    val constraints = GridBagConstraints()
    constraints.anchor = GridBagConstraints.NORTH
    scorePanelWrapper.add(scorePanel)

    val leftPanel = JBPanel<JBPanel<*>>()
    leftPanel.layout = BorderLayout(0,3)
    leftPanel.isOpaque = false
    leftPanel.border = JBUI.Borders.empty(0,0,0,10)
    leftPanel.add(link,BorderLayout.NORTH)
    leftPanel.add(content,BorderLayout.CENTER)

    val result = JPanel()
    result.layout = BorderLayout()
    result.isOpaque = false
    result.add(leftPanel,BorderLayout.CENTER)
    result.add(scorePanelWrapper,BorderLayout.EAST)
    return result
}

private fun prettyTimeOf(date: Date): String {
    val ptNow = PrettyTime()
    return ptNow.format(date)
}

private fun contentOfFirstAndLast(model: CodeObjectError): String {
    return "${spanGrayed("Started:")} ${span(prettyTimeOf(model.firstOccurenceTime))}" +
                "  ${spanGrayed("Last:")} ${span(prettyTimeOf(model.lastOccurenceTime))}"
}


