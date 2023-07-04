package org.digma.intellij.plugin.insights

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.navigation.NavigationModel
import org.digma.intellij.plugin.navigation.codenavigation.CodeNavigator
import org.digma.intellij.plugin.service.EditorService
import org.digma.intellij.plugin.service.ErrorsActionsService
import org.digma.intellij.plugin.ui.ToolWindowShower
import org.digma.intellij.plugin.ui.service.ErrorsViewService
import org.digma.intellij.plugin.ui.service.InsightsViewService

/**
 * the job of this class is to show insights for code objects and navigate to source code if necessary and possible.
 */
@Service(Service.Level.PROJECT)
class InsightsViewOrchestrator(val project: Project) {

    //todo: this class is WIP.
    // we need to separate the flows of showing insights and navigating to code.
    // currently we rely heavily on caret event to show insights when the caret changes positions in source code.
    // this class should be the central point of showing insights and navigating to code. it should be the listener
    // for caret events and decide if to show insights or not. and it should be the central point to show insights
    // for code objects and navigate to code if required and possible.
    //maybe this class also has state of what is currently showing and where did the request come from so it can make decisions
    // what to do in following caret events.
    //if this class is the central point for showing insights it can make decisions based on state.


    val logger = Logger.getInstance(this::class.java)

    private var currentState: AtomicProperty<ViewState> = AtomicProperty(ViewState.NoFile)

    enum class ViewState {
        CodelessSpan,
        SpanOrMethodWithNavigation,
        NonSupportedFile,
        NoFile,
        MethodFromSourceCode,
        DummyMethod,
        DocumentPreviewList
    }


    init {
        @Suppress("UnstableApiUsage")
        currentState.afterChange {
            project.service<NavigationModel>().viewStateChanged(currentState.get())
        }
    }

    /**
     * shows insights for a span.
     */
    fun showInsightsForCodelessSpan(spanId: String) {

        currentState.set(ViewState.CodelessSpan)

        Log.log(logger::debug, project, "Got showInsightsForSpan {}", spanId)

        project.service<InsightsViewService>().updateInsightsModel(
            CodeLessSpan(spanId)
        )

        project.service<ErrorsViewService>().updateErrorsModel(
            CodeLessSpan(spanId)
        )

        project.service<ErrorsActionsService>().closeErrorDetailsBackButton()

        //clear the latest method so that if user clicks on the editor again after watching code less insights the context will change
        project.service<CurrentContextUpdater>().clearLatestMethod()

        ToolWindowShower.getInstance(project).showToolWindow()
    }

    /**
     * shows insights for span or method by which ever is non-null, and be navigated to code.
     * This method should be called only if it is possible to navigate to code. can be checked with
     * codeNavigator.canNavigateToSpan(spanId) || codeNavigator.canNavigateToMethod(methodId)
     */
    //todo: this method needs clarification. what do we want to do?
    // first option is show insights for the span and navigate to either span location or method location, which ever is possible.
    // second option is just navigate to code location and rely on caret event to show insights. usually navigating to span will show
    // insights for the enclosing method, but maybe we want to show only the span insights and navigate to code. as said above caret listener
    // events should be processed by this class.
    // currently we can not do both because one will override the other. we need to separate the flows of showing insights and navigating
    // to code.
    fun showInsightsForSpanOrMethodAndNavigateToCode(spanCodeObjectId: String?, methodCodeObjectId: String?): Boolean {

        currentState.set(ViewState.SpanOrMethodWithNavigation)

        //todo: this is WIP, currently relying on showing insights by navigating to code locations and relying on caret event.
        // this class should show insights regardless of caret event and navigate to code if possible.
        // we need to separate this two actions , showing insights and navigating to source code.

        ToolWindowShower.getInstance(project).showToolWindow()

        //we have a situation where the target button wants to navigate to a method or span and show its insights,
        // but the caret in the editor is already on the same offset, in that case a caret event will not be fired.
        // so if that's the case we emulate a caret event
        //as noted above this is WIP and we should change this class over time to show insights of code object

        val currentCaretLocation: Pair<String, Int>? = project.service<EditorService>().currentCaretLocation
        val methodLocation: Pair<String, Int>? = methodCodeObjectId?.let { project.service<CodeNavigator>().getMethodLocation(methodCodeObjectId) }
        val spanLocation: Pair<String, Int>? = spanCodeObjectId?.let { project.service<CodeNavigator>().getSpanLocation(spanCodeObjectId) }

        //if currentCaretLocation is null maybe there is no file opened
        if (currentCaretLocation == null) {
            return project.service<CodeNavigator>().maybeNavigateToSpanOrMethod(spanCodeObjectId, methodCodeObjectId)
        }

        //we can navigate to span, it will cause a caret event and show the insights
        if (spanLocation != null && spanLocation != currentCaretLocation) {
            return project.service<CodeNavigator>().maybeNavigateToSpan(spanCodeObjectId)
        }

        //navigate to method, it will cause a caret event and show the insights
        if (methodLocation != null && methodLocation != currentCaretLocation) {
            return project.service<CodeNavigator>().maybeNavigateToMethod(methodCodeObjectId)
        }

        //methodLocation equals currentCaretLocation, so we have to emulate a caret event to show the method insights
        if (methodLocation != null) {
            return emulateCaretEvent(methodCodeObjectId, methodLocation.first)
        }

        //todo: not sure about that
        if (spanLocation != null) {
            showInsightsForCodelessSpan(spanCodeObjectId)
            return true
        }

        return project.service<CodeNavigator>().maybeNavigateToSpanOrMethod(spanCodeObjectId, methodCodeObjectId)

    }

