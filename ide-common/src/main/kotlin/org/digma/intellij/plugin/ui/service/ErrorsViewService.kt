package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.document.CodeObjectsUtil
import org.digma.intellij.plugin.errors.ErrorsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.navigation.ErrorsDetailsHelper
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.psi.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsTabCard
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

@Service(Service.Level.PROJECT)
class ErrorsViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(ErrorsViewService::class.java)
    private val lock: ReentrantLock = ReentrantLock()

    //the model is single per the life of an open project in intellij. it shouldn't be created
    //elsewhere in the program. it can not be singleton.
    val model = ErrorsModel()


    companion object {
        @JvmStatic
        fun getInstance(project: Project): ErrorsViewService {
            return project.getService(ErrorsViewService::class.java)
        }
    }


    override fun getViewDisplayName(): String {
        return "Errors" + if (model.errorsCount > 0) " (${model.count()})" else ""
    }


    fun updateErrors(methodId: String) {
        val methodInfo = tryFindMethodInfo(methodId)

        updateErrorsModel(methodInfo)
    }


    private fun updateErrorsModel(methodInfo: MethodInfo) {
        lock.lock()
        try {
            updateErrorsModelImpl(methodInfo)
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }


    private fun updateErrorsModelImpl(methodInfo: MethodInfo) {

        val errorsProvider: ErrorsProvider = project.getService(ErrorsProvider::class.java)
        updateErrorsModelWithErrorsProvider(methodInfo, errorsProvider)
    }

    private fun updateErrorsModelWithErrorsProvider(methodInfo: MethodInfo, errorsProvider: ErrorsProvider) {
        lock.lock()
        Log.log(logger::debug, "Lock acquired for contextChanged to {}. ", methodInfo)
        try {
            Log.log(logger::debug, "contextChanged to {}. ", methodInfo)

            val errorsListContainer = errorsProvider.getErrors(methodInfo)

            model.listViewItems = errorsListContainer.listViewItems ?: listOf()
            model.previewListViewItems = ArrayList()
            model.scope = MethodScope(methodInfo)
            model.card = ErrorsTabCard.ERRORS_LIST
            model.errorsCount = errorsListContainer.count

            updateUi()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                Log.log(logger::debug, "Lock released for contextChanged to {}. ", methodInfo)
            }
        }
    }

    fun showErrorDetails(uid: String) {

        Log.log(logger::debug, "showDocumentPreviewList for {}. ", uid)
        val errorsProvider = project.service<ErrorsProvider>()
        val errorDetails = errorsProvider.getErrorDetails(uid)
        errorDetails.flowStacks.isWorkspaceOnly = PersistenceService.getInstance().isWorkspaceOnly()
        model.errorDetails = errorDetails
        model.card = ErrorsTabCard.ERROR_DETAILS

        updateUi()

    }


    override fun canUpdateUI(): Boolean {
        return !project.service<ErrorsDetailsHelper>().isErrorDetailsOn()
    }

    fun closeErrorDetails() {

        Log.log(logger::debug, "closeErrorDetails called")

        model.errorDetails = createEmptyErrorDetails()
        model.card = ErrorsTabCard.ERRORS_LIST
        updateUi()
    }


    /**
     * empty should be called only when there is no file opened in the editor and not in
     * any other case.
     */
    fun empty() {

        Log.log(logger::debug, "empty called")

        model.listViewItems = Collections.emptyList()
        model.previewListViewItems = ArrayList()
        model.scope = EmptyScope("")
        model.card = ErrorsTabCard.ERRORS_LIST
        model.errorsCount = 0

        updateUi()
    }


    private fun createEmptyErrorDetails(): ErrorDetailsModel {
        val emptyErrorDetails = ErrorDetailsModel()
        emptyErrorDetails.flowStacks.isWorkspaceOnly = PersistenceService.getInstance().isWorkspaceOnly()
        return emptyErrorDetails
    }


    //todo: best effort to find MethodInfo. not the best way to do it.
    // need to implement language service methods to do method discovery from methodCodeObjectId
    private fun tryFindMethodInfo(methodCodeObjectId: String): MethodInfo {

        val methodClassAndName: Pair<String, String> = CodeObjectsUtil.getMethodClassAndName(methodCodeObjectId)
        val defaultResult = MethodInfo(methodCodeObjectId, methodClassAndName.second, methodClassAndName.first, "", "", 0)

        return try {
            val languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId)
            val workspaceUris = languageService.findWorkspaceUrisForMethodCodeObjectIds(listOf(methodCodeObjectId))
            val fileUri = workspaceUris[methodCodeObjectId]?.first
            if (fileUri != null) {
                val psiFile = PsiUtils.uriToPsiFile(fileUri, project)
                if (PsiUtils.isValidPsiFile(psiFile)) {
                    BuildDocumentInfoProcessContext.buildDocumentInfoUnderProcessOnCurrentThreadNoRetry { pi ->
                        val context = BuildDocumentInfoProcessContext(pi)
                        val documentInfo = languageService.buildDocumentInfo(psiFile, context)
                        documentInfo.methods[methodCodeObjectId] ?: defaultResult
                    }

                } else {
                    return defaultResult
                }
            } else {
                return defaultResult
            }
        } catch (e: Throwable) {
            defaultResult
        }
    }

}