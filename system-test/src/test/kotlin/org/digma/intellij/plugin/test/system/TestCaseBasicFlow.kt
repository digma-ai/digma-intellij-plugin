package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.jcef.JBCefBrowser
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.insights.InsightsMessageRouterHandler
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanPayload
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.recentactivity.RecentActivityService
import org.digma.intellij.plugin.test.system.framework.DigmaAssertion
import org.digma.intellij.plugin.test.system.framework.clearSpyAssertion
import org.digma.intellij.plugin.test.system.framework.createSpyBrowsers
import org.digma.intellij.plugin.test.system.framework.environmentList
import org.digma.intellij.plugin.test.system.framework.getMethodReference
import org.digma.intellij.plugin.test.system.framework.injectSpyBrowser
import org.digma.intellij.plugin.test.system.framework.invokeMethod
import org.digma.intellij.plugin.test.system.framework.mockGetCodeObjectNavigation
import org.digma.intellij.plugin.test.system.framework.mockGetEnvironments
import org.digma.intellij.plugin.test.system.framework.mockGetInsightOfSingeSpan
import org.digma.intellij.plugin.test.system.framework.mockGetInsightsOfMethods
import org.digma.intellij.plugin.test.system.framework.mockGetRecentActivity
import org.digma.intellij.plugin.test.system.framework.replaceExecuteJSWithAssertionFunction
import org.digma.intellij.plugin.ui.common.CodeNavigationButton
import org.digma.intellij.plugin.ui.service.InsightsService
import org.jetbrains.plugins.notebooks.visualization.EDITOR_SCROLLING_POSITION_KEEPER_KEY
import java.lang.reflect.Field

class TestCaseBasicFlow : DigmaTestCase() {

    private val documentInfoService: DocumentInfoService
        get() = DocumentInfoService.getInstance(project)


    override fun getTestDataPath(): String {
        return "src/test/resources"
    }


