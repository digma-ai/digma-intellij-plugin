package org.digma.intellij.plugin.rider.protocol

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.framework.IProtocol
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.projectView.solution
import org.digma.intellij.plugin.codelens.provider.CodeLensProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.lens.CodeLens
import org.jetbrains.annotations.NotNull
import java.util.function.Consumer

//don't make it light service because it will register on all IDEs, but we want it only on Rider
@Suppress("LightServiceMigrationCode")
class CodeLensHost(project: Project) : LifetimedProjectComponent(project){

    private val logger = Logger.getInstance(CodeLensHost::class.java)

    private val codeLensProvider: CodeLensProvider = CodeLensProvider.getInstance(project)



    //always use getInstance instead of injecting directly to other services.
    // this ensures lazy init only when this host is needed.
    // when injecting directly in constructor of other services it needs to load the solution model
    // and that required EDT which is not always a good idea.
    companion object {
        @JvmStatic
        fun getInstance(project: Project): CodeLensHost {
            return project.getService(CodeLensHost::class.java)
        }
    }


    private fun getProtocol(model: CodeObjectsModel): IProtocol {
        return model.protocol!! //protocol is nullable in 2023.2, remove when 2023.2 is our base
    }


    internal fun refreshFile(virtualFile: VirtualFile) {
        try {
            Log.log(logger::debug, "Refreshing code lens for {}", virtualFile)
            val codeLens: Set<CodeLens> = codeLensProvider.getCodeLens(virtualFile)
            Log.log(logger::debug, "Got codeLens for {}: {}", virtualFile, codeLens)
            installCodeLens(virtualFile, codeLens)
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in refresh for {}", virtualFile)
            ErrorReporter.getInstance().reportError(project, "CodeLensHost.refresh", e)
        }
    }

    fun installCodeLens(@NotNull file: VirtualFile, @NotNull codeLenses: Set<CodeLens>) {
        //todo: why on EDT ?
//        EDT.ensureEDT {
            installCodeLensOnEDT(file, codeLenses)
//        }
    }

    private fun installCodeLensOnEDT(@NotNull file: VirtualFile, @NotNull codeLenses: Set<CodeLens>) {

        Log.log(logger::debug, "got request to installCodeLensOnEDT for {}: {}", file, codeLenses)

        //install code lens for a document. this code will also take care of clearing old
        //code lens of this document, necessary in environment change event.

        //always try to find ProjectModelId for the psi file, it is the preferred way to find a psi file in resharper
        val projectModelId: Int? = tryGetProjectModelId(file, project)
        val psiUri = file.url
        val psiId = PsiFileID(projectModelId, psiUri)

        Log.log(logger::debug, "Installing code lens for {}", psiId)

        val model: CodeObjectsModel = project.solution.codeObjectsModel
        getProtocol(model).scheduler.invokeOrQueue {

            //first remove all code lens entries belonging to this document.
            //the map is not keyed by document, so we have to search
            val toRemove = mutableSetOf<String>()
            model.codeLens.forEach { entry ->
                entry.value.lens.forEach { codeLensInfo ->
                    if (codeLensInfo.psiUri == psiUri) {
                        toRemove.add(entry.key)
                    }
                }
            }
            toRemove.forEach {
                model.codeLens.remove(it)
            }


            //add code lens to the rider protocol
            codeLenses.forEach(Consumer { codeLens ->
                model.codeLens.computeIfAbsent(codeLens.codeMethod) { LensPerObjectId() }
                model.codeLens[codeLens.codeMethod]?.lens?.add(codeLens.toRiderCodeLensInfo(psiUri))
            })

            Log.log(logger::debug, "Calling reanalyze for {}", psiId)
            model.reanalyze.fire(psiId)
        }
    }


    private fun CodeLens.toRiderCodeLensInfo(psiUri: String) = RiderCodeLensInfo(
        id = id,
        codeObjectId = codeMethod,
        scopeCodeObjectId = scopeCodeObjectId,
        lensTitle = lensTitle,
        lensDescription = lensDescription,
        moreText = lensMoreText,
        psiUri = psiUri
    )


}