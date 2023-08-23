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

class TestCaseBasicFlow : LightJavaCodeInsightFixtureTestCase(), Disposable {

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
        messageBusTestListeners = MessageBusTestListeners(project.messageBus, this)
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

    override fun dispose() {
        // do something then the class is disposed
        messageBusTestListeners.disconnectAll()
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
            TestCase.assertEquals(expectedEnv, actualEnv)
        }
        // subscribe to documentInfoChanged event
        messageBusTestListeners.registerSubToDocumentInfoChangedEvent { psiFile ->
            currentDocumentName = psiFile.name
            TestCase.assertEquals(expectedDocumentName, currentDocumentName)
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

        // verify that method under cerat is the same as the destinationMethod


        val document1infoContainer = documentInfoService.getDocumentInfo(document1File)
        if (document1infoContainer == null) {
            TestCase.fail("document1infoContainer is null")
            return
        }
        val codeObjectOfDocument1: List<String> = getCodeObjectIdsFromDocumentContainer(document1infoContainer)
        val methodInfosOfDocument1: Map<String, MethodInfo> = getMethodInfos(document1infoContainer)
        // check that the number of methods in the documentInfoService is the same as the number of methods in the file
        TestCase.assertEquals(methodList.size, methodInfosOfDocument1.size)


        // check what is the current env --> should be env1
        actualEnv = analyticsService.environment.getCurrent()
        TestCase.assertEquals(expectedEnv, actualEnv)


        // get insights of current file from document Info service
        val insightMap: MutableMap<String, MutableList<CodeObjectInsight>> = document1infoContainer.allMethodWithInsightsMapForCurrentDocument

        // check if the insights are correct that correlates to the current env and the codeObjectId of the method
        assertInsightsForDocument(insightMap, expectedInsightsOfMethodsResponseEnv1)

        // see that recentActivities are present from env1
        val recentActivityService = project.getService(RecentActivityService::class.java)
        val latestActivityProperty = recentActivityService.javaClass.getDeclaredField("latestActivityResult")
        latestActivityProperty.isAccessible = true
        val latestActivityResult = latestActivityProperty.get(recentActivityService) as RecentActivityResult
        TestCase.assertEquals(expectedEnv, latestActivityResult.entries[0].environment)


        //push new recentActivities from env 2 --> don't know how to do so

        //click on the recentActivity from env2 --> span1
        val javaCodeLens = JavaCodeLensService.getInstance(project)
        val codeLens = javaCodeLens.getCodeLens(document1File)
        

        // test that the environment is changed to env2

        //wait for 500 ms

        //The new Insights should be loaded from env2 in the documentInfoService

        // check that the insights are correct that correlates to the current env and the codeObjectId of the method

        // click second span from env2 that will open insights.

        // click on span and navigate to the method in editor on document2

        // verify that the focused document is document2 and the caret is on the method the span is related to


        // get insights of current file from document Info service for env2


        // check if the insights are correct that correlates to the current env


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