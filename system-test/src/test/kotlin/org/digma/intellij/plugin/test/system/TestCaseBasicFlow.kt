package org.digma.intellij.plugin.test.system

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import junit.framework.TestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult
import org.digma.intellij.plugin.recentactivity.RecentActivityService
import org.digma.intellij.plugin.test.system.framework.WaitFinishRule
import org.digma.intellij.plugin.test.system.framework.environmentList
import org.digma.intellij.plugin.test.system.framework.expectedInsightsOfMethodsResponseEnv1
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider
import org.junit.Rule

class TestCaseBasicFlow : LightJavaCodeInsightFixtureTestCase() {

    private val document1Name: String = "EditorEventsHandler.java"
    private val document2Name: String = "EditorEventsHandler2.java"


    private val logger = Logger.getInstance(TestCaseBasicFlow::class.java)

    @get:Rule
    val done = WaitFinishRule()

    private lateinit var messageBusTestListeners: MessageBusTestListeners

    private val analyticsService: AnalyticsService
        get() = AnalyticsService.getInstance(project)

    private val documentInfoService: DocumentInfoService
        get() = DocumentInfoService.getInstance(project)

    override fun setUp() {
        super.setUp()
        done.complete = false
        mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
    }

    override fun tearDown() {

        //waiting for the test to complete
        while (true) {
            if (done.complete) {
                done()
                break
            } else {
                done.signalComplete()
            }
        }

        // 
        try {
            super.tearDown()
        } catch (e: Exception) {
            Log.test(logger::error, "Exception in tearDown {}", e.message)
        }
    }

    override fun getTestDataPath(): String {
        return "src/test/resources"
    }

    fun testInsightAndActivityFlow() {

        var expectedEnv = environmentList[0]
        var actualEnv: String?
        var currentDocumentName = ""
        var expectedDocumentName = document1Name
        // subscribe to env changed event
        messageBusTestListeners.registerSubToEnvironmentChangedEvent { env, _ ->
            actualEnv = env
            assertEquals(expectedEnv, actualEnv)
        }
        // subscribe to documentInfoChanged event
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent { psiFile ->
            currentDocumentName = psiFile.name
            assertEquals(expectedDocumentName, currentDocumentName)
        }

        // open document1
        expectedDocumentName = document1Name
        val document1File = myFixture.configureByFile(document1Name)
        myFixture.openFileInEditor(document1File.virtualFile)

        //wait for 500 ms
        runBlocking { delay(500) }


        // navigate to method --> get the method codeObjectId
        val methodList = collectMethodsInFile(document1File)
        val destinationMethod = methodList.find { it.name == "isRelevantFile" }.let { it!! }
        editor.caretModel.moveToOffset(destinationMethod.textOffset)

        // verify that method under caret is the same as the destinationMethod


        val document1infoContainer = documentInfoService.getDocumentInfo(document1File)
        if (document1infoContainer == null) {
            TestCase.fail("document1infoContainer is null")
            return
        }

        // get insights of current file from document Info service
        val insightMap: MutableMap<String, MutableList<CodeObjectInsight>> = document1infoContainer.allMethodWithInsightsMapForCurrentDocument
        // check if the insights are correct that correlates to the current env and the codeObjectId of the method
        assertInsightsForDocument(insightMap, expectedInsightsOfMethodsResponseEnv1)

        val codeObjectOfDocument1: List<String> = getCodeObjectIdsFromDocumentContainer(document1infoContainer)
        val methodInfosOfDocument1: Map<String, MethodInfo> = getMethodInfos(document1infoContainer)
        // check that the number of methods in the documentInfoService is the same as the number of methods in the file
        TestCase.assertEquals(methodList.size, methodInfosOfDocument1.size)


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