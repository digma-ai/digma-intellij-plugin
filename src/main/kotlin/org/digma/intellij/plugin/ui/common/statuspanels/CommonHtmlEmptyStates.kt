package org.digma.intellij.plugin.ui.common.statuspanels

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.JBUI.insets
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.ui.common.Links
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants


fun createStartupEmptyStatePanel(project: Project): JPanel {
    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.FileLight
    } else {
        Laf.Icons.Common.FileDark
    }
    return createCommonEmptyStateWithIconTitleAndParagraphAndSlackLink(
        project, icon, "Nothing to show",
        "Navigate to any code file in your workspace,<br>or click a recent activity,<br>to see runtime data and insights here."
    )
}

fun createUpgradeBackendMessagePanel(project: Project): JPanel {
    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.FileLight
    } else {
        Laf.Icons.Common.FileDark
    }
    val textPane = createTextPaneWithHtmlTitleAndParagraph(
        "We've added some new features.",
        "Please update the Digma Engine<br>to the latest version using<br>the action above to continue using Digma"
    )
    val componentsPanel = createCommonEmptyStatePanelWithTextPane(textPane)
//    val componentsPanel = createCommonEmptyStatePanelWIthIconAndTextPane(icon,textPane)
    return createCommonEmptyStatePanelWithSlackLink(project, componentsPanel)
//    return componentsPanel
}

//fun createNoFileEmptyStatePanel(project: Project):JPanel{
//    val icon = if (JBColor.isBright()) {
//        Laf.Icons.Common.FileLight
//    } else {
//        Laf.Icons.Common.FileDark
//    }
//    return createCommonEmptyStateWithIconTitleAndParagraphAndSlackLink(project,icon,"No File Opened",
//        "Navigate to any code file in your workspace<br>to see runtime data and insights here.")
//}


//fun createNonSupportedEmptyStatePanel(project: Project):JPanel{
//    val icon = if (JBColor.isBright()) {
//        Laf.Icons.Common.FileLight
//    } else {
//        Laf.Icons.Common.FileDark
//    }
//    return createCommonEmptyStateWithIconTitleAndParagraphAndSlackLink(project,icon,"File Type is Not Supported",
//        "Navigate to any code file in your workspace<br>to see runtime data and insights here.")
//}

//fun createNoDataYetEmptyStatePanel(project: Project): JPanel {
//    val icon = if (JBColor.isBright()){
//        Laf.Icons.Common.NoDataYetLight
//    }else{
//        Laf.Icons.Common.NoDataYetDark
//    }
//    val messagePanel = createCommonEmptyStateWithIconTitleAndParagraph(
//        icon,
//        "No Data Yet",
//        "Trigger actions that call this code<br>object to learn more about it's<br>runtime behavior."
//    )
//
//
//    val componentsPanel = JPanel(GridBagLayout())
//    componentsPanel.isOpaque = false
//    componentsPanel.border = JBUI.Borders.empty()
//
//    val constraints = GridBagConstraints()
//    constraints.gridx = 0
//    constraints.gridy = 0
//    constraints.ipady = 20
//    componentsPanel.add(messagePanel, constraints)
//
//
//    val link = ActionLink("Not Seeing Your Application Data?") {
//        ActivityMonitor.getInstance(project).registerCustomEvent("troubleshooting link clicked",
//            mapOf(
//                "origin" to "NoDataYetEmptyStatePane"
//            ))
//        MainToolWindowCardsController.getInstance(project).showTroubleshooting()
//    }
//
//    constraints.gridx = 0
//    constraints.gridy = 1
//    constraints.ipady = 0
//    componentsPanel.add(link, constraints)
//    return componentsPanel
//}


fun createNoErrorsEmptyStatePanel(): JPanel {
    val icon = if (JBColor.isBright()) {
        Laf.Icons.Common.NoErrorsLight
    } else {
        Laf.Icons.Common.NoErrorsDark
    }

    return createCommonEmptyStateWithIconTwoTitlesAndParagraph(
        icon, "Good News!",
        "No Errors Where Recorded Here Yet.",
        "You should return to this page if<br>any exceptions do occur to see<br>more details."
    )
}


