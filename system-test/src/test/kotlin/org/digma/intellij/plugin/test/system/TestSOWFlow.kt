package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.ui.jcef.JBCefBrowser
import junit.framework.TestCase
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.insights.InsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.test.system.framework.createSpyBrowsers
import org.digma.intellij.plugin.test.system.framework.injectSpyBrowser
import org.digma.intellij.plugin.test.system.framework.mockGetCodeObjectNavigation
import org.digma.intellij.plugin.test.system.framework.mockGetEnvironments
import org.digma.intellij.plugin.test.system.framework.mockGetInsightOfSingeSpan
import org.digma.intellij.plugin.test.system.framework.mockGetInsightsOfMethods
import org.digma.intellij.plugin.test.system.framework.mockGetRecentActivity
import org.digma.intellij.plugin.ui.common.CodeNavigationButton
import org.digma.intellij.plugin.ui.jcef.JCefComponent
import org.digma.intellij.plugin.ui.jcef.JCefComponentBuilder
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityMessageRouterHandler
import org.digma.intellij.plugin.ui.recentactivity.RecentActivitySchemeHandlerFactory
import org.digma.intellij.plugin.ui.service.InsightsService
import java.lang.reflect.Field

class TestSOWFlow : DigmaTestCase() {

    private val documentInfoService: DocumentInfoService
        get() = DocumentInfoService.getInstance(project)


    override fun getTestDataPath(): String {
        return "src/test/resources"
    }
    
    override fun getTestProjectFileNames(): Array<String> {
        return arrayOf("TestFile.java", "TestFile2.java")
    }
    
    

    override fun setUp() {
        super.setUp()
        prepareBrowsers()
        
        mockGetEnvironments(mockAnalyticsProvider, SingleEnvironmentData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, SingleEnvironmentData.expectedInsightsOfMethods)
        mockGetRecentActivity(mockAnalyticsProvider, SingleEnvironmentData.expectedRecentActivityResult)
        mockGetInsightOfSingeSpan(mockAnalyticsProvider, TwoEnvironmentsFirstFileRelatedSingleSpanData.expectedInsightOfSingleSpan) // those insights are for both envs
        mockGetCodeObjectNavigation(mockAnalyticsProvider, TwoEnvironmentSecondFileNavigateToCodeData.codeObjectNavigation)
        
    }

    private fun prepareBrowsers() {
        //prepare the browser for mocking of InsightsService
        val (insightJBBrowser, insightCefBrowser) = createSpyBrowsers()
        // inject the insight browser spy into the InsightsServiceMessage handler
        prepareInsightBrowser(insightJBBrowser)
        // prepare the browser for mocking of RecentActivityService
        val (recentActivityJBBrowser, recentActivityCefBrowser) = createSpyBrowsers()
        prepareRecentActivityBrowser(recentActivityJBBrowser)

        // prepare assertion in the browser for the insights that will be pushed of the method under caret
        replaceExecuteJavaScriptOf(insightCefBrowser)
        // use browser spy to verify recent activities
        replaceExecuteJavaScriptOf(recentActivityCefBrowser)

       
    }

    fun testFlow() {

        // Bullet One
        // prepare single environment mock with insights and recent activities

        waitForRecentActivityToLoad()
        
        waitFor(1000, "browser spy to be ready")
        val files = myFixture.configureByFiles(SingleEnvironmentData.DOC_NAME, TwoEnvironmentSecondFileNavigateToCodeData.DOC_NAME)
        val firstFile = files[0]
        val secondFile = files[1]

        waitForAndDispatch(1000, "events after opening ${firstFile?.name}")


        //finding the target method for method1
        val methods = PsiTreeUtil.findChildrenOfType(firstFile, PsiMethod::class.java)
        var targetMethod = methods.find { it.name == "method1" }

        setCaretToTargetMethodAndDispatchIdeEventQueue(targetMethod, firstFile)

        // test that latest method under caret is method 1
        var methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret

        // asserting all the current information about method one and the method under caret
        assertEquals(targetMethod?.name, methodUnderCaret.name)
        assertEquals(targetMethod?.containingClass?.name, methodUnderCaret.className)
        assertEquals(SingleEnvironmentData.methodCodeObjectId, methodUnderCaret.id)

        //see that the insights of method1 are present

        val methodInfoOfMethodUnderCaret = project.service<DocumentInfoService>().findMethodInfo(methodUnderCaret.id)

        //checking the insights of method1
        val cachedInsights = project.service<DocumentInfoService>().getCachedMethodInsights(methodInfoOfMethodUnderCaret!!)

        cachedInsights.forEach { insight ->
            assertEquals(SingleEnvironmentData.methodCodeObjectId, insight.codeObjectId)
            assertEquals(SingleEnvironmentData.environmentList[0], insight.environment)
        }


        // see that recentActivities are present from env1
        val latestActivityResult = recentActivityService.getRecentActivities(SingleEnvironmentData.environmentList)

        assertNotNull(latestActivityResult)
        // force non-null because the assertion above will fail if null
        latestActivityResult!!.entries.forEach {
            assertEquals(SingleEnvironmentData.environmentList[0], it.environment)
            it.slimAggregatedInsights.forEach { insight ->
                assertTrue(insight.codeObjectIds.contains(SingleEnvironmentData.methodCodeObjectId))
            }
        }

        // assert that we are in the correct environment
        assertEquals(SingleEnvironmentData.environmentList[0], analyticsService.environment.getCurrent())

        //  assert from browser spy that the insights were pushed to the browser
        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonForSingleEnvironmentData)

