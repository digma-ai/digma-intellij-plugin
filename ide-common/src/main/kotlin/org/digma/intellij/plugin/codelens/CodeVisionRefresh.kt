package org.digma.intellij.plugin.codelens

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.isValidVirtualFile
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * Handles code vision refresh
 *
 * In 233 the class was changed, but [its signature is the same](https://github.com/JetBrains/intellij-community/blob/e662a9d96d43d27c4a95c146f65ab46864df00fe/platform/lang-impl/src/com/intellij/codeInsight/daemon/impl/InlayHintsPassFactory.kt#L68C19-L68C19)
 * [Commit](https://github.com/JetBrains/intellij-community/commit/48979aef7bf3d5b100053809684a6de5e389858a)
 * @see <a href="https://youtrack.jetbrains.com/issue/IDEA-333164/Breaking-change-with-InlayHintsPassFactory-class-moved-in-another-package">Issue in youtrack</a>
 */
internal class CodeVisionRefresh(private val project: Project) {

    // What is this class:
    // see this discussion:
    //@see <a href="https://youtrack.jetbrains.com/issue/IDEA-333164/Breaking-change-with-InlayHintsPassFactory-class-moved-in-another-package">Issue in youtrack</a>
    //todo: follow this discussion for changes and maybe one day there will be a clear API to force refresh code visions

    //just calling DaemonCodeAnalyzer.restart does not force CodeVisionPass and intellij does not yet provide a clear public
    // API to force refresh of code vision hints.
    //in 2022.3 - 2023.1 it was possible to call CodeVisionPassFactory.clearModificationStamp(editor)
    // before DaemonCodeAnalyzer.restart and that worked.
    //in 2023.2 - 2023.3 CodeVisionPassFactory changed to internal class and is not possible to call it.
    //in 2024.1 they added a public class called ModificationStampUtil which does again have a method clearModificationStamp(editor)
    //So the call to clearModificationStamp is different between intellij releases.
    //this class uses reflection to call the correct API.

    //todo: if there is ever a public clear API to force code vision pass then change this class.
    //todo: the reflection may break at some point. check error in posthog CodeVisionRefresh.clearModificationStamp,
    // it will notify when this reflection is not working anymore.


    private val logger: Logger = Logger.getInstance(CodeVisionRefresh::class.java)


    /**
     * Locate the correct class with clearModificationStamp method.
     * Note this initialization bias by looking first at the latest known class
     * supposing users tend to have the latest version.
     * the variable is initialized on class init.
     */
    private val clearModificationStampMH: Result<MethodHandle> = runCatching {
        // latest builds of 241 have public ModificationStampUtil
        val javaClass = Class.forName("com.intellij.codeInsight.hints.codeVision.ModificationStampUtil")
        val kClass = javaClass.kotlin
        val modificationsStampUtilInstance = kClass.objectInstance

        val methodHandler = MethodHandles.lookup().unreflect(
            javaClass.getMethod("clearModificationStamp", Editor::class.java)
        ).bindTo(modificationsStampUtilInstance)

        Log.log(logger::trace, "binding clearModificationStamp to ModificationStampUtil.clearModificationStamp")
        methodHandler

    }.recoverCatching { throwable: Throwable ->

        // 223 all builds
        // 231 all builds
        // 232 all builds - CodeVisionPassFactory changed to internal class
        // 233 all builds - CodeVisionPassFactory still internal class
        val codeVisionPassFactoryClass = Class.forName("com.intellij.codeInsight.hints.codeVision.CodeVisionPassFactory")
        val codeVisionPassFactoryCompanionInstance =
            codeVisionPassFactoryClass.getField("Companion").get(null)
        val codeVisionPassFactoryCompanionClass = codeVisionPassFactoryCompanionInstance.javaClass
        val methodHandler = MethodHandles.lookup().unreflect(
            codeVisionPassFactoryCompanionClass.getMethod("clearModificationStamp", Editor::class.java)
        ).bindTo(codeVisionPassFactoryCompanionInstance)

        Log.log(logger::trace, "binding clearModificationStamp to CodeVisionPassFactory.Companion.clearModificationStamp")
        methodHandler
    }.onFailure { throwable: Throwable ->
        ErrorReporter.getInstance().reportError(
            project, "CodeVisionRefresh.clearModificationStampMH",
            throwable,
            mutableMapOf(
                "error message" to "Failed to acquire a method handle to method clearModificationStamp"
            )
        )
    }


    fun refreshForFile(psiFile: PsiFile) {
        try {
            Log.log(logger::trace, "refreshForFile called for file {}", psiFile)
            restartDaemonCodeAnalyzer(project, setOf(psiFile.virtualFile))
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("CodeVisionRefresh.refreshForFile", e)
        }
    }

    fun refreshForFiles(psiFilesUrls: List<String>) {
        try {
            Log.log(logger::trace, "refreshForFiles called for files {}", psiFilesUrls)

            //need to convert the psi urls to files, if something fails just ignore it.
            val files = psiFilesUrls.mapNotNull {
                try {
                    PsiUtils.uriToPsiFile(it, project).virtualFile
                } catch (e: Throwable) {
                    null
                }
            }.toSet()

            restartDaemonCodeAnalyzer(project, files)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("CodeVisionRefresh.refreshForFiles", e)
        }
    }


    fun refreshAll() {
        try {
            Log.log(logger::trace, "refreshAll called")
            restartDaemonCodeAnalyzer(project)
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("CodeVisionRefresh.refreshAll", e)
        }
    }


    /**
     * Properly restart the DCA for this editor.
     *
     * The regular `DaemonCodeAnalyzer.getInstance(project).restart(psiFile)` (or its variant without the file)
     * will not re-run inlay hint providers if the file is not modified.
     * To proceed, the current best approach is to clear the modification
     * stamp on the editor for each file.
     *
     * @param project The project
     * @param virtualFilesFilter Whether to restart the DCA for the editor with this file, if empty all editors are restarted.
     * @see clearModificationStamp
     */
    private fun restartDaemonCodeAnalyzer(project: Project, virtualFilesFilter: Set<VirtualFile> = emptySet()) {
        openedTextEditors(project).forEach { editor ->
            if (virtualFilesFilter.isEmpty() || virtualFilesFilter.contains(editor.file)) {
                editor.file?.takeIf { editor.isValid && isValidVirtualFile(editor.file) }?.let { virtualFile ->
                    Log.log(logger::trace, "calling clearModificationStamp for {}", virtualFile)
                    if (clearModificationStamp(editor.editor)) {
                        Log.log(logger::trace, "clearModificationStamp succeeded for file {}", virtualFile)
                        runReadAction {
                            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                            psiFile?.let {
                                Log.log(logger::trace, "calling DaemonCodeAnalyzer.restart for {}", psiFile)
                                DaemonCodeAnalyzer.getInstance(project).restart(it)
                            }
                        }
                    } else {
                        Log.log(logger::trace, "clearModificationStamp failed, fallback to drop psi caches")
                        //worst case fallback, better not to get here.
                        // this code always works.
                        EDT.ensureEDT {
                            WriteAction.run<RuntimeException> {
                                if (!project.isDisposed) {
                                    val psiFile = PsiManager.getInstance(project).findFile(editor.file)
                                    psiFile?.takeIf { editor.isValid && PsiUtils.isValidPsiFile(psiFile) }?.let {
                                        val manager = PsiManager.getInstance(project)
                                        manager.dropPsiCaches()
                                        manager.dropResolveCaches()
                                        DaemonCodeAnalyzer.getInstance(project).restart(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openedTextEditors(project: Project): List<TextEditor> {
        return FileEditorManager.getInstance(project).allEditors.filterIsInstance<TextEditor>().toList()
    }


    private fun clearModificationStamp(editor: Editor): Boolean {
        return clearModificationStampMH.getOrNull()?.let { methodHandler ->
            methodHandler.invokeWithArguments(editor)
            true
        } ?: false
    }

//    private fun clearModificationStamp(project: Project, editor: Editor) {
//        clearModificationStampMH.onFailure { exception: Throwable ->
//
//            ErrorReporter.getInstance().reportInternalFatalError(
//                project, "CodeVisionRefresh.clearModificationStamp",
//                exception,
//                mutableMapOf(
//                    "error message" to "Failed to acquire a method handle to method clearModificationStamp"
//                )
//            )
//
//            //worst case fallback, better not to get here.
//            // this code always works , calling DaemonCodeAnalyzer.restart after that will trigger a code vision pass.
//            EDT.ensureEDT {
//                WriteAction.run<RuntimeException> {
//                    if (!project.isDisposed) {
//                        val manager = PsiManager.getInstance(project)
//                        manager.dropPsiCaches()
//                        manager.dropResolveCaches()
//                        DaemonCodeAnalyzer.getInstance(project).restart(it)
//                    }
//                }
//            }
//
//        //this won't do anything if there was a failure locating clearModificationStamp method,
//        // exception should be sent to telemetry.
//        }.getOrNull()?.invokeWithArguments(editor)
//    }
//

}