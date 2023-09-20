package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.SkipInHeadlessEnvironment
import com.intellij.ui.jcef.JBCefBrowser
import junit.framework.TestCase
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.insights.InsightsMessageRouterHandler
import org.digma.intellij.plugin.jcef.common.JCefBrowserUtil
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.test.system.framework.DigmaAssertion
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
import org.digma.intellij.plugin.ui.recentactivity.RecentActivityService
import org.digma.intellij.plugin.ui.service.InsightsService
import java.lang.reflect.Field

class TestCaseBasicFlow : DigmaTestCase() {

    private val documentInfoService: DocumentInfoService
        get() = DocumentInfoService.getInstance(project)


    override fun getTestDataPath(): String {
        return "src/test/resources"
    }


    override fun setUp() {
        super.setUp()
        prepareBrowsers()
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
        setupExecuteJSOf(insightCefBrowser)
        // use browser spy to verify recent activities from env2
        setupExecuteJSOf(recentActivityCefBrowser)
    }

    fun testSowFlow() {
        var incomingEnv: String? = null
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { env, _ ->
            Log.test(logger::info, "env changed to: {}", env)
            incomingEnv = env
        }


        mockGetEnvironments(mockAnalyticsProvider, BulletOneData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, BulletOneData.expectedInsightsOfMethods)
        mockGetRecentActivity(mockAnalyticsProvider, BulletOneData.expectedRecentActivityResult)
        mockGetInsightOfSingeSpan(mockAnalyticsProvider, BulletFourData.expectedInsightOfSingleSpan) // those insights are for both envs
        mockGetCodeObjectNavigation(mockAnalyticsProvider, BulletFiveData.codeObjectNavigation)
        //todo: mock the notification endpoint in the analyticsProvider 


        // Bullet One
        // prepare single environment mock with insights and recent activities

        waitForRecentActivityToLoad()

        //does not exist any more
//        val processRecentActivityInitMethodRef = getMethodReference(recentActivityService, "processRecentActivityInitialized")
//        invokeMethod(recentActivityService, processRecentActivityInitMethodRef)


        waitFor(1000, "browser spy to be ready")
        val files = myFixture.configureByFiles(BulletOneData.DOC_NAME, BulletFiveData.DOC_NAME)
        val firstFile = files[0]
        val secondFile = files[1]
//        myFixture.openFileInEditor(firstFile.virtualFile)

        waitForAndDispatch(1000, "events after opening ${firstFile?.name}")


        //finding the target method for method1
        val methods = PsiTreeUtil.findChildrenOfType(firstFile, PsiMethod::class.java)
        var targetMethod = methods.find { it.name == "method1" }

        setCaretToTargetMethodAndDispatchIdeEventQueue(targetMethod, firstFile)

        // test that latest method under caret is method 1
        var methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret

        // asserting all the current information about method one and the method under caret
        DigmaAssertion.assertEquals(targetMethod?.name, methodUnderCaret.name)
        DigmaAssertion.assertEquals(targetMethod?.containingClass?.name, methodUnderCaret.className)
        DigmaAssertion.assertEquals(BulletOneData.methodCodeObjectId, methodUnderCaret.id)

        //see that the insights of method1 are present

        val methodInfoOfMethodUnderCaret = project.service<DocumentInfoService>().findMethodInfo(methodUnderCaret.id)

        //checking the insights of method1
        val cachedInsights = project.service<DocumentInfoService>().getCachedMethodInsights(methodInfoOfMethodUnderCaret!!)

        cachedInsights.forEach { insight ->
            DigmaAssertion.assertEquals(BulletOneData.methodCodeObjectId, insight.codeObjectId)
            DigmaAssertion.assertEquals(BulletOneData.environmentList[0], insight.environment)
        }


        // see that recentActivities are present from env1
        val latestActivityResult = recentActivityService.getRecentActivities(BulletOneData.environmentList)

        assertNotNull(latestActivityResult)
        // force non-null because the assertion above will fail if null
        latestActivityResult!!.entries.forEach {
            DigmaAssertion.assertEquals(BulletOneData.environmentList[0], it.environment)
            it.slimAggregatedInsights.forEach { insight ->
                DigmaAssertion.assertTrue(insight.codeObjectIds.contains(BulletOneData.methodCodeObjectId))
            }
        }

        // assert that we are in the correct environment
        assertEquals(BulletOneData.environmentList[0], analyticsService.environment.getCurrent())

        //  assert from browser spy that the insights were pushed to the browser
        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonBrowserForBulletOne)

