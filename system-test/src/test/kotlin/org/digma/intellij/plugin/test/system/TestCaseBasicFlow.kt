package org.digma.intellij.plugin.test.system

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.PlatformTestUtil
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
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.recentactivity.RecentActivityService
import org.digma.intellij.plugin.test.system.framework.environmentList
import org.digma.intellij.plugin.test.system.framework.getMethodReference
import org.digma.intellij.plugin.test.system.framework.invokeMethod
import org.digma.intellij.plugin.test.system.framework.mockGetEnvironments
import org.digma.intellij.plugin.test.system.framework.mockGetInsightsOfMethods
import org.digma.intellij.plugin.test.system.framework.mockGetRecentActivity
import org.digma.intellij.plugin.test.system.framework.prepareDefaultSpyCalls
import org.digma.intellij.plugin.test.system.framework.replaceCefBrowserWithSpy
import org.digma.intellij.plugin.test.system.framework.replaceExecuteJSWithAssertionFunction
import org.digma.intellij.plugin.test.system.framework.replaceJBCefWithExistingSpy
import org.digma.intellij.plugin.ui.service.InsightsService

class TestCaseBasicFlow : DigmaTestCase() {

    private val documentInfoService: DocumentInfoService
        get() = DocumentInfoService.getInstance(project)


    override fun getTestDataPath(): String {
        return "src/test/resources"
    }


