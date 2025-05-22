package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.editors.getProjectModelId
import com.jetbrains.rider.projectView.solution
import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.digma.intellij.plugin.codelens.provider.CodeLensChanged
import org.digma.intellij.plugin.codelens.provider.CodeLensProvider
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer
import kotlin.coroutines.coroutineContext

//don't make it light service because it will register on all IDEs, but we want it only on Rider
@Suppress("LightServiceMigrationCode")
class CodeLensHost(project: Project, private val cs: CoroutineScope) : LifetimedProjectComponent(project) {

    private val logger = Logger.getInstance(CodeLensHost::class.java)
    private val runningJobs: ConcurrentMap<VirtualFile, Job> = ConcurrentMap()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CodeLensHost {
            return project.service<CodeLensHost>()
        }
    }

    //must be a registered listener to get events early and not depend on initialization of the CodeLensHost service.
    class CodeLensHostCodelensListener(private val project: Project) : CodeLensChanged {
        override fun codelensChanged(virtualFile: VirtualFile) {
            getInstance(project).installCodeLens(virtualFile)
        }

        override fun codelensRemoved(virtualFile: VirtualFile) {
            getInstance(project).removeCodelens(virtualFile)
        }
    }

    override fun dispose() {
        runningJobs.values.forEach { it.cancel(CancellationException("CodeLensHost dispose")) }
        runningJobs.clear()
        super.dispose()
    }

    private fun installCodeLens(virtualFile: VirtualFile) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "starting installCodeLens job for {}", virtualFile)
        }
        runningJobs[virtualFile]?.cancel(CancellationException("new installCodeLens job started"))
        val job = cs.launchWithErrorReporting("CodeLensHost.installCodeLens", logger) {
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "Refreshing code lens for {}", virtualFile)
            }
            val codeLens: Set<CodeLens> = CodeLensProvider.getInstance(project).getCodeLens(virtualFile)
            coroutineContext.ensureActive()
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "Got codeLens for {}: {}", virtualFile, codeLens)
            }
            installCodeLens(virtualFile, codeLens)
        }
        runningJobs[virtualFile] = job
        job.invokeOnCompletion { cause ->
            runningJobs.remove(virtualFile)
        }
    }


    private fun removeCodelens(virtualFile: VirtualFile) {
        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "starting removeCodelens job for {}", virtualFile)
        }
        runningJobs[virtualFile]?.cancel(CancellationException("new removeCodelens job started"))
        val job = cs.launchWithErrorReporting("CodeLensHost.removeCodelens", logger) {
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "Removing code lens for {}", virtualFile)
            }
            coroutineContext.ensureActive()
            uninstallCodeLens(virtualFile)
        }
        runningJobs[virtualFile] = job
        job.invokeOnCompletion { cause ->
            runningJobs.remove(virtualFile)
        }
    }

    private suspend fun uninstallCodeLens(@NotNull file: VirtualFile) {

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "uninstalling code lens for {}", file)
        }

        val projectModelId: Int? = withContext(Dispatchers.EDT) {
            val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)
            fileEditor?.takeIf { it is TextEditor }?.let { it as TextEditor }?.editor?.getProjectModelId()
        }
        val fileUrl = file.url
        val psiId = PsiFileID(projectModelId, fileUrl)

        coroutineContext.ensureActive()
        val model = project.solution.codeObjectsModel
        model.protocol?.scheduler?.invokeOrQueue {
            //first remove all code lens entries belonging to this document.
            //the map is not keyed by a document, so we have to search
            val toRemove = model.codeLens.entries
                .filter { (_, value) -> value.lens.any { it.psiUri == fileUrl } }
                .mapTo(mutableSetOf()) { it.key }
            toRemove.forEach {
                model.codeLens.remove(it)
            }
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "Calling reanalyze for {}", psiId)
            }
            model.reanalyze.fire(psiId)
        }
    }


    private suspend fun installCodeLens(@NotNull file: VirtualFile, @NotNull codeLenses: Set<CodeLens>) {

        if (logger.isTraceEnabled) {
            Log.log(logger::trace, "installing code lens for {}: {}", file, codeLenses)
        }

        //Install code lens for a document. This code will also take care of clearing old
        //code lens of this document
        //always try to find ProjectModelId for the psi file. It is the preferred way to find a psi file in resharper

        val projectModelId: Int? = withContext(Dispatchers.EDT) {
            val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file)
            fileEditor?.takeIf { it is TextEditor }?.let { it as TextEditor }?.editor?.getProjectModelId()
        }
        val fileUrl = file.url
        val psiId = PsiFileID(projectModelId, fileUrl)

        coroutineContext.ensureActive()
        val model = project.solution.codeObjectsModel
        model.protocol?.scheduler?.invokeOrQueue {
            //first remove all code lens entries belonging to this document.
            //the map is not keyed by a document, so we have to search
            val toRemove = model.codeLens.entries
                .filter { (_, value) -> value.lens.any { it.psiUri == fileUrl } }
                .mapTo(mutableSetOf()) { it.key }
            toRemove.forEach {
                model.codeLens.remove(it)
            }

            //add code lens to the rider protocol
            codeLenses.forEach(Consumer { codeLens ->
                model.codeLens.computeIfAbsent(codeLens.codeMethod) { LensPerObjectId() }
                model.codeLens[codeLens.codeMethod]?.lens?.add(codeLens.toRiderCodeLensInfo(fileUrl))
            })
            if (logger.isTraceEnabled) {
                Log.log(logger::trace, "Calling reanalyze for {}", psiId)
            }
            model.reanalyze.fire(psiId)
        }
    }


    private fun CodeLens.toRiderCodeLensInfo(psiUri: String) = RiderCodeLensInfo(
        id = id,
        methodCodeObjectId = codeMethod,
        scopeCodeObjectId = scopeCodeObjectId,
        lensTitle = lensTitle,
        lensDescription = lensDescription,
        moreText = lensMoreText,
        psiUri = psiUri
    )
}