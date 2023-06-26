package org.digma.intellij.plugin.ui.common

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation
import org.digma.intellij.plugin.model.rest.navigation.NavItemType
import org.digma.intellij.plugin.model.rest.navigation.SpanNavigationItem
import org.digma.intellij.plugin.navigation.NavigationModel
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.ui.list.RoundedPanel
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import javax.swing.JLabel

private const val CODE_NOT_FOUND = "Code not found"

class CodeNavigationButton(val project: Project, private val panelModel: PanelModel, enabled: Boolean = true) : TargetButton(project, enabled) {

    private val logger: Logger = Logger.getInstance(CodeNavigationButton::class.java)
    private val myOriginalBackground: Color = background

    init {

        val showCodeNavigation = project.service<NavigationModel>().showCodeNavigation
        isEnabled = showCodeNavigation.get()

        updateState()

        @Suppress("UnstableApiUsage")
        showCodeNavigation.afterChange {
            isEnabled = it
            updateState()
        }


        addActionListener {

            try {
                val codeLessSpan = getCodeLessSpan()
                if (codeLessSpan != null) {

                    val objectIdToUse = CodeObjectsUtil.addSpanTypeToId(codeLessSpan.spanId)
                    val codeObjectNavigation =
                        project.service<AnalyticsService>().getCodeObjectNavigation(objectIdToUse)

                    navigate(codeObjectNavigation)
                    return@addActionListener
                }

                val methodInfo = getMethodInfo()
                if (methodInfo != null) {

                    val methodId = methodInfo.id
                    val codeNavigator = project.service<CodeNavigator>()
                    if (codeNavigator.canNavigateToMethod(methodId)) {
                        codeNavigator.maybeNavigateToMethod(methodInfo.id)
                    } else {
                        HintManager.getInstance().showHint(
                            JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                            HintManager.HIDE_BY_ESCAPE, 5000
                        )
                    }

                    return@addActionListener
                }

                val documentInfo = getDocumentInfo()
                if (documentInfo != null) {

                    val fileUri = documentInfo.fileUri
                    val codeNavigator = project.service<CodeNavigator>()
                    if (codeNavigator.canNavigateToFile(fileUri)) {
                        codeNavigator.maybeNavigateToFile(fileUri)
                    } else {
                        HintManager.getInstance().showHint(
                            JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                            HintManager.HIDE_BY_ESCAPE, 5000
                        )
                    }

                    return@addActionListener
                }


            } catch (e: Exception) {
                HintManager.getInstance().showHint(
                    JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                    HintManager.HIDE_BY_ESCAPE, 5000
                )
                Log.debugWithException(logger, project, e, "Error in getCodeObjectNavigation")
            }
        }

    }