    private fun emulateCaretEvent(methodId: String, fileUri: String): Boolean {

        val methodNameAndClass: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodId)

        val methodUnderCaret = MethodUnderCaret(
            CodeObjectsUtil.stripMethodPrefix(methodId),
            methodNameAndClass.first,
            methodNameAndClass.second,
            "",
            fileUri
        )

        val methodInfo: MethodInfo? = project.service<DocumentInfoService>().getMethodInfo(methodUnderCaret)
        if (methodInfo == null) {
            Log.log({ message: String? -> logger.warn(message) }, "Could not find MethodInfo for MethodUnderCaret {}. ", methodUnderCaret)
            val dummyMethodInfo = MethodInfo(
                methodUnderCaret.id, methodUnderCaret.name, methodUnderCaret.className, "",
                methodUnderCaret.fileUri, 0
            )
            Log.log({ message: String? -> logger.warn(message) }, "Using dummy MethodInfo for to update views {}. ", dummyMethodInfo)
            updateInsightsWithDummyMethodInfo(methodUnderCaret, dummyMethodInfo)
        } else {
            Log.log({ message: String? -> logger.debug(message) }, "Context changed to {}. ", methodInfo)
            updateInsightsWithMethodFromSource(methodUnderCaret, methodInfo)
        }


        return true
    }

    fun nonSupportedFileOpened(fileUri: String) {

        currentState.set(ViewState.NonSupportedFile)

        project.service<InsightsViewService>().emptyNonSupportedFile(fileUri)
        project.service<ErrorsViewService>().emptyNonSupportedFile(fileUri)
    }

    fun noFileOpened() {

        currentState.set(ViewState.NoFile)

        project.service<InsightsViewService>().empty()
        project.service<ErrorsViewService>().empty()
    }

    fun updateInsightsWithMethodFromSource(methodUnderCaret: MethodUnderCaret, methodInfo: MethodInfo) {

        currentState.set(ViewState.MethodFromSourceCode)

        val documentInfo: DocumentInfoContainer? = project.service<DocumentInfoService>().getDocumentInfo(methodUnderCaret)
        documentInfo?.let {
            val methodHasNewInsights = documentInfo.loadInsightsForMethod(methodUnderCaret.id) // might be long call since going to the backend
            project.service<InsightsViewService>().updateInsightsModel(methodInfo)
            project.service<ErrorsViewService>().updateErrorsModel(methodInfo)
        }
    }

    fun updateInsightsWithDummyMethodInfo(methodUnderCaret: MethodUnderCaret, dummyMethodInfo: MethodInfo) {

        currentState.set(ViewState.DummyMethod)

        project.service<InsightsViewService>().contextChangeNoMethodInfo(dummyMethodInfo)
        project.service<ErrorsViewService>().contextChangeNoMethodInfo(dummyMethodInfo)
    }

    fun updateWithDocumentPreviewList(documentInfoContainer: DocumentInfoContainer?, fileUri: String) {

        currentState.set(ViewState.DocumentPreviewList)

        project.service<InsightsViewService>().showDocumentPreviewList(documentInfoContainer, fileUri)
        project.service<ErrorsViewService>().showDocumentPreviewList(documentInfoContainer, fileUri)
    }

}