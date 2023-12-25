package org.digma.intellij.plugin.navigation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator.ViewState
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator.ViewState.*
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.service.InsightsViewService

@Service(Service.Level.PROJECT)
class NavigationModel(private val project: Project) {

    var showCodeNavigation = AtomicBooleanProperty(false)


    fun viewStateChanged(currentState: ViewState) {

        val insightsModel = project.service<InsightsViewService>().model

        when (currentState) {
            CodelessSpan -> {
                showCodeNavigation.set(true)
            }

            MethodFromBackNavigation, MethodWithoutNavigation -> {
                showCodeNavigation.set(true)
            }

            MethodFromSourceCode,
            EndpointFromSourceCode,
            SpanOrMethodWithNavigation,
            DummyMethod,
            -> {
                showCodeNavigation.set(false)
            }

            NonSupportedFile,
            NoFile,
            -> {
                if (insightsModel.scope is MethodScope || insightsModel.scope is CodeLessSpanScope || insightsModel.scope is EndpointScope) {
                    showCodeNavigation.set(true)
                } else if (insightsModel.scope is DocumentScope && insightsModel.insightsCount == 0) {
                    showCodeNavigation.set(true)
                } else {
                    showCodeNavigation.set(false)
                }
            }

            DocumentPreviewList -> {
                showCodeNavigation.set(false)
            }
        }
    }

}