    fun testSowFlow() {

        //prepare the browser for mocking of InsightsService
        val (jbCef, cef) = replaceCefBrowserWithSpy(
            containingService = project.service<InsightsService>(),
            messageHandlerFieldName = "messageHandler",
            messageHandlerType = InsightsMessageRouterHandler::class.java,
            jbBrowserFieldName = "jbCefBrowser"
        )
        prepareDefaultSpyCalls(jbCef, cef)

        // prepare the browser for mocking of RecentActivityService
        replaceJBCefWithExistingSpy(recentActivityService, "jbCefBrowser", jbCef)


        // Bullet One
        // prepare single environment mock with insights and recent activities

        mockGetEnvironments(mockAnalyticsProvider, BulletOneData.environmentList)
        mockGetInsightsOfMethods(mockAnalyticsProvider, BulletOneData.expectedInsightsOfMethods)
        mockGetRecentActivity(mockAnalyticsProvider, BulletOneData.expectedRecentActivityResult)


        var asseritonEnv: String? = null
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { env, _ ->
            Log.test(logger::info, "env changed to: {}", env)
            asseritonEnv = env
        }

        while (asseritonEnv == null) {
            waitFor(500, "env to be changed")
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }

        val processRecentActivityInitMethodRef = getMethodReference(recentActivityService, "processRecentActivityInitialized")
        invokeMethod(recentActivityService, processRecentActivityInitMethodRef)

        // prepare assertion in the browser for the insights that will be pushed of the method under caret
        replaceExecuteJSWithAssertionFunction(cef, this::assertJsonBrowserForBulletOne)

        waitFor(2000, "browser spy to be ready")
        val psiFile = myFixture.configureByFile(BulletOneData.DOC_NAME)
        myFixture.openFileInEditor(psiFile.virtualFile)
        waitFor(2000, "events after opening ${psiFile.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()


        //finding the target method for method1

        val editor = myFixture.editor

        val methods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)
        val targetMethod = methods.find { it.name == "method1" }

        targetMethod?.let {
            val offset = targetMethod.textOffset
            editor.caretModel.moveToOffset(offset)
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            document?.let {
                Log.test(logger::info, "setting caret position")
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }
        readyToAssert()
        waitFor(1000, "caret event in file ${psiFile.name}")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        // test that latest method under caret is method 1
        val methodUnderCaret = project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret

        // asserting all the current information about method one and the method under caret
        assertEquals(targetMethod?.name, methodUnderCaret.name)
        assertEquals(targetMethod?.containingClass?.name, methodUnderCaret.className)
        assertEquals(BulletOneData.methodCodeObjectId, methodUnderCaret.id)

        //see that the insights of method1 are present

        val methodInfoOfMethodUnderCaret = project.service<DocumentInfoService>().findMethodInfo(methodUnderCaret.id)

        val cachedInsights = project.service<DocumentInfoService>().getCachedMethodInsights(methodInfoOfMethodUnderCaret!!)

        cachedInsights.forEach { insight ->
            assertEquals(BulletOneData.methodCodeObjectId, insight.codeObjectId)
            assertEquals(BulletOneData.environmentList[0], insight.environment)
        }


        // see that recentActivities are present from env1
        val latestActivityResult = getRecentActivityResult(recentActivityService)
        latestActivityResult.entries.forEach {
            assertEquals(BulletOneData.environmentList[0], it.environment)
            it.slimAggregatedInsights.forEach { insight ->
                TestCase.assertTrue(insight.codeObjectIds.contains(BulletOneData.methodCodeObjectId))
            }
            Log.test(logger::info, "recentActivityEntry: {}", it)
        }

        // Bullet Two
        notReadyToAssert()

        // bullet 2 - Given new trace from new environment (Env2) arrived.
        // mock new recentActivities from env 2 in analyticsProvider
        mockGetEnvironments(mockAnalyticsProvider, BulletTwoData.environmentList)
        mockGetRecentActivity(mockAnalyticsProvider, BulletTwoData.expectedRecentActivityResult)

        // use browser spy to verify recent activities from env2 - I don't think that there is a browser push for recent activities
        replaceExecuteJSWithAssertionFunction(cef, this::assertJsonBrowserForBulletTwo)

        waitFor(1000, "new environments to be fetched")
        // wait for time event ??
        // call recentActivityService.fetchRecentActivities()
        readyToAssert()
        // force fetching of recent activities
        val fetchRecentActivitiesMethodRef = getMethodReference(recentActivityService, "fetchRecentActivities")
        invokeMethod(recentActivityService, fetchRecentActivitiesMethodRef)

        waitFor(1000, "recent activities to be fetched")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        // Then I should see new activities from Env2 in the recent activity view.
        val recentActivityResultFromEnv2 = getRecentActivityResult(recentActivityService)
        // assert that the new recentActivities are from env2
        assertTrue(recentActivityResultFromEnv2.entries.any { it.environment == BulletTwoData.environmentList[1] })



        // bullet 3 -

        // When I click on the link (span_endpoint1) in the recent activity
        // Then the selected environment in the main panel should be changed from Env1 to Env2
        // and I should see that the scope has been changed to span_endpoint1 and 
        // span_endpoint1 insights (all insights**) are visible

        // call processRecentActivityGoToSpan~~~~
        // check the spy --> should push some json to the browser - should see that env2 is in the json

        // check that the environment is changed to env2
        // either use the environment changed event or do to analyticsService.environment.getCurrent()

        // test that the environment is changed to env2

        //wait for 500 ms

        // bullet 4 -
        // When I click on span_2 link on insight1
        // Then I should see the scoped changed to span_2 and insights of span_2 are shown.

        // call processInsightGoToSpan~~~~ --> to go the  span that is related to the insight1
        // check the spy --> should push some json to the browser - should see that span_2 is in the json

        // bullet 5 - 
        // When I click target icon (go to code)
        // Then I should be navigated to document#2 and the cursor should be navigated to the span location.

        // call processRecentActivityGoToSpanRequest
        // check that the document is changed to document2 in the editor
        // check that caret is on the span in document2


    }

    private fun assertJsonBrowserForBulletTwo(json: String) {
        Log.test(logger::info, "responseToBrowser: {}", json)
        Log.test(logger::info, "second replacement of executeJS")
        val mapper = ObjectMapper()
        val responseToBrowser = mapper.readTree(json)
        val action = responseToBrowser.get("action").asText()
        Log.test(logger::info, "action: {}", action)
        val payload = responseToBrowser.get("payload")
        Log.test(logger::info, "payload: {}", payload)

        // assertion on response
        assertEquals("RECENT_ACTIVITY/SET_DATA", action)
        assertEquals("digma", responseToBrowser.get("type").asText())

        // assertion  on payload
        val envs = payload.get("environments").asIterable()
        envs.map { it.asText() }.forEachIndexed { index, env ->
            assertEquals(BulletTwoData.environmentList[index], env)
        }
        
        val entries = mapper.readValue(payload.get("entries").toString(), Array<RecentActivityResponseEntry>::class.java)
        entries.forEachIndexed { index, recentActivityResponseEntry -> 
            TestCase.assertEquals(BulletTwoData.expectedRecentActivityResult.entries[index].environment, recentActivityResponseEntry.environment)
            TestCase.assertEquals(BulletTwoData.expectedRecentActivityResult.entries[index].firstEntrySpan.methodCodeObjectId, recentActivityResponseEntry.firstEntrySpan.methodCodeObjectId)
            TestCase.assertEquals(BulletTwoData.expectedRecentActivityResult.entries[index].firstEntrySpan.scopeId, recentActivityResponseEntry.firstEntrySpan.scopeId)
        }
    }

    private fun getRecentActivityResult(recentActivityService: RecentActivityService): RecentActivityResult {
        val latestActivityProperty = recentActivityService.javaClass.getDeclaredField("latestActivityResult")
        latestActivityProperty.isAccessible = true
        return latestActivityProperty.get(recentActivityService) as RecentActivityResult
    }

    private fun assertJsonBrowserForBulletOne(json: String) {
        if (!readyToAssert) {
            return
        }

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
        assertEquals("INSIGHTS/SET_DATA", action)
        assertEquals("digma", responseToBrowser.get("type").asText())

        // assertion  on payload
        assertEquals(BulletOneData.methodCodeObjectId, assetId)
        assertEquals(BulletOneData.environmentList[0], env)

        val insightsOfPayload = payload.get("insights")
        val insights: Array<CodeObjectInsight> = mapper.readValue(insightsOfPayload.toString(), Array<CodeObjectInsight>::class.java)
        Log.test(logger::info, "insights: {}", insights)
        insights.forEachIndexed { index, insight ->
            assertEquals(BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].environment, insight.environment)
            assertEquals(BulletOneData.expectedInsightsOfMethods.methodsWithInsights[0].insights[index].codeObjectId, insight.codeObjectId)
        }

        // assert that we are in the correct environment
        assertEquals(BulletOneData.environmentList[0], analyticsService.environment.getCurrent())
        //assertRecentActivities
        val recentActivityService = project.getService(RecentActivityService::class.java)

        // end bullet one
    }

