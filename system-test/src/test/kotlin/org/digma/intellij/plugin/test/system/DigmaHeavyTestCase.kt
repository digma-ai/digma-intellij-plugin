package org.digma.intellij.plugin.test.system

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.document.DocumentInfoService
import org.digma.intellij.plugin.editor.CurrentContextUpdater
import org.digma.intellij.plugin.idea.psi.java.JavaCodeLensService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.test.system.framework.MessageBusTestListeners
import org.digma.intellij.plugin.test.system.framework.mockRestAnalyticsProvider


open class DigmaHeavyTestCase : HeavyPlatformTestCase() {

    protected val logger = Logger.getInstance(DigmaHeavyTestCase::class.java)

    protected lateinit var messageBusTestListeners: MessageBusTestListeners

    protected val analyticsService: AnalyticsService
        get() {
            return AnalyticsService.getInstance(project)
        }

    val systemTestDataPath = "src/test/resources"

    private lateinit var myFixture: CodeInsightTestFixture

    override fun setUp() {
        super.setUp()

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createFixtureBuilder(name, true)
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixtureBuilder.fixture)
        myFixture.testDataPath = systemTestDataPath
        myFixture.setUp()

        mockRestAnalyticsProvider(project)
        messageBusTestListeners = MessageBusTestListeners(project.messageBus)
    }

    override fun tearDown() {
        Log.test(logger::info, "tearDown started")
        JavaCodeLensService.getInstance(project).dispose()
        try {
            super.tearDown()
        } catch (ex: Exception) {
            Log.test(logger::warn, "PluginException in tearDown")
        }
        Log.test(logger::info, "tearDown completed")
    }

    fun `test empty`() {
        assertTrue(true)
    }

    fun waitForDocumentInfoToLoad(file: PsiFile) {
        runBlocking {
            while (DocumentInfoService.getInstance(project).getDocumentInfo(file.virtualFile) == null) {
                delay(100L)
            }
        }
    }

    fun waitForEvent () {
        val timeMillis: Long = 2000
        Log.test(logger::info, "delay $timeMillis millis for file event")
        runBlocking {
            delay(timeMillis)
        }
    }

    fun `test that method is selected according to caret position`() {
        val psiFile = myFixture.configureByFile("TestFile.java")
        myFixture.openFileInEditor(psiFile.virtualFile)
        val editor = myFixture.editor

        val methods = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java)
        val targetMethod = methods.find { it.name == "method1" }
        targetMethod?.let {
            val offset = targetMethod.textOffset
            editor.caretModel.moveToOffset(offset)

            val manager = FileDocumentManager.getInstance()
            val document = manager.getDocument(psiFile.virtualFile)
            document?.let {
                val caretAndSelectionState = EditorTestUtil.extractCaretAndSelectionMarkers(document)
                EditorTestUtil.setCaretsAndSelection(editor, caretAndSelectionState)
            }
        }

        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

        Log.test(logger::info, "selected method = ${project.getService(CurrentContextUpdater::class.java).latestMethodUnderCaret}")

    }

}
