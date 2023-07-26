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
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.navigation.NavItemType
import org.digma.intellij.plugin.model.rest.navigation.SpanNavigationItem
import org.digma.intellij.plugin.navigation.NavigationModel
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.list.RoundedPanel
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.service.InsightsViewService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import javax.swing.JLabel

private const val CODE_NOT_FOUND = "Code not found"

class CodeNavigationButton(val project: Project) : TargetButton(project, true) {

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
            Backgroundable.ensureBackground(project, "Navigating to code object") {
                doActionListener()
            }
        }

    }


    private fun doActionListener() {

        try {
            val codeLessSpan = getCodeLessSpan()
            if (codeLessSpan != null) {
                tryNavigate(codeLessSpan.spanId)
                return
            }

            val methodInfo = getMethodInfo()
            if (methodInfo != null) {

                val methodId = methodInfo.id
                val codeNavigator = project.service<CodeNavigator>()
                if (codeNavigator.canNavigateToMethod(methodId)) {
                    ActivityMonitor.getInstance(project).registerNavigationButtonClicked(true)
                    codeNavigator.maybeNavigateToMethod(methodInfo.id)
                } else {
                    ActivityMonitor.getInstance(project).registerNavigationButtonClicked(false)
                    EDT.ensureEDT {
                        HintManager.getInstance().showHint(
                            JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                            HintManager.HIDE_BY_ESCAPE, 5000
                        )
                    }
                }

                return
            }

            val documentInfo = getDocumentInfo()
            if (documentInfo != null) {

                val fileUri = documentInfo.fileUri
                val codeNavigator = project.service<CodeNavigator>()
                if (codeNavigator.canNavigateToFile(fileUri)) {
                    ActivityMonitor.getInstance(project).registerNavigationButtonClicked(true)
                    codeNavigator.maybeNavigateToFile(fileUri)
                } else {
                    ActivityMonitor.getInstance(project).registerNavigationButtonClicked(false)
                    EDT.ensureEDT {
                        HintManager.getInstance().showHint(
                            JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                            HintManager.HIDE_BY_ESCAPE, 5000
                        )
                    }
                }

                return
            }


        } catch (e: Exception) {
            EDT.ensureEDT {
                HintManager.getInstance().showHint(
                    JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                    HintManager.HIDE_BY_ESCAPE, 5000
                )
            }
            Log.debugWithException(logger, project, e, "Error in getCodeObjectNavigation")
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


    private fun tryNavigate(spanId: String) {
        val codeNavigator = project.service<CodeNavigator>()

        //first try direct navigation, if can't then build navigation list and show user
        if (codeNavigator.maybeNavigateToSpan(spanId)) {
            Log.log(logger::debug, project, "Navigation to direct span succeeded for span {}", spanId)
            ActivityMonitor.getInstance(project).registerNavigationButtonClicked(true)
        } else {
            tryNavigateToCorrelatedMethods(spanId)
        }
    }

    private fun tryNavigateToCorrelatedMethods(spanId: String) {
        val spanCodeObjectId = CodeObjectsUtil.addSpanTypeToId(spanId)

        val codeNavigator = project.service<CodeNavigator>()
        val codeObjectNavigation = project.service<AnalyticsService>().getCodeObjectNavigation(spanCodeObjectId)
        val methodIds = codeNavigator.buildPotentialMethodIds(codeObjectNavigation)

        var managedToNav = false
        if (methodIds.size == 1) {
            val methodId = methodIds.first()

            if (codeNavigator.canNavigateToMethod(methodId)) {
                managedToNav = true
                EDT.ensureEDT {
                    project.service<InsightsViewOrchestrator>().showInsightsForSpanOrMethodAndNavigateToCode(spanCodeObjectId, methodId)
                }
                Log.log(logger::debug, project, "Navigation to method '{}' succeeded for span {}", methodId, spanCodeObjectId)
            }
            ActivityMonitor.getInstance(project).registerNavigationButtonClicked(true)
        }

        if (!managedToNav) {
            val closestParentItems = codeObjectNavigation.navigationEntry.closestParentSpans
                .filter { spanNavigationItem -> spanNavigationItem.navItemType == NavItemType.ClosestParentInternal }
                .sortedBy { spanNavigationItem -> spanNavigationItem.distance }
                .filter { spanNavigationItem ->
                    codeNavigator
                        .canNavigateToSpanOrMethod(spanNavigationItem.spanCodeObjectId, spanNavigationItem.methodCodeObjectId)
                }

            val closestParentWithMethodItems = codeObjectNavigation.navigationEntry.closestParentSpans
                .filter { spanNavigationItem -> spanNavigationItem.navItemType == NavItemType.ClosestParentWithMethodCodeObjectId }
                .sortedBy { spanNavigationItem -> spanNavigationItem.distance }
                .filter { spanNavigationItem ->
                    codeNavigator
                        .canNavigateToSpanOrMethod(spanNavigationItem.spanCodeObjectId, spanNavigationItem.methodCodeObjectId)
                }

            val hasAnyCodeLocation =
                closestParentItems.isNotEmpty() || closestParentWithMethodItems.isNotEmpty() || methodIds.isNotEmpty()

            if (!hasAnyCodeLocation) {
                ActivityMonitor.getInstance(project).registerNavigationButtonClicked(false)
                EDT.ensureEDT {
                    HintManager.getInstance().showHint(
                        JLabel(CODE_NOT_FOUND), RelativePoint.getSouthWestOf(this),
                        HintManager.HIDE_BY_ESCAPE, 5000
                    )
                }
            } else {
                ActivityMonitor.getInstance(project).registerNavigationButtonClicked(true)
                EDT.ensureEDT {
                    HintManager.getInstance().showHint(
                        NavigationList(project, methodIds, closestParentItems, closestParentWithMethodItems),
                        RelativePoint.getSouthWestOf(this),
                        HintManager.HIDE_BY_ESCAPE, 5000
                    )
                }
            }
        }
    }


    private fun getCodeLessSpan(): CodeLessSpan? {
        val panelModel = project.service<InsightsViewService>().model
        return if (panelModel.scope is CodeLessSpanScope) {
            (panelModel.scope as CodeLessSpanScope).getSpan()
        } else {
            null
        }
    }

    private fun getMethodInfo(): MethodInfo? {
        val panelModel = project.service<InsightsViewService>().model
        return if (panelModel.scope is MethodScope) {
            (panelModel.scope as MethodScope).getMethodInfo()
        } else {
            null
        }
    }

    private fun getDocumentInfo(): DocumentInfo? {
        val panelModel = project.service<InsightsViewService>().model
        return if (panelModel.scope is DocumentScope) {
            (panelModel.scope as DocumentScope).getDocumentInfo()
        } else {
            null
        }
    }

    private class NavigationList(
        project: Project,
        methodIds: List<String>,
        closestParentItems: List<SpanNavigationItem>,
        closestParentWithMethodItems: List<SpanNavigationItem>,
    ) : RoundedPanel(30) {

        init {

            val panel = panel {

                if (methodIds.isNotEmpty()) {
                    row {
                        @Suppress("DialogTitleCapitalization")
                        label("Code Location")
                    }
                    methodIds.forEach { methodId ->
                        val displayName = CodeObjectsUtil.getShortNameForDisplay(methodId)
                        row {
                            icon(Laf.Icons.General.CODE_LOCATION_LINK).gap(RightGap.SMALL)
                            link(displayName) {
                                project.service<InsightsViewOrchestrator>()
                                    .showInsightsForSpanOrMethodAndNavigateToCode(null, methodId)
                                HintManager.getInstance().hideAllHints()
                            }
                        }
                    }
                }

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