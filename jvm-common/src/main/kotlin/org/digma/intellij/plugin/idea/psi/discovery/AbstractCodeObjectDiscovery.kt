package org.digma.intellij.plugin.idea.psi.discovery

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscoveryService
import org.digma.intellij.plugin.idea.psi.discovery.span.AbstractSpanDiscovery
import org.digma.intellij.plugin.idea.psi.getClassSimpleName
import org.digma.intellij.plugin.idea.psi.getMethodsInClass
import org.digma.intellij.plugin.idea.psi.runInReadAccessInSmartModeAndRetry
import org.digma.intellij.plugin.idea.psi.runInReadAccessInSmartModeWithResultAndRetry
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
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
        Note about buildDocumentInfo:
        buildDocumentInfo should be called on background thread with read action and in smart mode.
        callers are responsible for that.
        this method may throw several kind of exceptions, especially IndexNotReadyException.
        IndexNotReadyException may be thrown if the IDE goes into dumb mode while this method executes.
        callers should handle the exception and possibly retry.
        there are few issues with this method:
        it holds a read lock for long time, it means that write lock access will wait for it to complete
        which is not the best.
        and if an exception is thrown the whole method will exit, and possibly retried by caller.

        another option to implement this method is not to call it in read access, and acquire read access for
        every line of code or blocks of code that needs it. also wait for smart more for each of these code
        blocks. this way the read access time will be shorter and the chance for IndexNotReadyException is reduced.
        another this is not to skip the whole method in case of exceptions but try as much as possible to collect
        data, catch exceptions in every block of code that hold read access.
        this may be a better approach but will make the code very verbose.

        TODO: see the second option above of implementing this method, monitor errors in posthog and decide in the future
         if we want to implement the second option.

        TODO: errors in span discovery and frameworks discovery are caught and the operation continues so at least we have
         a DocumentInfo with some discovery. a disadvantage is that the exceptions will not be caught in the caller
         ,for example AbstractJvmLanguageService.buildDocumentInfo, and there will not be a complete retry of the operation.
         pne way to solve it is to mark the document as incomplete and check that in the caller and retry a few times to get
         a complete DocumentInfo.
     */

    open fun buildDocumentInfo(project: Project, psiFile: PsiFile): DocumentInfo {

        if (project.isDisposed || !psiFile.isValid) {
            return DocumentInfo(PsiUtils.psiFileToUri(psiFile), mutableMapOf())
        }

        EDT.assertNonDispatchThread()

        try {

            val fileUri = PsiUtils.psiFileToUri(psiFile)

            val uFile: UFile = psiFile.toUElementOfType<UFile>() ?: return DocumentInfo(fileUri, mutableMapOf())

            val packageName = uFile.packageName

            val classes = uFile.classes

            val methodInfoMap = mutableMapOf<String, MethodInfo>()

            //all spans in file.
            //if span discovery fails catch it and continue execution so at least we have a partial document info.
            val spans: Collection<SpanInfo> =
                try {
                    runInReadAccessInSmartModeWithResultAndRetry(project) {
                        spanDiscovery.discoverSpans(project, psiFile)
                    }
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error in span discovery {}", e)
                    ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.buildDocumentInfo.spanDiscovery", e)
                    listOf()
                }


            collectMethods(project, fileUri, classes, packageName, methodInfoMap, spans)

            val documentInfo = DocumentInfo(fileUri, methodInfoMap)

            val endpointDiscoveryList = EndpointDiscoveryService.getInstance(project).getEndpointDiscoveryForLanguage(psiFile)
            endpointDiscoveryList.forEach(Consumer { it: EndpointDiscovery ->
                //if a framework fails catch it and continue execution so at least we have a partial document info.
                try {
                    runInReadAccessInSmartModeAndRetry(project) {
                        it.endpointDiscovery(
                            psiFile,
                            documentInfo
                        )
                    }
                } catch (e: Throwable) {
                    Log.warnWithException(logger, project, e, "error in framework {},{}", it.javaClass.simpleName, e)
                    ErrorReporter.getInstance().reportError("${this::class.java.simpleName}.buildDocumentInfo.${it.javaClass.simpleName}", e)
                }
            })

            return documentInfo
        } catch (e: Throwable) {
            Log.warnWithException(logger, project, e, "error in buildDocumentInfo {}", e)
            throw e
        }
    }

    //todo: fix inner classes
    private fun collectMethods(
        project: Project,
        fileUri: String,
        classes: List<UClass>,
        packageName: String,
        methodInfoMap: MutableMap<String, MethodInfo>,
        spans: Collection<SpanInfo>,
    ) {

        classes.forEach { uClass ->
            if (isRelevantClassType(uClass)) {

                val methods: Collection<UMethod> = getMethodsInClass(project, uClass)

                methods.forEach { uMethod ->
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

                collectMethods(project, fileUri, uClass.innerClasses.asList(), packageName, methodInfoMap, spans)

            }
        }
    }


    private fun isRelevantClassType(uClass: UClass): Boolean {
        return !(uClass.isAnnotationType || uClass.isEnum || uClass.isRecord)
    }


}