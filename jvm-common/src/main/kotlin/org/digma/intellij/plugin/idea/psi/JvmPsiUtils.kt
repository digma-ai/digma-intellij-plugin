package org.digma.intellij.plugin.idea.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.common.isReadAccessAllowed
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.psi.LanguageService
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType
import java.util.function.Predicate

private const val OBJECT_CLASS_FQN = "java.lang.Object"

fun isJvmSupportedFile(project: Project, psiFile: PsiFile): Boolean {
    SupportedJvmLanguages.values().forEach {
        val languageService = LanguageService.findLanguageServiceByName(project, it.language.languageServiceClassName)
        if (languageService != null &&
            languageService is JvmLanguageService &&
            languageService.isSupportedFile(psiFile)
        ) {

            return true
        }
    }
    return false
}


fun getClassSimpleName(uClass: UClass): String {

    val packageName = uClass.getParentOfType<UFile>()?.packageName ?: ""
    val packageNameLength = if (packageName.isBlank()) {
        0
    } else {
        packageName.length + 1
    }

    return uClass.qualifiedName?.substring(packageNameLength) ?: uClass.name ?: ""
}


fun findMethodInClass(cls: UClass, methodId: String): UMethod? {
    return getMethodsInClass(cls).firstOrNull { uMethod: UMethod -> methodId == createMethodCodeObjectId(uMethod) }
}

@Suppress("UnstableApiUsage")
fun findMethodInClass(psiClass: PsiClass, methodName: String, methodPredicate: Predicate<PsiMethod>): PsiMethod? {
    val methods = psiClass.findMethodsByName(methodName)
    for (method in methods) {
        if (method is PsiMethod && methodPredicate.test(method)) {
            return method
        }
    }
    return null
}


fun getMethodsInClass(cls: UClass): Collection<UMethod> {

    if (cls.sourcePsi is PsiExtensibleClass) {

        // avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198

        val ownMethods = if (isReadAccessAllowed()) {
            (cls.sourcePsi as PsiExtensibleClass).ownMethods
        } else {
            runInReadAccessWithResult { (cls.sourcePsi as PsiExtensibleClass).ownMethods }
        }
        return ownMethods.map { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>()!! }
    }
    return cls.methods.asList()
}


fun getMethodsInClass(psiClass: PsiClass): List<PsiMethod> {
    if (psiClass is PsiExtensibleClass) {

        // avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198

        return if (isReadAccessAllowed()) {
            psiClass.ownMethods
        } else {
            runInReadAccessWithResult { psiClass.ownMethods }
        }

    }
    return psiClass.methods.asList()
}


//must be called in read access
fun hasOneOfAnnotations(psiClass: PsiClass, vararg annotationsFqn: String): Boolean {
    annotationsFqn.forEach {
        val annotObj = psiClass.getAnnotation(it)
        if (annotObj != null) {
            return true
        }
    }
    return false
}

fun findNearestAnnotation(psiMethod: PsiMethod, annotationFqn: String): PsiAnnotation? {
    val annotClass = psiMethod.getAnnotation(annotationFqn)
    if (annotClass != null) {
        return annotClass
    }

    val superMethods = psiMethod.findSuperMethods(false)
    superMethods.forEach {
        val theAnnotation = it.getAnnotation(annotationFqn)
        if (theAnnotation != null) {
            return theAnnotation
        }
    }
    return null
}

fun findNearestAnnotation(psiClass: PsiClass, annotationFqn: String): PsiAnnotation? {
    val annotClass = psiClass.getAnnotation(annotationFqn)
    if (annotClass != null) {
        return annotClass
    }

    val superClasses = psiClass.supers
    superClasses.forEach {
        val theAnnotation = it.getAnnotation(annotationFqn)
        if (theAnnotation != null) {
            return theAnnotation
        }
    }
    return null
}

fun climbUpToBaseClass(psiClass: PsiClass): PsiClass {
    var prevCLass: PsiClass = psiClass
    var currentClass: PsiClass? = psiClass
    while (currentClass != null && !isBaseClass(currentClass)) {
        prevCLass = currentClass
        currentClass = currentClass.superClass
    }

    return currentClass ?: prevCLass
}

fun isBaseClass(psiClass: PsiClass): Boolean {
    val superClass = psiClass.superClass
    return (superClass == null || superClass.qualifiedName.equals(OBJECT_CLASS_FQN))
}


fun getInnerClassesOf(psiClass: PsiClass): List<PsiClass> {
    if (psiClass is PsiExtensibleClass) {
        return psiClass.ownInnerClasses
    }
    return psiClass.innerClasses.asList()
}

fun toFileUri(psiMethod: PsiMethod): String {
    val containingFile: PsiFile? = PsiTreeUtil.getParentOfType(psiMethod, PsiFile::class.java)
    val containingFileUri = PsiUtils.psiFileToUri(containingFile!!)
    return containingFileUri
}