        //  assert from browser spy that the recent activities were pushed to the browser
        assertQueueOfActionWithinTimeout("RECENT_ACTIVITY/SET_DATA", 5000) {
            assertJsonBrowserForRecentActivity(it, BulletOneData.environmentList, BulletOneData.expectedRecentActivityResult)
        }


        // Bullet Two
//        notReadyToTest()

        // bullet 2 - Given new trace from new environment (Env2) arrived.
        // mock new recentActivities from env 2 in analyticsProvider
        mockGetEnvironments(mockAnalyticsProvider, BulletTwoData.environmentList)
        mockGetRecentActivity(mockAnalyticsProvider, BulletTwoData.expectedRecentActivityResult)


        waitFor(1000, "new environments to be fetched")
        // call recentActivityService.fetchRecentActivities()

        // force fetching of recent activities
        recentActivityUpdater.updateLatestActivities()
        waitForAndDispatch(1000, "recent activities to be fetched")


        // assert that the new recentActivities are from env2:

        // in the plugin runtime
        val recentActivityResultFromEnv2 = recentActivityService.getRecentActivities(BulletTwoData.environmentList)
        assertNotNull(recentActivityResultFromEnv2)
        assertTrue(recentActivityResultFromEnv2!!.entries.any { it.environment == BulletTwoData.environmentList[1] }) // forcing non-null because the assertion above will fail if null

        // in the browser
        assertQueueOfActionWithinTimeout("RECENT_ACTIVITY/SET_DATA", 5000) {
            assertJsonBrowserForRecentActivity(it, BulletTwoData.environmentList, BulletTwoData.expectedRecentActivityResult)
        }

        // bullet 3 -
        // When I click on the link (span_endpoint1) in the recent activity
        // Then the selected environment in the main panel should be changed from Env1 to Env2
        // and I should see that the scope has been changed to span_endpoint1 and 
        // span_endpoint1 insights (all insights**) are visible

        mockGetEnvironments(mockAnalyticsProvider, BulletThreeData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, BulletThreeData.expectedInsightsOfMethods) // those insights are for env2
        mockGetRecentActivity(mockAnalyticsProvider, BulletThreeData.expectedRecentActivityResult)
        

        // call processRecentActivityGoToSpan -- this is actually the trigger to open the recent activity insights in the side panel. also do the navigation in the code to the span
        recentActivityService.processRecentActivityGoToSpanRequest(BulletThreeData.goToSpanRequestPayload)
        waitForAndDispatch(1000, "go to span to be processed")


        // check that the environment is changed to env2
        // either use the environment changed event or do to analyticsService.environment.getCurrent()
        assertEquals(BulletThreeData.environmentList[1], analyticsService.environment.getCurrent())
        // assert that the caret moved to the span in the code

        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        assertEquals(methodUnderCaret.id, BulletThreeData.goToSpanRequestPayload.span.methodCodeObjectId)
        assertEquals(targetMethod?.name, methodUnderCaret.name) //todo: not sure about that

        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonForBulletThree)


        // bullet 4 -
        // When I click on span_2 link on insight1
        // Then I should see the scoped changed to span_2 and insights of span_2 are shown.
        waitForAndDispatch(1000, "update the single span insights") // todo: maybe too long of a time out

        // check the spy --> should push some json to the browser - should see that span_2 is in the json