    fun testInsightAndActivityFlow() {

        var expectedEnv = environmentList[0]
        var actualEnv: String?
        var currentDocumentName = ""
//        var expectedDocumentName = document1Name
        // subscribe to env changed event
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { env, _ ->
            actualEnv = env
            assertEquals(expectedEnv, actualEnv)
        }
        // subscribe to documentInfoChanged event
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent { psiFile ->
            currentDocumentName = psiFile.name
//            assertEquals(expectedDocumentName, currentDocumentName)
        }

        // open document1
//        expectedDocumentName = document1Name
//        val document1File = myFixture.configureByFile(document1Name)
//        myFixture.openFileInEditor(document1File.virtualFile)

        //wait for 500 ms
        runBlocking { delay(500) }


        // navigate to method --> get the method codeObjectId
//        val methodList = collectMethodsInFile(document1File)
//        val destinationMethod = methodList.find { it.name == "isRelevantFile" }.let { it!! }
//        editor.caretModel.moveToOffset(destinationMethod.textOffset)

        // verify that method under caret is the same as the destinationMethod


//        val document1infoContainer = documentInfoService.getDocumentInfo(document1File)
//        if (document1infoContainer == null) {
//            TestCase.fail("document1infoContainer is null")
//            return
//        }

        // get insights of current file from document Info service
//        val insightMap: MutableMap<String, MutableList<CodeObjectInsight>> = document1infoContainer.allMethodWithInsightsMapForCurrentDocument
//        // check if the insights are correct that correlates to the current env and the codeObjectId of the method
//        assertInsightsForDocument(insightMap, expectedInsightsOfMethodsResponseEnv1)
//
//        val codeObjectOfDocument1: List<String> = getCodeObjectIdsFromDocumentContainer(document1infoContainer)
//        val methodInfosOfDocument1: Map<String, MethodInfo> = getMethodInfos(document1infoContainer)
//        // check that the number of methods in the documentInfoService is the same as the number of methods in the file
//        TestCase.assertEquals(methodList.size, methodInfosOfDocument1.size)


        // check what is the current env --> should be env1
        actualEnv = analyticsService.environment.getCurrent()
        TestCase.assertEquals(expectedEnv, actualEnv)


        // end of bullet one

        // see that recentActivities are present from env1
        val recentActivityService = project.getService(RecentActivityService::class.java)
        val latestActivityProperty = recentActivityService.javaClass.getDeclaredField("latestActivityResult")
        latestActivityProperty.isAccessible = true
        val latestActivityResult = latestActivityProperty.get(recentActivityService) as RecentActivityResult
        TestCase.assertEquals(expectedEnv, latestActivityResult.entries[0].environment)

        // bullet 2 - Given new trace from new environment (Env2) arrived.
        // mock new recentActivities from env 2 in analyticsProvider
        // wait for time event or call recentActivityService.fetchRecentActivities()

        // Then I should see new activities from Env2 in the recent activity view.
        // assert that the new recentActivities are from env2
        // use browser spy to verify recent activities from env2

        // When I click on the link (span_endpoint1) in the recent activity
        // Then the selected environment in the main panel should be changed from Env1 to Env2
        // and I should see that the scope has been changed to span_endpoint1 and 
        // span_endpoint1 insights (all insights**) are visible

        // call processRecentActivityGoToSpan~~~~
        // check the spy --> should push some json to the browser - should see that env2 is in the json

        // check that the environment is changed to env2
        // either use the environment changed event or do to analyticsService.environment.getCurrent()

        // test that the environment is changed to env2

        //wait for 500 ms

        // bullet 4 -
        // When I click on span_2 link on insight1
        // Then I should see the scoped changed to span_2 and insights of span_2 are shown.

        // call processInsightGoToSpan~~~~ --> to go the  span that is related to the insight1
        // check the spy --> should push some json to the browser - should see that span_2 is in the json

        // bullet 5 - 
        // When I click target icon (go to code)
        // Then I should be navigated to document#2 and the cursor should be navigated to the span location.

        // call processRecentActivityGoToSpanRequest
        // check that the document is changed to document2 in the editor
        // check that caret is on the span in document2


    }

