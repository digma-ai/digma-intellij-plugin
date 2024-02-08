package org.digma.intellij.plugin.idea.psi.discovery

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.executeCatching
import org.digma.intellij.plugin.common.executeCatchingWithResult
import org.digma.intellij.plugin.common.executeCatchingWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.common.isProjectValid
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.common.runInReadAccessWithRetryIgnorePCE
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscoveryService
import org.digma.intellij.plugin.idea.psi.discovery.span.AbstractSpanDiscovery
import org.digma.intellij.plugin.idea.psi.getClassSimpleName
import org.digma.intellij.plugin.idea.psi.getMethodsInClass
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.BuildDocumentInfoProcessContext
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import java.util.function.Consumer

/**
 * code object discovery for jvm languages
 */
abstract class AbstractCodeObjectDiscovery(private val spanDiscovery: AbstractSpanDiscovery) {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    /*
        Note about debugging:
        because of the way this method is implemented,see bellow, many code blocks are not run in read access,
        but there are visible variables like UMethod or UClass, when stopping in breakpoints intellij debugger
        will try to show the toString of these variables and that will throw exceptions because the toString of many
        classes needs read access.
        to avoid it configure intellij debugger:
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

    //todo: run under process and use org.digma.intellij.plugin.progress.ProcessContext
    // to track errors, create a ProcessContext class for buildDocumentInfo like BuildDocumentInfoProcessContext
    open fun buildDocumentInfo(project: Project, psiFile: PsiFile, context: BuildDocumentInfoProcessContext): DocumentInfo {

        if (!isProjectValid(project) || !PsiUtils.isValidPsiFile(psiFile)) {
            return DocumentInfo(PsiUtils.psiFileToUri(psiFile), mutableMapOf())
        }

        try {

            //this method should not be already in read access , see comments above
            ReadActions.assertNotInReadAccess()

            //should always run on background
            EDT.assertNonDispatchThread()

            DumbService.getInstance(project).waitForSmartMode()

            //build a file data in read access
            val fileData = runInReadAccessWithResult {
                FileData.buildFileData(psiFile)
            }

            val fileUri = fileData.fileUri

            //maybe uFile is null,there is nothing to do without a UFile.
            fileData.uFile ?: return DocumentInfo(fileUri, mutableMapOf())

            val packageName = fileData.packageName

            val classes = fileData.classes

            val methodInfoMap = mutableMapOf<String, MethodInfo>()

            val spans: Collection<SpanInfo> = collectSpans(project, psiFile, context)

            //if collectMethods throws exception we don't catch it, there is no use for DocumentInfo without methods.
            //the exception will be caught be caller and hopefully retry and succeed.
            collectMethods(project, fileUri, classes, packageName, methodInfoMap, spans, context)

            val documentInfo = DocumentInfo(fileUri, methodInfoMap)

            collectEndpoints(project, psiFile, documentInfo, context)

            return documentInfo

        } catch (e: Throwable) {
            context.addError("buildDocumentInfo", e)
            throw e
        }
    }


    private fun collectEndpoints(
        project: Project,
        psiFile: PsiFile,
        documentInfo: DocumentInfo,
        context: BuildDocumentInfoProcessContext,
    ) {
        val endpointDiscoveryList = EndpointDiscoveryService.getInstance(project).getEndpointDiscoveryForLanguage(psiFile)
        endpointDiscoveryList.forEach(Consumer { framework: EndpointDiscovery ->
            //if a framework fails catch it and continue execution so at least we have a partial document info.
            executeCatchingWithResultAndRetryIgnorePCE({
                framework.endpointDiscovery(psiFile, documentInfo, context)
            }, { e ->
                context.addError("collectEndpoints", e)
            })
        })
    }


    private fun collectSpans(
        project: Project,
        psiFile: PsiFile,
        context: BuildDocumentInfoProcessContext,
    ): Collection<SpanInfo> {
        //if span discovery fails catch it and continue execution so at least we have a partial document info.
        return executeCatchingWithResultAndRetryIgnorePCE({
            spanDiscovery.discoverSpans(project, psiFile, context)
        }, { e ->
            context.addError("collectSpans", e)
            listOf()
        })
    }


    private fun collectMethods(
        project: Project,
        fileUri: String,
        classes: List<UClass>,
        packageName: String,
        methodInfoMap: MutableMap<String, MethodInfo>,
        spans: Collection<SpanInfo>,
        context: BuildDocumentInfoProcessContext,
    ) {

        classes.forEach { uClass ->
            if (isRelevantClassType(uClass)) {

                val methods: Collection<UMethod> = runInReadAccessWithResult {
                    getMethodsInClass(uClass)
                }

                methods.forEach { uMethod ->
                    executeCatching({
                        runInReadAccessWithRetryIgnorePCE {
                            val id: String = createMethodCodeObjectId(uMethod)
                            val name: String = uMethod.name
                            val containingClassName: String = uClass.qualifiedName ?: getClassSimpleName(uClass)
                            val containingFileUri: String = fileUri
                            val offsetAtFileUri: Int = uMethod.sourcePsi?.textOffset ?: 0
                            val methodInfo = MethodInfo(id, name, containingClassName, packageName, containingFileUri, offsetAtFileUri)

                            val methodSpans = spans.filter { spanInfo: SpanInfo -> spanInfo.containingMethodId == id }

                            methodInfo.addSpans(methodSpans)

                            methodInfoMap[id] = methodInfo
                        }
                    }) { e ->
                        context.addError("collectMethods", e)
                    }
                }

                val innerClasses = runInReadAccessWithResult {
                    uClass.innerClasses.asList()
                }

                //optimization: check if we made at least 50% of method discovery, if not throw exception, the operation will restart
                //todo: need to know if its the last attempt and not throw the exception
////                if (methods.isNotEmpty()) {
////                    val percentage = (methodInfoMap.size * 100) / methods.size
////                    if (percentage < 50){
////                        throw CodeObjectDiscoveryException("less the 50% methods discovered.")
////                    }
////                }


                if (innerClasses.isNotEmpty()) {
                    collectMethods(project, fileUri, innerClasses, packageName, methodInfoMap, spans, context)
                }
            }
        }
    }


    private fun isRelevantClassType(uClass: UClass): Boolean {
        return runInReadAccessWithResult {
            !(uClass.isAnnotationType || uClass.isEnum || uClass.isRecord)
        }
    }
}


private class FileData(val fileUri: String, val uFile: UFile?, val packageName: String, val classes: List<UClass>) {

    constructor() : this("", null, "", listOf())
    constructor(fileUri: String) : this(fileUri, null, "", listOf())

    companion object {
        fun buildFileData(psiFile: PsiFile): FileData {
            return executeCatchingWithResult({
                val fileUri = PsiUtils.psiFileToUri(psiFile)
                //usually it will return a UFile but must consider null
                val uFile: UFile? = psiFile.toUElementOfType<UFile>()
                uFile?.let {
                    val packageName = it.packageName
                    val classes = it.classes
                    FileData(fileUri, it, packageName, classes)
                } ?: FileData(fileUri)
            }, {
                FileData()
            })
        }
    }

}