//        replaceExecuteJSWithAssertionFunction(insightCefBrowser, this::assertJsonForBulletFour)

        // call processInsightGoToSpan~~~~ --> to go the  span that is related to the insight1 - goToInsight
        project.getService(InsightsService::class.java)
            .showInsight(BulletFourData.relatedSpansToMethod[0]) // another span to show insights for from another document

        waitForAndDispatch(1000, "insights to be shown")

        assertQueueOfActionWithinTimeout("INSIGHTS/SET_DATA", 5000, this::assertJsonForBulletFour)

        // bullet 5 -

        waitForDocumentInfoToLoad(secondFile)

        val targetButton = CodeNavigationButton(project)

        // When I click target icon (go to code)
        targetButton.doClick()

        waitForAndDispatch(1000, "go to code to be processed and switch to document2")

        // Then I should be navigated to document#2 and the caret should be navigated to the span location.
        val currentDocument = FileEditorManager.getInstance(project).selectedEditor?.file
        Log.test(logger::info, "currentDocument: {}", currentDocument?.name)
        //check in document Info service that the document is changed to document2
        val currentDocumentByDigma = documentInfoService.focusedFile
        Log.test(logger::info, "currentDocumentByDigma: {}", currentDocumentByDigma?.name)
        // checking that we switched to document2
        TestCase.assertEquals(currentDocument, currentDocumentByDigma)
        // check that caret is on the span in document2

        // check method under caret
        targetMethod = PsiTreeUtil.findChildrenOfType(secondFile, PsiMethod::class.java).find { it.name == "relatedSpan1" }
        waitForAndDispatch(1000, "caret event in file ${file?.name}")
        


        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        Log.test(logger::info, "current document name: {}", currentDocument?.name)
        Log.test(logger::info, "target method name: {}", targetMethod?.name)
        FileEditorManager.getInstance(project).selectedTextEditor?.let {
            PsiDocumentManager.getInstance(project)
            Log.test(logger::info, "selected text editor: {}", it.javaClass.name)
            Log.test(logger::info, "caret offset: {}", it.caretModel.offset)
        }
        // asserting all the current information about method one and the method under caret
        TestCase.assertEquals(
            BulletFiveData.expectedMethodUnderCaretAfterNavigationName,
            methodUnderCaret?.name
        ) // I think there is a bug. not updating the method under caret after navigating to it
        TestCase.assertEquals(BulletFiveData.targetMethodOffSet, editor.caretModel.offset)
    }

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


    private fun assertJsonBrowserForBulletOne(payload: JsonNode) {
        Log.test(logger::info, "assertion for bullet 1")
        
        val assetId = payload.get("assetId").asText()
        Log.test(logger::info, "assetId: {}", assetId)
        val env = payload.get("environment").asText()
        Log.test(logger::info, "env: {}", env)

        // assertion  on payload
        assertEquals(BulletOneData.methodCodeObjectId, assetId)
        assertEquals(BulletOneData.environmentList[0], env)

        val insightsOfPayload = payload.get("insights")
        val insights: Array<CodeObjectInsight> = objectMapper.readValue(insightsOfPayload.toString(), Array<CodeObjectInsight>::class.java)
        insights.forEachIndexed { index, insight ->
            assertEquals(
                BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].environment,
                insight.environment
            )
            assertEquals(
                BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].codeObjectId,
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

    private fun assertJsonForBulletThree(payload: JsonNode) {
        Log.test(logger::info, "assertion for bullet 3")
        val insights = objectMapper.readValue(payload.get("insights").toString(), Array<CodeObjectInsight>::class.java)

        // assert that the scope has changed to the new scope
        val actualScopes = insights.map { it.scope }.toSet()
        val expectedScope =
            BulletThreeData.expectedInsightsOfMethods.methodsWithInsights.map { it.insights.map { it2 -> it2.scope } }.flatten().toSet()
        assertEquals(expectedScope, actualScopes)

        insights.forEachIndexed { index, insight ->
            val codeObjectInsight = BulletThreeData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index]
            assertEquals(codeObjectInsight.environment, insight.environment)
            assertEquals(codeObjectInsight.codeObjectId, insight.codeObjectId)
        }
        Log.test(logger::info, "Finished assertion for bullet 3 in the browser")
    }

    private fun assertJsonForBulletFour(payload: JsonNode) {
        // assertion  on payload
        val insights = objectMapper.readValue(payload.get("insights").toString(), Array<CodeObjectInsight>::class.java)
        Log.test(logger::info, "insights: {}", insights.map { it.codeObjectId })
        insights.forEachIndexed { index, insight ->
            assertEquals(
                BulletFourData.expectedInsightOfSingleSpan.insights[index].environment,
                insight.environment
            )
            assertEquals(
                BulletFourData.expectedInsightOfSingleSpan.insights[index].codeObjectId,
                insight.codeObjectId
            )
        }
        Log.test(logger::info, "Finished assertion for bullet 4 in the browser")
    }

}