fun createCommonEmptyStateWithIconTwoTitlesAndParagraph(icon: Icon, firstTitle: String, secondTitle: String, paragraph: String): JPanel {
    val textPane = createTextPaneWithHtmlTwoTitlesAndParagraph(firstTitle, secondTitle, paragraph)
    val componentsPanel = createCommonEmptyStatePanelWIthIconAndTextPane(icon, textPane)
    return wrapWithScrollable(componentsPanel)
}

//fun createCommonEmptyStateWithIconTitleAndParagraph(icon: Icon, title: String, paragraph: String): JPanel {
//    val textPane = createTextPaneWithHtmlTitleAndParagraph(title,paragraph)
//    val componentsPanel = createCommonEmptyStatePanelWIthIconAndTextPane(icon, textPane)
//    return wrapWithScrollable(componentsPanel)
//}

fun createCommonEmptyStateWithIconTitleAndParagraphAndSlackLink(project: Project, icon: Icon, title: String, paragraph: String): JPanel {
    val textPane = createTextPaneWithHtmlTitleAndParagraph(title, paragraph)
    val componentsPanel = createCommonEmptyStatePanelWIthIconAndTextPane(icon, textPane)
    return createCommonEmptyStatePanelWithSlackLink(project, componentsPanel)
}

private fun createCommonEmptyStatePanelWithSlackLink(project: Project, componentsPanel: JPanel): JPanel {

    val mainPanel = JPanel(GridBagLayout())
    mainPanel.isOpaque = false
    mainPanel.border = JBUI.Borders.empty()

    val constraints = GridBagConstraints()
    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 0)
    mainPanel.add(componentsPanel, constraints)

    constraints.gridy = 2
    val slackLinkPanel = createSlackLinkPanel(project)
    mainPanel.add(slackLinkPanel, constraints)

    return wrapWithScrollable(mainPanel)
}

fun createCommonEmptyStatePanelWithTextPane(textPane: JTextPane): JPanel {

//    val componentsPanel = JPanel(GridBagLayout())
//    componentsPanel.isOpaque = false
//    componentsPanel.border = JBUI.Borders.empty()
//
//    val constraints = GridBagConstraints()
//
//    constraints.gridx = 1
//    constraints.gridy = 1
//    constraints.gridwidth = 1
//    constraints.gridheight = 1
//    constraints.anchor = GridBagConstraints.CENTER
//    constraints.insets = insets(10, 0)
//    constraints.fill = GridBagConstraints.HORIZONTAL
//    val messagePanel = JPanel(BorderLayout())
//    messagePanel.isOpaque = false
//    messagePanel.border = JBUI.Borders.empty()
//    messagePanel.add(textPane,BorderLayout.CENTER)
//    componentsPanel.add(messagePanel,constraints)
//    componentsPanel.add(textPane,constraints)
//
//    return componentsPanel

    val messagePanel = JPanel(BorderLayout())
    messagePanel.isOpaque = false
    messagePanel.border = JBUI.Borders.empty()
    messagePanel.add(textPane, BorderLayout.CENTER)
    return messagePanel


}


fun createCommonEmptyStatePanelWIthIconAndTextPane(icon: Icon, textPane: JTextPane): JPanel {

    val componentsPanel = JPanel(GridBagLayout())
    componentsPanel.isOpaque = false
    componentsPanel.border = JBUI.Borders.empty()

    val constraints = GridBagConstraints()

    constraints.gridx = 1
    constraints.gridy = 1
    constraints.gridwidth = 1
    constraints.gridheight = 1
    constraints.anchor = GridBagConstraints.CENTER
    constraints.insets = insets(10, 0)
    val iconLabel = JLabel(icon)
    iconLabel.horizontalAlignment = SwingConstants.CENTER
    componentsPanel.add(iconLabel, constraints)


    constraints.insets = emptyInsets()
    constraints.gridy = 2
    val messageTextPane = textPane
    val messagePanel = JPanel(BorderLayout())
    messagePanel.isOpaque = false
    messagePanel.border = JBUI.Borders.empty()
    messagePanel.add(messageTextPane, BorderLayout.CENTER)
    componentsPanel.add(messagePanel, constraints)

    return componentsPanel
}