    private fun updateState() {
        if (isEnabled) {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            background = myOriginalBackground
            border = JBUI.Borders.empty()
            toolTipText = "Navigate to code"
        } else {
            cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            toolTipText = asHtml("Already at code location")
            border = JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1)
            background = Laf.Colors.TRANSPARENT
        }
    }


    private fun navigate(
        codeObjectNavigation: CodeObjectNavigation,
    ) {

        val spanId = codeObjectNavigation.navigationEntry.spanInfo?.spanCodeObjectId
        val methodId = codeObjectNavigation.navigationEntry.spanInfo?.methodCodeObjectId

        val codeNavigator = project.service<CodeNavigator>()

        //first try direct navigation, if can't then build navigation list and show user
        if (codeNavigator.canNavigateToSpan(spanId) || codeNavigator.canNavigateToMethod(methodId)) {
            project.service<InsightsViewOrchestrator>().showInsightsForSpanOrMethodAndNavigateToCode(spanId, methodId)
            Log.log(logger::debug, project, "Navigation to direct span succeeded for {},{}", spanId, methodId)
        } else {

            val closestParentItems = codeObjectNavigation.navigationEntry.closestParentSpans
                .filter { spanNavigationItem -> spanNavigationItem.navItemType == NavItemType.ClosestParentInternal }
                .sortedBy { spanNavigationItem -> spanNavigationItem.distance }
                .filter { spanNavigationItem ->
                    project.service<CodeNavigator>()
                        .canNavigateToSpanOrMethod(spanNavigationItem.spanCodeObjectId, spanNavigationItem.methodCodeObjectId)
                }

            val closestParentWithMethodItems = codeObjectNavigation.navigationEntry.closestParentSpans
                .filter { spanNavigationItem -> spanNavigationItem.navItemType == NavItemType.ClosestParentWithMethodCodeObjectId }
                .sortedBy { spanNavigationItem -> spanNavigationItem.distance }
                .filter { spanNavigationItem ->
                    project.service<CodeNavigator>()
                        .canNavigateToSpanOrMethod(spanNavigationItem.spanCodeObjectId, spanNavigationItem.methodCodeObjectId)
                }


            if (closestParentItems.isEmpty() && closestParentWithMethodItems.isEmpty()) {
                HintManager.getInstance().showHint(
                    JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                    HintManager.HIDE_BY_ESCAPE, 5000
                )
            } else {
                HintManager.getInstance().showHint(
                    NavigationList(project, closestParentItems, closestParentWithMethodItems), RelativePoint.getSouthWestOf(this),
                    HintManager.HIDE_BY_ESCAPE, 5000
                )
            }

        }
    }


    private fun getCodeLessSpan(): CodeLessSpan? {
        if (panelModel is InsightsModel && panelModel.scope is CodeLessSpanScope) {
            return (panelModel.scope as CodeLessSpanScope).getSpan()
        } else if (panelModel is ErrorsModel && panelModel.scope is CodeLessSpanScope) {
            return (panelModel.scope as CodeLessSpanScope).getSpan()
        }
        return null
    }

    private fun getMethodInfo(): MethodInfo? {
        if (panelModel is InsightsModel && panelModel.scope is MethodScope) {
            return (panelModel.scope as MethodScope).getMethodInfo()
        } else if (panelModel is ErrorsModel && panelModel.scope is MethodScope) {
            return (panelModel.scope as MethodScope).getMethodInfo()
        }
        return null
    }

    private fun getDocumentInfo(): DocumentInfo? {
        if (panelModel is InsightsModel && panelModel.scope is DocumentScope) {
            return (panelModel.scope as DocumentScope).getDocumentInfo()
        } else if (panelModel is ErrorsModel && panelModel.scope is DocumentScope) {
            return (panelModel.scope as DocumentScope).getDocumentInfo()
        }
        return null
    }

    private class NavigationList(
        project: Project,
        closestParentItems: List<SpanNavigationItem>,
        closestParentWithMethodItems: List<SpanNavigationItem>,
    ) : RoundedPanel(30) {

        init {

            val panel = panel {

                if (closestParentItems.isNotEmpty()) {
                    row {
                        @Suppress("DialogTitleCapitalization")
                        label("Code Location")
                    }

                    closestParentItems.forEach { navItem ->
                        row {
                            icon(Laf.Icons.General.CODE_LOCATION_LINK).gap(RightGap.SMALL)
                            link(navItem.displayName) {
                                project.service<InsightsViewOrchestrator>()
                                    .showInsightsForSpanOrMethodAndNavigateToCode(navItem.spanCodeObjectId, navItem.methodCodeObjectId)
                                HintManager.getInstance().hideAllHints()
                            }
                        }

                    }
                }

                if (closestParentWithMethodItems.isNotEmpty()) {
                    row {
                        @Suppress("DialogTitleCapitalization")
                        label("Related Code Location")
                    }

                    closestParentWithMethodItems.forEach { navItem ->
                        row {
                            icon(Laf.Icons.General.CODE_LOCATION_LINK).gap(RightGap.SMALL)
                            link(navItem.displayName) {
                                project.service<InsightsViewOrchestrator>()
                                    .showInsightsForSpanOrMethodAndNavigateToCode(navItem.spanCodeObjectId, navItem.methodCodeObjectId)
                                HintManager.getInstance().hideAllHints()
                            }
                        }
                    }
                }
            }.andTransparent().withBorder(JBUI.Borders.empty(5))

            isOpaque = false
            background = Laf.Colors.LIST_ITEM_BACKGROUND
            layout = BorderLayout()
            border = JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1)
            add(panel, BorderLayout.CENTER)
        }
    }

}