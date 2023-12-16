package org.digma.intellij.plugin.idea.psi.discovery

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscoveryService
import org.digma.intellij.plugin.idea.psi.discovery.span.AbstractSpanDiscovery
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


    open fun buildDocumentInfo(project: Project, psiFile: PsiFile): DocumentInfo {

        val fileUri = PsiUtils.psiFileToUri(psiFile)

        val uFile: UFile = psiFile.toUElementOfType<UFile>() ?: return DocumentInfo(fileUri, mutableMapOf())

        val packageName = uFile.packageName

        val classes = uFile.classes

        val methodInfoMap = mutableMapOf<String, MethodInfo>()

        //all spans in file
        val spans: Collection<SpanInfo> = spanDiscovery.discoverSpans(project, psiFile)

        collectMethods(project, fileUri, classes, packageName, methodInfoMap, spans)

        val documentInfo = DocumentInfo(fileUri, methodInfoMap)


        EndpointDiscoveryService.getInstance(project)
            .endpointDiscoveryList.forEach(Consumer { it: EndpointDiscovery ->
                it.endpointDiscovery(
                    psiFile,
                    documentInfo
                )
            })


        return documentInfo
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

                val methods: Array<UMethod> = getMethodsOf(project, uClass)

                methods.forEach { uMethod ->
                    val id: String = createMethodCodeObjectId(uMethod)
                    val name: String = uMethod.name
                    val containingClassName: String = uClass.qualifiedName ?: uClass.name ?: ""
                    val containingNamespace = packageName
                    val containingFileUri: String = fileUri
                    val offsetAtFileUri: Int = uMethod.sourcePsi?.textOffset ?: 0
                    val methodInfo = MethodInfo(id, name, containingClassName, containingNamespace, containingFileUri, offsetAtFileUri)

                    val methodSpans = spans.filter { spanInfo: SpanInfo -> spanInfo.containingMethodId == id }

                    methodInfo.addSpans(methodSpans)

                    methodInfoMap[id] = methodInfo
                }

                collectMethods(project, fileUri, uClass.innerClasses.asList(), packageName, methodInfoMap, spans)

            }
        }
    }

    private fun getMethodsOf(project: Project, uClass: UClass): Array<UMethod> {

        //todo: see org.digma.intellij.plugin.idea.psi.java.JavaPsiUtils.Companion.getMethodsOf,
        // should we check (psiClass is PsiExtensibleClass) for kotlin
        return uClass.methods
    }


    private fun isRelevantClassType(uClass: UClass): Boolean {
        return !(uClass.isAnnotationType || uClass.isEnum || uClass.isRecord)
    }


}