fun wrapWithScrollable(componentsPanel: JPanel): JPanel {
    val scrollPane = JBScrollPane()
    scrollPane.border = JBUI.Borders.empty()
    scrollPane.isOpaque = false
    scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scrollPane.setViewportView(componentsPanel)

    val scrollablePanel = JPanel(BorderLayout())
    scrollablePanel.isOpaque = false
    scrollablePanel.border = JBUI.Borders.empty()
    scrollablePanel.add(scrollPane, BorderLayout.CENTER)
    return scrollablePanel
}


fun createSlackLinkPanel(project: Project): JPanel {

    val slackLinkPanel = JPanel(BorderLayout(10, 5))
    slackLinkPanel.isOpaque = false
    slackLinkPanel.border = JBUI.Borders.empty()

    slackLinkPanel.add(JLabel(Laf.Icons.General.SLACK), BorderLayout.WEST)
    val slackLink = ActionLink("Join Our Slack Channel for Support") {
        ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.NoConnection, "slack")
        BrowserUtil.browse(Links.DIGMA_SLACK_SUPPORT_CHANNEL, project)
    }
    slackLink.toolTipText = "Join Our Slack Channel for Support"
    slackLinkPanel.add(slackLink, BorderLayout.CENTER)
    return slackLinkPanel
}


//fun createTextPaneWithHtmlTitleAndParagraphAndSlackLink(title: String,paragraph: String): JTextPane {
//todo: try to implement a text pane where the slack icon and link are part of the html so
// it will align with the text above it
// see examples:
// http://www.java2s.com/Tutorials/Java/Swing/JTextPane/Add_icon_to_JTextPane_in_Java.htm
// https://www.infoworld.com/article/2077472/java-tip-109--display-images-using-jeditorpane.html
//}

//fun createTextPaneWithHtmlParagraph(paragraph: String): JTextPane {
//    return createTextPaneWithHtml(getHtmlWithParagraphCenterAlign(paragraph))
//}

fun createTextPaneWithHtmlTitleAndParagraph(title: String, paragraph: String): JTextPane {
    return createTextPaneWithHtml(getHtmlWithTitleAndParagraphCenterAlign(title, paragraph))
}

fun createTextPaneWithHtmlTwoTitlesAndParagraph(firstTitle: String, secondTitle: String, paragraph: String): JTextPane {
    return createTextPaneWithHtml(getHtmlWithTwoTitlesAndParagraphCenterAlign(firstTitle, secondTitle, paragraph))
}

fun createTextPaneWithHtml(html: String): JTextPane {

    val textPane = JTextPane()
    return textPane.apply {
        contentType = "text/html"
        isEditable = false
        isOpaque = false
        background = null
        border = null
        putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
//        font = BaseCopyableLabel.DEFAULT_FONT
        text = html
        toolTipText = ""
        isFocusCycleRoot = false
        isFocusTraversalPolicyProvider = false
    }
}


fun getHtmlWithTwoTitlesAndParagraphCenterAlign(firstTitle: String, secondTitle: String, paragraph: String): String {
    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "h4 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$firstTitle</h3>" +
            "<h4>$secondTitle</h4>" +
            "<p>$paragraph</p>" +
            "</body>" +
            "</html>"
}

fun getHtmlWithTitleAndParagraphCenterAlign(title: String, paragraph: String): String {
    return "<html>" +
            "<head>" +
            "<style>" +
            "h3 {text-align: center;}" +
            "p {text-align: center;}" +
            "div {text-align: center;}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h3>$title</h3>" +
            "<p>$paragraph</p>" +
            "</body>" +
            "</html>"
}

//fun getHtmlWithParagraphCenterAlign(paragraph: String): String{
//    return "<html>" +
//            "<head>" +
//            "<style>" +
//            "p {text-align: center;}" +
//            "div {text-align: center;}" +
//            "</style>" +
//            "</head>" +
//            "<body>" +
//            "<p>$paragraph</p>" +
//            "</body>" +
//            "</html>"
//}