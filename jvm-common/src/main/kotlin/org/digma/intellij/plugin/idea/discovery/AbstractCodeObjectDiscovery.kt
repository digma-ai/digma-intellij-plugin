package org.digma.intellij.plugin.idea.discovery

import com.intellij.lang.Language
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.ensureActive
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.suspendableRetry
import org.digma.intellij.plugin.idea.JvmLanguageService
import org.digma.intellij.plugin.idea.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.span.AbstractSpanDiscovery
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.LanguageServiceProvider
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import kotlin.coroutines.coroutineContext

/**
 * code object discovery for jvm languages
 */
abstract class AbstractCodeObjectDiscovery(private val spanDiscovery: AbstractSpanDiscovery) {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    /*
        Note about debugging:
        because of the way this method is implemented, see bellow, many code blocks are not run in read access,
        but there are visible variables like UMethod or UClass, when stopping in breakpoints intellij debugger
        will try to show the toString of these variables and that will throw exceptions because the toString of many
        classes needs read access.
        To avoid it, configure the intellij debugger:
        disable 'Enable toString object view'
        disable 'show values inline'
     */

    /*
        Please read before changing buildDocumentInfo.

        buildDocumentInfo is the main flow for code object discovery for a file. it starts here and calls
        AbstractSpanDiscovery,EndpointDiscovery and potentially other utilities.

        Ideally it is easy to call this method in read action in smart mode. but after we noticed many errors
        like IndexNotReadyException we decided to implement differently.
        another thing is that it may be a long operation and holding read access for a long time is not advisable.
        so,
        buildDocumentInfo must be called on background thread and not in read access.
        the code makes the best effort to collect as many code objects as possible without failing,skipping some if there
        are exceptions.
        every code block or line that needs read access should start a read access locally and release the read lock as
        quickly as possible.
        every code block or line that needs to wait for smart mode should do it locally.
        that way we don't hold read lock for long time. and we minimize the change for IndexNotReadyException.
        IndexNotReadyException is bound to happen here and there and the code should know how to handle it.
        code blocks that may throw exceptions should catch it and skip the operation. for example if iterating over
        a list of methods to collect code objects and an operation on one method throws exception the exception is caught
        and the method is skipped. that way we may be missing one code object but the whole operation completes.

        also notice that inner operations may throw exception and should be skipped without failing the whole operation.
        see for example AbstractSpanDiscovery.discoverWithSpanAnnotationSpans:
        if WithSpan class can not be found discoverWithSpanAnnotationSpans will throw exception, the exception is caught
        and the operation continues to the next discovery.

        //todo: in case some code objects are missed because of exceptions we will never try again.
        // one option is to mark the DocumentInfo as incomplete ,check that in the caller and retry.
        // although we have a lot of retries along the way and this may be just more optimization, it may help
        // in case indexing started while the operation is running and takes long time, the only option is to retry.
        // we should monitor errors and decide based on the amount and types of errors.

     */

    /**
     * Builds a DocumentInfo.
     * Assumes the psi file is valid so validate before calling.
     * Assumes no read access, so don't call this method in read access
     */
    open suspend fun buildDocumentInfo(project: Project, psiFile: PsiFile, fileUrl: String, language: Language): DocumentInfo? {
        try {
            //don't call this method on EDT or in read access, read access is taken when needed
            EDT.assertNonDispatchThread()
            ReadActions.assertNotInReadAccess()
            DumbService.getInstance(project).waitForSmartMode()
            //maybe uFile is null,there is nothing to do without a UFile.
            val fileData = readAction {
                FileData.buildFileData(psiFile)
            } ?: return null
            coroutineContext.ensureActive()

            val packageName = fileData.packageName
            val methodInfoMap = mutableMapOf<String, MethodInfo>()
            val spans: Collection<SpanInfo> = collectSpans(project, psiFile)
            coroutineContext.ensureActive()
            val classes = readAction { fileData.uFile.classes }
            collectMethods(project, fileUrl, classes, packageName, methodInfoMap, spans)
            coroutineContext.ensureActive()
            val documentInfo = DocumentInfo(fileUrl, methodInfoMap, language.id)
            collectEndpoints(project, psiFile, documentInfo)
            coroutineContext.ensureActive()
            return documentInfo
        } catch (e: Throwable) {
            //may also be CancellationException
            Log.warnWithException(logger, project, e, "error building document info for file {}", fileUrl)
            throw e
        }
    }


    open suspend fun collectEndpoints(project: Project, psiFile: PsiFile, documentInfo: DocumentInfo) {
        val endpointDiscoveryList = getEndpointDiscoveryForLanguage(project, psiFile)
        for (framework in endpointDiscoveryList) {
            coroutineContext.ensureActive()
            suspendableRetry {
                framework.endpointDiscovery(psiFile, documentInfo)
            }
        }
    }


    private fun getEndpointDiscoveryForLanguage(project: Project, psiFile: PsiFile): Collection<EndpointDiscovery> {
        return LanguageServiceProvider.getInstance(project).getLanguageService(psiFile.language)?.takeIf { it is JvmLanguageService }?.let {
            (it as JvmLanguageService).getEndpointFrameworks(project)
        } ?: emptyList()
    }


    open suspend fun collectSpans(project: Project, psiFile: PsiFile): Collection<SpanInfo> {
        return suspendableRetry {
            coroutineContext.ensureActive()
            spanDiscovery.discoverSpans(project, psiFile)
        }
    }


    open suspend fun collectMethods(
        project: Project,
        fileUri: String,
        classes: List<UClass>,
        packageName: String,
        methodInfoMap: MutableMap<String, MethodInfo>,
        spans: Collection<SpanInfo>
    ) {
        classes.forEach { uClass ->
            coroutineContext.ensureActive()
            if (isRelevantClassType(uClass)) {
                val methods: Collection<UMethod> = readAction {
                    getMethodsInClass(uClass)
                }
                methods.forEach { uMethod ->
                    suspendableRetry {
                        coroutineContext.ensureActive()
                        readAction {
                            val id: String = createMethodCodeObjectId(uMethod)
                            val name: String = uMethod.name
                            val containingClassName: String = uClass.qualifiedName ?: getClassSimpleName(uClass)
                            val containingFileUri: String = fileUri
                            val methodInfo = MethodInfo(id, name, containingClassName, packageName, containingFileUri)
                            val methodSpans = spans.filter { spanInfo: SpanInfo -> spanInfo.containingMethodId == id }
                            methodInfo.addSpans(methodSpans)
                            methodInfoMap[id] = methodInfo
                        }
                    }
                }
                val innerClasses = readAction {
                    uClass.innerClasses.asList()
                }
                if (innerClasses.isNotEmpty()) {
                    collectMethods(project, fileUri, innerClasses, packageName, methodInfoMap, spans)
                }
            }
        }
    }


    open suspend fun isRelevantClassType(uClass: UClass): Boolean {
        return readAction {
            !(uClass.isAnnotationType || uClass.isEnum || uClass.isRecord)
        }
    }
}


private class FileData(val uFile: UFile, val packageName: String) {
    companion object {
        fun buildFileData(psiFile: PsiFile): FileData? {
            val uFile: UFile? = psiFile.toUElementOfType<UFile>()
            return uFile?.let {
                val packageName = it.packageName
                FileData(it, packageName)
            }
        }
    }
}