    fun testSowFlow() {

        //prepare the browser for mocking of InsightsService
        val (insightJBBrowser, insightCefBrowser) = createSpyBrowsers()
        // inject the insight browser spy into the InsightsServiceMessage handler
        prepareInsightBrowser(insightJBBrowser)

        // prepare the browser for mocking of RecentActivityService
        val (recentActivityJBBrowser, recentActivityCefBrowser) = createSpyBrowsers()
        injectSpyBrowser(recentActivityService, "jbCefBrowser", recentActivityJBBrowser)

        var assertionEnv: String? = null
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { env, _ ->
            Log.test(logger::info, "env changed to: {}", env)
            assertionEnv = env
        }

        mockGetInsightOfSingeSpan(mockAnalyticsProvider, BulletFourData.expectedInsightOfSingleSpan) // those insights are for both envs

        // Bullet One
        // prepare single environment mock with insights and recent activities

        mockGetEnvironments(mockAnalyticsProvider, BulletOneData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, BulletOneData.expectedInsightsOfMethods)
        mockGetRecentActivity(mockAnalyticsProvider, BulletOneData.expectedRecentActivityResult)
        mockGetCodeObjectNavigation(mockAnalyticsProvider, BulletFiveData.codeObjectNavigation)


        while (assertionEnv == null) {
            waitFor(500, "env to be changed")
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }


        val processRecentActivityInitMethodRef = getMethodReference(recentActivityService, "processRecentActivityInitialized")
        invokeMethod(recentActivityService, processRecentActivityInitMethodRef)

        // prepare assertion in the browser for the insights that will be pushed of the method under caret
        replaceExecuteJSWithAssertionFunction(insightCefBrowser, this::assertJsonBrowserForBulletOne)

        waitFor(1000, "browser spy to be ready")
        val firstFile = myFixture.configureByFile(BulletOneData.DOC_NAME)
        val secondFile = myFixture.configureByFile(BulletFiveData.DOC_NAME)
        myFixture.openFileInEditor(firstFile.virtualFile)
        waitFor(1000, "events after opening ${firstFile?.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()


        //finding the target method for method1

        val editor = myFixture.editor

        val methods = PsiTreeUtil.findChildrenOfType(firstFile, PsiMethod::class.java)
        var targetMethod = methods.find { it.name == "method1" }

        targetMethod?.let { psiMethod ->
            val offset = psiMethod.textOffset
            editor.caretModel.moveToOffset(offset)
            val document = PsiDocumentManager.getInstance(project).getDocument(firstFile!!)
            document?.let {
                Log.test(logger::info, "setting caret position")
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(it)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }
        readyToTest()
        waitFor(1000, "caret event in file ${firstFile?.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()


        DigmaAssertion.assertFlag()
        // test that latest method under caret is method 1
        var methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret

        // asserting all the current information about method one and the method under caret
        DigmaAssertion.assertEquals(targetMethod?.name, methodUnderCaret.name)
        DigmaAssertion.assertEquals(targetMethod?.containingClass?.name, methodUnderCaret.className)
        DigmaAssertion.assertEquals(BulletOneData.methodCodeObjectId, methodUnderCaret.id)

        //see that the insights of method1 are present

        val methodInfoOfMethodUnderCaret = project.service<DocumentInfoService>().findMethodInfo(methodUnderCaret.id)

        val cachedInsights = project.service<DocumentInfoService>().getCachedMethodInsights(methodInfoOfMethodUnderCaret!!)

        cachedInsights.forEach { insight ->
            DigmaAssertion.assertEquals(BulletOneData.methodCodeObjectId, insight.codeObjectId)
            DigmaAssertion.assertEquals(BulletOneData.environmentList[0], insight.environment)
        }


        // see that recentActivities are present from env1
        val latestActivityResult = getRecentActivityResult(recentActivityService)
        latestActivityResult.entries.forEach {
            DigmaAssertion.assertEquals(BulletOneData.environmentList[0], it.environment)
            it.slimAggregatedInsights.forEach { insight ->
                DigmaAssertion.assertTrue(insight.codeObjectIds.contains(BulletOneData.methodCodeObjectId))
            }
            Log.test(logger::info, "recentActivityEntry: {}", it)
        }

        // Bullet Two
        notReadyToTest()

        // bullet 2 - Given new trace from new environment (Env2) arrived.
        // mock new recentActivities from env 2 in analyticsProvider
        mockGetEnvironments(mockAnalyticsProvider, BulletTwoData.environmentList)
        mockGetRecentActivity(mockAnalyticsProvider, BulletTwoData.expectedRecentActivityResult)

        // use browser spy to verify recent activities from env2 - I don't think that there is a browser push for recent activities
        replaceExecuteJSWithAssertionFunction(recentActivityCefBrowser, this::assertJsonBrowserForBulletTwo)

        waitFor(1000, "new environments to be fetched")
        // wait for time event ??
        // call recentActivityService.fetchRecentActivities()
        readyToTest()
        // force fetching of recent activities
        val fetchRecentActivitiesMethodRef = getMethodReference(recentActivityService, "fetchRecentActivities")
        invokeMethod(recentActivityService, fetchRecentActivitiesMethodRef)

        waitFor(1000, "recent activities to be fetched")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        DigmaAssertion.assertFlag()

        // Then I should see new activities from Env2 in the recent activity view.
        val recentActivityResultFromEnv2 = getRecentActivityResult(recentActivityService)
        // assert that the new recentActivities are from env2
        DigmaAssertion.assertTrue(recentActivityResultFromEnv2.entries.any { it.environment == BulletTwoData.environmentList[1] })
        DigmaAssertion.assertFlag()

        clearSpyAssertion(recentActivityCefBrowser)

        // bullet 3 -
        // When I click on the link (span_endpoint1) in the recent activity
        // Then the selected environment in the main panel should be changed from Env1 to Env2
        // and I should see that the scope has been changed to span_endpoint1 and 
        // span_endpoint1 insights (all insights**) are visible
        notReadyToTest()

        mockGetEnvironments(mockAnalyticsProvider, BulletThreeData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, BulletThreeData.expectedInsightsOfMethods) // those insights are for env2
        mockGetRecentActivity(mockAnalyticsProvider, BulletThreeData.expectedRecentActivityResult)


        // check the spy --> should push some json to the browser - should see that env2 is in the json
        waitFor(1000, "spy to take effect")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        replaceExecuteJSWithAssertionFunction(insightCefBrowser, this::assertJsonForBulletThree)

        readyToTest()
        // call processRecentActivityGoToSpan -- this is actually the trigger to open the recent activity insights in the side panel. also do the navigation in the code to the span
        val goToSpanMethodRef = getMethodReference(
            recentActivityService,
            "processRecentActivityGoToSpanRequest",
            RecentActivityEntrySpanPayload::class.java,
            Project::class.java
        )
        invokeMethod(recentActivityService, goToSpanMethodRef, BulletThreeData.goToSpanRequestPayload, project)

        waitFor(1000, "go to span to be processed")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        
        DigmaAssertion.assertFlag()


        // check that the environment is changed to env2
        // either use the environment changed event or do to analyticsService.environment.getCurrent()
        TestCase.assertEquals(BulletThreeData.environmentList[1], analyticsService.environment.getCurrent())
        // assert that the caret moved to the span in the code

        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        TestCase.assertEquals(methodUnderCaret.id, BulletThreeData.goToSpanRequestPayload.span.methodCodeObjectId)
        TestCase.assertEquals(targetMethod?.name, methodUnderCaret.name)

        notReadyToTest()


        // bullet 4 -
        // When I click on span_2 link on insight1
        // Then I should see the scoped changed to span_2 and insights of span_2 are shown.
        waitFor(2000, "update the single span insights")

        // check the spy --> should push some json to the browser - should see that span_2 is in the json
        replaceExecuteJSWithAssertionFunction(insightCefBrowser, this::assertJsonForBulletFour)
        waitFor(1000, "update the single span insights")

        readyToTest()
        // call processInsightGoToSpan~~~~ --> to go the  span that is related to the insight1 - goToInsight
        project.getService(InsightsService::class.java)
            .showInsight(BulletFourData.relatedSpansToMethod[0]) // another span to show insights for from another document
        waitFor(1000, "insights to be shown")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        DigmaAssertion.assertFlag()

        notReadyToTest()

        // bullet 5 -

        waitForDocumentInfoToLoad(secondFile)

        replaceExecuteJSWithAssertionFunction(insightCefBrowser) {
            Log.test(logger::info, "assertion for bullet 5")
            Log.test(logger::info, "placeholder for assertion for bullet 5 so I can see that we replaced the assertion function")
        }
        val targetButton = CodeNavigationButton(project)

        readyToTest()

        // When I click target icon (go to code)
        targetButton.doClick()
        waitFor(1000, "go to code to be processed and switch to document2")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

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
        targetMethod?.let { psiMethod ->
            val offset = psiMethod.textOffset
            editor.caretModel.moveToOffset(offset)
            val document = PsiDocumentManager.getInstance(project).getDocument(secondFile!!)
            document?.let {
                Log.test(logger::info, "setting caret position")
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(it)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }
        readyToTest()
        waitFor(2000, "caret event in file ${secondFile?.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()


        DigmaAssertion.assertFlag()
        // test that latest method under caret is method 1
        methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret
        
        // asserting all the current information about method one and the method under caret
        TestCase.assertEquals(targetMethod?.name, methodUnderCaret?.name)
        TestCase.assertEquals(BulletFiveData.targetMethodOffSet, editor.caretModel.offset)
    }
    

    private fun assertJsonBrowserForBulletOne(json: String) {
        if (!readyToAssert) {
            return
        }
        Log.test(logger::info, "assertion for bullet 1")
        val mapper = ObjectMapper()
        val responseToBrowser = mapper.readTree(json)
        Log.test(logger::info, "responseToBrowser: {}", json)
        val action = responseToBrowser.get("action").asText()
        Log.test(logger::info, "action: {}", action)
        val payload = responseToBrowser.get("payload")
        Log.test(logger::info, "payload: {}", payload)
        val assetId = payload.get("assetId").asText()
        Log.test(logger::info, "assetId: {}", assetId)
        val env = payload.get("environment").asText()
        Log.test(logger::info, "env: {}", env)

        // assertion on response
        DigmaAssertion.assertEquals("INSIGHTS/SET_DATA", action)
        DigmaAssertion.assertEquals("digma", responseToBrowser.get("type").asText())

        // assertion  on payload
        DigmaAssertion.assertEquals(BulletOneData.methodCodeObjectId, assetId)
        DigmaAssertion.assertEquals(BulletOneData.environmentList[0], env)

        val insightsOfPayload = payload.get("insights")
        val insights: Array<CodeObjectInsight> = mapper.readValue(insightsOfPayload.toString(), Array<CodeObjectInsight>::class.java)
        insights.forEachIndexed { index, insight ->
            DigmaAssertion.assertEquals(
                BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].environment,
                insight.environment
            )
            DigmaAssertion.assertEquals(
                BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].codeObjectId,
                insight.codeObjectId
            )
        }

        // assert that we are in the correct environment
        DigmaAssertion.assertEquals(BulletOneData.environmentList[0], analyticsService.environment.getCurrent())

        Log.test(logger::info, "Finished assertion for bullet 1 in the browser")
    }

    private fun assertJsonBrowserForBulletTwo(json: String) {
        if (!readyToAssert) {
            return
        }
        Log.test(logger::info, "assertion for bullet 2")
        val mapper = ObjectMapper()
        val responseToBrowser = mapper.readTree(json)
        val action = responseToBrowser.get("action").asText()
        val payload = responseToBrowser.get("payload")

        // assertion on response
        DigmaAssertion.assertEquals("RECENT_ACTIVITY/SET_DATA", action)
        DigmaAssertion.assertEquals("digma", responseToBrowser.get("type").asText())

        // assertion  on payload
        val envs = payload.get("environments").asIterable()
        envs.map { it.asText() }.forEachIndexed { index, env ->
            DigmaAssertion.assertEquals(BulletTwoData.environmentList[index], env)
        }

        val entries = mapper.readValue(payload.get("entries").toString(), Array<RecentActivityResponseEntry>::class.java)
        entries.forEachIndexed { index, recentActivityResponseEntry ->
            DigmaAssertion.assertEquals(
                BulletTwoData.expectedRecentActivityResult.entries[index].environment,
                recentActivityResponseEntry.environment
            )
            DigmaAssertion.assertEquals(
                BulletTwoData.expectedRecentActivityResult.entries[index].firstEntrySpan.methodCodeObjectId,
                recentActivityResponseEntry.firstEntrySpan.methodCodeObjectId
            )
            DigmaAssertion.assertEquals(
                BulletTwoData.expectedRecentActivityResult.entries[index].firstEntrySpan.scopeId,
                recentActivityResponseEntry.firstEntrySpan.scopeId
            )
        }
        Log.test(logger::info, "Finished assertion for bullet 2 in the browser")
    }

    private fun assertJsonForBulletThree(json: String) {
        if (!readyToAssert) {
            return
        }
        val mapper = ObjectMapper()
        val responseToBrowser = mapper.readTree(json)
        Log.test(logger::info, "assertion for bullet 3")
        val insights = mapper.readValue(responseToBrowser.get("payload").get("insights").toString(), Array<CodeObjectInsight>::class.java)

        // assert that the scope has changed to the new scope
        val actualScopes = insights.map { it.scope }.toSet()
        val expectedScope =
            BulletThreeData.expectedInsightsOfMethods.methodsWithInsights.map { it.insights.map { it2 -> it2.scope } }.flatten().toSet()
        DigmaAssertion.assertEquals(expectedScope, actualScopes)
        Log.test(logger::info, "Finished assertion for bullet 3 in the browser")
    }

    private fun assertJsonForBulletFour(json: String) {
        if (!readyToAssert) {
            return
        }
        Log.test(logger::info, "assertion for bullet 4")
        Log.test(logger::info, "Request Data: {}", json)
        val mapper = ObjectMapper()
        val responseToBrowser = mapper.readTree(json)

        val payload = responseToBrowser.get("payload")

        // assertion on response
        DigmaAssertion.assertEquals("INSIGHTS/SET_DATA", responseToBrowser.get("action").asText())
        DigmaAssertion.assertEquals("digma", responseToBrowser.get("type").asText())

        // assertion  on payload
        val insights = mapper.readValue(payload.get("insights").toString(), Array<CodeObjectInsight>::class.java)
        Log.test(logger::info, "insights: {}", insights.map { it.codeObjectId })
        insights.forEachIndexed { index, insight ->
            DigmaAssertion.assertEquals(
                BulletFourData.expectedInsightOfSingleSpan.insights[index].environment,
                insight.environment
            )
            DigmaAssertion.assertEquals(
                BulletFourData.expectedInsightOfSingleSpan.insights[index].codeObjectId,
                insight.codeObjectId
            )
        }
        Log.test(logger::info, "Finished assertion for bullet 4 in the browser")
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
    private fun getRecentActivityResult(recentActivityService: RecentActivityService): RecentActivityResult {
        val latestActivityProperty = recentActivityService.javaClass.getDeclaredField("latestActivityResult")
        latestActivityProperty.isAccessible = true
        return latestActivityProperty.get(recentActivityService) as RecentActivityResult
    }
    
}