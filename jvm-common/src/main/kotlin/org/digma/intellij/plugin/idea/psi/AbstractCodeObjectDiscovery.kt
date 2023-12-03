package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

/**
 * code object discovery for jvm languages
 */
abstract class AbstractCodeObjectDiscovery {


    fun buildDocumentInfo(project: Project, psiFile: PsiFile): DocumentInfo {

        val fileUri = PsiUtils.psiFileToUri(psiFile)

        val uFile: UFile = psiFile.toUElementOfType<UFile>() ?: return DocumentInfo(fileUri, mutableMapOf())

        val packageName = uFile.packageName

        val classes = uFile.classes

        val methodInfoMap = mutableMapOf<String, MethodInfo>()

        collectMethods(project, fileUri, classes, packageName, methodInfoMap)

        return DocumentInfo(fileUri, methodInfoMap)

    }

    private fun collectMethods(
        project: Project,
        fileUri: String,
        classes: List<UClass>,
        packageName: String,
        methodInfoMap: MutableMap<String, MethodInfo>,
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
                    //todo: span discovery
                    methodInfo.addSpans(ArrayList())
                    methodInfoMap[id] = methodInfo
                }

                collectMethods(project, fileUri, uClass.innerClasses.asList(), packageName, methodInfoMap)

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