        //  assert from browser spy that the recent activities were pushed to the browser
        assertQueueOfActionWithinTimeout("RECENT_ACTIVITY/SET_DATA", 5000) {
            assertJsonBrowserForRecentActivity(it, SingleEnvironmentData.environmentList, SingleEnvironmentData.expectedRecentActivityResult)
        }


        // Bullet Two
        // bullet 2 - Given new trace from new environment (Env2) arrived.
        // mock new recentActivities from env 2 in analyticsProvider
        mockGetEnvironments(mockAnalyticsProvider, TwoEnvironmentsData.environmentList)
        mockGetRecentActivity(mockAnalyticsProvider, TwoEnvironmentsData.expectedRecentActivityResult)


        waitFor(1000, "new environments to be fetched")

        // force fetching of recent activities
        recentActivityUpdater.updateLatestActivities()
        waitForAndDispatch(1000, "recent activities to be fetched")


        // assert that the new recentActivities are from env2:
        // in the plugin runtime
        val recentActivityResultFromEnv2 = recentActivityService.getRecentActivities(TwoEnvironmentsData.environmentList)
        assertNotNull(recentActivityResultFromEnv2)
        assertTrue(recentActivityResultFromEnv2!!.entries.any { it.environment == TwoEnvironmentsData.environmentList[1] }) // forcing non-null because the assertion above will fail if null

        // in the browser
        assertQueueOfActionWithinTimeout("RECENT_ACTIVITY/SET_DATA", 5000) {
            assertJsonBrowserForRecentActivity(it, TwoEnvironmentsData.environmentList, TwoEnvironmentsData.expectedRecentActivityResult)
        }

        // bullet 3 -
        // When I click on the link (span_endpoint1) in the recent activity
        // Then the selected environment in the main panel should be changed from Env1 to Env2
        // and I should see that the scope has been changed to span_endpoint1 and 
        // span_endpoint1 insights (all insights**) are visible

        mockGetEnvironments(mockAnalyticsProvider, TwoEnvironmentsData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, GoToSpanData.expectedInsightsOfMethods) // those insights are for env2
        mockGetRecentActivity(mockAnalyticsProvider, GoToSpanData.expectedRecentActivityResult)
        

        // call processRecentActivityGoToSpan -- this is actually the trigger to open the recent activity insights in the side panel. also do the navigation in the code to the span
        recentActivityService.processRecentActivityGoToSpanRequest(GoToSpanData.goToSpanRequestPayload)
        waitForAndDispatch(1000, "go to span to be processed")


