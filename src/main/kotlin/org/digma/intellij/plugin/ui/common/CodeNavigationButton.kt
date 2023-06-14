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
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation
import org.digma.intellij.plugin.model.rest.navigation.NavItemType
import org.digma.intellij.plugin.model.rest.navigation.SpanNavigationItem
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.ui.list.RoundedPanel
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import java.awt.BorderLayout
import java.awt.Cursor
import javax.swing.JLabel

class CodeNavigationButton(val project: Project, private val panelModel: PanelModel, enabled: Boolean = true) : TargetButton(project, enabled) {

    private val logger: Logger = Logger.getInstance(CodeNavigationButton::class.java)

    init {

        isEnabled = getCodeLessSpan() != null
        if (!isEnabled) {
            toolTipText = asHtml("Already at code location")
            border = JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1)
            background = Laf.Colors.TRANSPARENT
        }

        if (isEnabled) {

            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addActionListener {

                try {
                    val codeLessSpan = getCodeLessSpan()
                    if (codeLessSpan != null) {

                        val objectIdToUse = CodeObjectsUtil.addSpanTypeToId(codeLessSpan.spanId)
                        val codeObjectNavigation =
                            project.service<AnalyticsService>().getCodeObjectNavigation(objectIdToUse)

                        val spanId = codeObjectNavigation.navigationEntry.spanInfo?.spanCodeObjectId
                        val methodId = codeObjectNavigation.navigationEntry.spanInfo?.methodCodeObjectId

                        //try direct navigation
                        navigate(spanId, methodId, codeObjectNavigation)
                    }
                } catch (e: Exception) {
                    HintManager.getInstance().showHint(
                        JLabel("Code Not Found!"), RelativePoint.getSouthWestOf(this),
                        HintManager.HIDE_BY_ESCAPE, 5000
                    )
                    Log.debugWithException(logger, project, e, "Error in getCodeObjectNavigation")
                }
            }
        }

    }

    private fun navigate(
        spanId: String?,
        methodId: String?,
        codeObjectNavigation: CodeObjectNavigation,
    ) {
        if (project.service<CodeNavigator>().maybeNavigateToSpan(spanId, methodId)) {
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
                    JLabel("Code Not Found!"), RelativePoint.getSouthWestOf(this),
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
                                project.service<CodeNavigator>().maybeNavigateToSpan(navItem.spanCodeObjectId, navItem.methodCodeObjectId)
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
                                project.service<CodeNavigator>().maybeNavigateToSpan(navItem.spanCodeObjectId, navItem.methodCodeObjectId)
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