    // irrelevant - insights are about to move away from the documentInfoService
    private fun assertInsightsForDocument(
        insightMap: MutableMap<String, MutableList<CodeObjectInsight>>,
        expectedMethodInsights: InsightsOfMethodsResponse,
    ) {
        insightMap.toList().forEachIndexed { index, it ->
            it.also { (methodId: String, insights: MutableList<CodeObjectInsight>) ->
                insights.forEachIndexed { indexed, insight ->
                    TestCase.assertEquals(methodId, insight.codeObjectId)
                    TestCase.assertEquals(expectedMethodInsights.methodsWithInsights[index].insights[indexed], insights[indexed])
                }
            }
        }
    }

    private fun getCodeObjectIdsFromDocumentContainer(infoContainer: DocumentInfoContainer): List<String> {
        val methodRef = infoContainer.javaClass.getMethod("getObjectIdsForCurrentDocument")

        @Suppress("UNCHECKED_CAST")
        val methodInfos: List<String> = methodRef.invoke(infoContainer) as List<String>

        return methodInfos
    }

    private fun getMethodInfos(infoContainer: DocumentInfoContainer): Map<String, MethodInfo> {
        return infoContainer.documentInfo.methods.toMap()

    }

    private fun collectMethodsInFile(file: PsiFile): List<PsiMethod> {
        val methodList = mutableListOf<PsiMethod>()
        file.accept(object : JavaRecursiveElementVisitor() {
            override fun visitMethod(method: PsiMethod) {
                methodList.add(method)
            }
        })
        return methodList
    }
}