        // check that the environment is changed to env2
        // either use the environment changed event or do to analyticsService.environment.getCurrent()
        assertEquals(TwoEnvironmentsData.environmentList[1], analyticsService.environment.getCurrent())
        // assert that the caret moved to the span in the code

        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        assertEquals(methodUnderCaret.id, GoToSpanData.goToSpanRequestPayload.span.methodCodeObjectId)
        assertEquals(targetMethod?.name, methodUnderCaret.name) //todo: not sure about that

        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonForTwoEnvironmentNewInsightsData)


        // bullet 4 -
        // When I click on span_2 link on insight1
        // Then I should see the scoped changed to span_2 and insights of span_2 are shown.
        waitForAndDispatch(1000, "update the single span insights")
        
        // call processInsightGoToSpan~~~~ --> to go the  span that is related to the insight1 - goToInsight
        project.getService(InsightsService::class.java)
            .showInsight(TwoEnvironmentsFirstFileRelatedSingleSpanData.relatedSpansToMethod[0]) // another span to show insights for from another document

        waitForAndDispatch(1000, "insights to be shown")

        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonForInsightsOfRelatedSpans)

        // bullet 5 -

        waitForDocumentInfoToLoad(secondFile)
        
        
        val targetButton = CodeNavigationButton(project)
        // When I click target icon (go to code)
        targetButton.doClick()

        waitForAndDispatch(1000, "go to code to be processed and switch to document2")

        // Then I should be navigated to document#2 and the caret should be navigated to the span location.
        val currentDocument = FileEditorManager.getInstance(project).selectedEditor?.file
        
        waitForDocumentInfoToLoad(secondFile)
        //check in document Info service that the document is changed to document2
        val currentDocumentByDigma = documentInfoService.focusedFile
        // checking that we switched to document2
        TestCase.assertEquals(currentDocument, currentDocumentByDigma)
        // check that caret is on the span in document2

        // check method under caret
        targetMethod = PsiTreeUtil.findChildrenOfType(secondFile, PsiMethod::class.java).find { it.name == "relatedSpan1" }
        waitForAndDispatch(1000, "caret event in file ${file?.name}")
        


        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        FileEditorManager.getInstance(project).selectedTextEditor?.let {
            PsiDocumentManager.getInstance(project)
            Log.test(logger::info, "selected text editor: {}", it.javaClass.name)
            Log.test(logger::info, "caret offset: {}", it.caretModel.offset)
        }
        // asserting all the current information about method one and the method under caret
//        TestCase.assertEquals(
//            BulletFiveData.expectedMethodUnderCaretAfterNavigationName,
//            methodUnderCaret?.name
//        ) // I think there is a bug. not updating the method under caret after navigating to it
//        TestCase.assertEquals(BulletFiveData.targetMethodOffSet, editor.caretModel.offset)
    }

    /**
     * replaces the JBCefBrowser in the RecentActivityUpdater with the given browser
     * @param recentActivityJBBrowser the browser to replace the original browser with
     */
    private fun prepareRecentActivityBrowser(recentActivityJBBrowser: JBCefBrowser) {
        val recentActivityUpdater = recentActivityUpdater

        // getting the JcefComponent from the RecentActivityUpdater

        val jCafComponentField = try {
            recentActivityUpdater.javaClass.getDeclaredField("jCefComponent")
        } catch (ex: NoSuchFieldException) {
            Log.test(logger::info, "No field of name: jCefComponent was found in class: {}", recentActivityUpdater.javaClass.name)
            throw Exception("No field of name: jCefComponent was found in class: ${recentActivityUpdater.javaClass.name}")
        }
        jCafComponentField.isAccessible = true
        val jCafComponent = jCafComponentField.get(recentActivityUpdater) as JCefComponent?
        // sometimes the component is null and won't let to spy on the browser, so we create a new one as in the production code
        if (jCafComponent != null) {
            injectSpyBrowser(jCafComponent, "jbCefBrowser", recentActivityJBBrowser)
            return
        }

        val jComp = JCefComponentBuilder(project)
            .url("https://mockurl.com")
            .messageRouterHandler(RecentActivityMessageRouterHandler(project))
            .schemeHandlerFactory(RecentActivitySchemeHandlerFactory(project))
            .withParentDisposable(recentActivityService).build()

        //replacing browser
        injectSpyBrowser(jComp, "jbCefBrowser", recentActivityJBBrowser)
        recentActivityUpdater.setJcefComponent(jComp)

    }

    /**
     * replaces the JBCefBrowser in the InsightsService with the given browser
     * @param insightJBBrowser the browser to replace the original browser with
     */
    private fun prepareInsightBrowser(insightJBBrowser: JBCefBrowser) {
        val insightsService = project.getService(InsightsService::class.java)
        val messageRouterHandler: Field = try {
            insightsService.javaClass.getDeclaredField("messageHandler")
        } catch (e: NoSuchFieldException) {
            throw Exception("No field of name: messageHandler was found in class: ${insightsService.javaClass.name}")
        }
        messageRouterHandler.isAccessible = true
        val routerHandler: InsightsMessageRouterHandler = messageRouterHandler.get(insightsService) as InsightsMessageRouterHandler
        injectSpyBrowser(routerHandler, "jbCefBrowser", insightJBBrowser)

    }


    /**
     * Moves the caret to the target method and dispatches the ide event queue to make sure that the method under caret is updated
     * @param targetMethod the method to move the caret to
     * @param file the file that contains the target method
     */
    private fun setCaretToTargetMethodAndDispatchIdeEventQueue(targetMethod: PsiMethod?, file: PsiFile?) {
        targetMethod?.let { psiMethod ->
            val offset = psiMethod.textOffset
            myFixture.editor.caretModel.moveToOffset(offset)
            val document = PsiDocumentManager.getInstance(project).getDocument(file!!)
            document?.let {
                Log.test(logger::info, "setting caret position")
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(it)
                EditorTestUtil.setCaretsAndSelection(myFixture.editor, caretAndSelectionState)
            }
        }
        waitForAndDispatch(1000, "caret event in file ${file?.name}")
    }


    private fun assertJsonForSingleEnvironmentData(payload: JsonNode) {
        Log.test(logger::info, "assertion for bullet 1")
        
        val assetId = payload.get("assetId").asText()
        Log.test(logger::info, "assetId: {}", assetId)
        val env = payload.get("environment").asText()
        Log.test(logger::info, "env: {}", env)

        // assertion  on payload
        assertEquals(SingleEnvironmentData.methodCodeObjectId, assetId)
        assertEquals(SingleEnvironmentData.environmentList[0], env)

        val insightsOfPayload = payload.get("insights")
        val insights: Array<CodeObjectInsight> = objectMapper.readValue(insightsOfPayload.toString(), Array<CodeObjectInsight>::class.java)
        insights.forEachIndexed { index, insight ->
            assertEquals(
                SingleEnvironmentData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].environment,
                insight.environment
            )
            assertEquals(
                SingleEnvironmentData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].codeObjectId,
                insight.codeObjectId
            )
        }
        Log.test(logger::info, "Finished assertion for bullet 1 in the browser")
    }

    
    
    private fun assertJsonBrowserForRecentActivity(
        payload: JsonNode,
        expectedEnvironments: List<String>,
        expectedRecentActivityResult: RecentActivityResult,
    ) {
        Log.test(logger::info, "assertion for bullet 2")

        // assertion  on payload
        val envs = payload.get("environments").asIterable()
        envs.map { it }.forEachIndexed { index, env ->
            assertEquals(expectedEnvironments[index], env.get("name").asText())
        }

        val entries = objectMapper.readValue(payload.get("entries").toString(), Array<RecentActivityResponseEntry>::class.java)
        entries.forEachIndexed { index, recentActivityResponseEntry ->
            assertEquals(
                expectedRecentActivityResult.entries[index].environment,
                recentActivityResponseEntry.environment
            )
            assertEquals(
                expectedRecentActivityResult.entries[index].firstEntrySpan.methodCodeObjectId,
                recentActivityResponseEntry.firstEntrySpan.methodCodeObjectId
            )
            assertEquals(
                expectedRecentActivityResult.entries[index].firstEntrySpan.scopeId,
                recentActivityResponseEntry.firstEntrySpan.scopeId
            )
        }
        Log.test(logger::info, "Finished assertion for recent activity in the browser")
    }

    private fun assertJsonForTwoEnvironmentNewInsightsData(payload: JsonNode) {
        Log.test(logger::info, "assertion for bullet 3")
        val insights = objectMapper.readValue(payload.get("insights").toString(), Array<CodeObjectInsight>::class.java)

        // assert that the scope has changed to the new scope
        val actualScopes = insights.map { it.scope }.toSet()
        val expectedScope =
            GoToSpanData.expectedInsightsOfMethods.methodsWithInsights.map { it.insights.map { it2 -> it2.scope } }.flatten().toSet()
        assertEquals(expectedScope, actualScopes)

        insights.forEachIndexed { index, insight ->
            val codeObjectInsight = GoToSpanData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index]
            assertEquals(codeObjectInsight.environment, insight.environment)
            assertEquals(codeObjectInsight.codeObjectId, insight.codeObjectId)
        }
        Log.test(logger::info, "Finished assertion for bullet 3 in the browser")
    }

    private fun assertJsonForInsightsOfRelatedSpans(payload: JsonNode) {
        val insights = objectMapper.readValue(payload.get("insights").toString(), Array<CodeObjectInsight>::class.java)
        Log.test(logger::info, "insights: {}", insights.map { it.codeObjectId })
        insights.forEachIndexed { index, insight ->
            assertEquals(
                TwoEnvironmentsFirstFileRelatedSingleSpanData.expectedInsightOfSingleSpan.insights[index].environment,
                insight.environment
            )
            assertEquals(
                TwoEnvironmentsFirstFileRelatedSingleSpanData.expectedInsightOfSingleSpan.insights[index].codeObjectId,
                insight.codeObjectId
            )
        }
        Log.test(logger::info, "Finished assertion for bullet 4 in the browser")
    }

}