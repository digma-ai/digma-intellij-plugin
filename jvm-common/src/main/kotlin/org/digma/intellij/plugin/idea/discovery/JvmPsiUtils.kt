package org.digma.intellij.plugin.idea.discovery

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElementOfType
import java.util.function.Predicate

private const val OBJECT_CLASS_FQN = "java.lang.Object"

@RequiresReadLock(generateAssertion = false)
fun getClassSimpleName(uClass: UClass): String {

    //try a few ways to get the class simple name
    try {
        val simpleName = if (uClass.sourcePsi is PsiClass) {
            (uClass.sourcePsi as PsiClass).name
        } else {
            null
        }

        if (simpleName != null) {
            return simpleName
        }
    } catch (_: Throwable) {
    }

    try {

        val packageName = uClass.getParentOfType<UFile>()?.packageName ?: ""
        val packageNameLength = if (packageName.isBlank()) {
            0
        } else {
            packageName.length + 1
        }

        //for some reason sometimes this throws StringIndexOutOfBoundsException.not clear why the computation of the
        // package name length above is correct. maybe it happens when indexes are not fully ready or if PSI parsing is not complete.
        return uClass.qualifiedName?.substring(packageNameLength) ?: uClass.qualifiedName ?: ""
    } catch (_: IndexOutOfBoundsException) {
        return uClass.name ?: uClass.qualifiedName ?: ""
    }

}

@RequiresReadLock(generateAssertion = false)
fun findMethodInClass(cls: UClass, methodId: String): UMethod? {
    return getMethodsInClass(cls).firstOrNull { uMethod: UMethod -> methodId == createMethodCodeObjectId(uMethod) }
}

@Suppress("UnstableApiUsage")
@RequiresReadLock(generateAssertion = false)
fun findMethodInClass(psiClass: PsiClass, methodName: String, methodPredicate: Predicate<PsiMethod>): PsiMethod? {
    val methods = psiClass.findMethodsByName(methodName)
    for (method in methods) {
        if (method is PsiMethod && methodPredicate.test(method)) {
            return method
        }
    }
    return null
}

@RequiresReadLock(generateAssertion = false)
fun getMethodsInClass(cls: UClass): Collection<UMethod> {
    return if (cls.sourcePsi is PsiExtensibleClass) {
        // Avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198
        val ownMethods = (cls.sourcePsi as PsiExtensibleClass).ownMethods
        ownMethods.mapNotNull { psiMethod: PsiMethod -> psiMethod.toUElementOfType<UMethod>() }
    } else {
        cls.methods.asList()
    }
}

@RequiresReadLock(generateAssertion = false)
fun getMethodsInClass(psiClass: PsiClass): List<PsiMethod> {
    return if (psiClass is PsiExtensibleClass) {
        // Avoid cases when there are generated methods and/or constructors such as lombok creates,
        // see issue https://github.com/digma-ai/digma-intellij-plugin/issues/833
        // see issue https://youtrack.jetbrains.com/issue/IDEA-323198
        psiClass.ownMethods
    } else {
        psiClass.methods.asList()
    }
}


@RequiresReadLock(generateAssertion = false)
fun hasOneOfAnnotations(psiClass: PsiClass, vararg annotationsFqn: String): Boolean {
    annotationsFqn.forEach {
        val annotObj = psiClass.getAnnotation(it)
        if (annotObj != null) {
            return true
        }
    }
    return false
}

@RequiresReadLock(generateAssertion = false)
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

@RequiresReadLock(generateAssertion = false)
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

@RequiresReadLock(generateAssertion = false)
fun climbUpToBaseClass(psiClass: PsiClass): PsiClass {
    var prevCLass: PsiClass = psiClass
    var currentClass: PsiClass? = psiClass
    while (currentClass != null && !isBaseClass(currentClass)) {
        prevCLass = currentClass
        currentClass = currentClass.superClass
    }

    return currentClass ?: prevCLass
}

@RequiresReadLock(generateAssertion = false)
fun isBaseClass(psiClass: PsiClass): Boolean {
    val superClass = psiClass.superClass
    return (superClass == null || superClass.qualifiedName.equals(OBJECT_CLASS_FQN))
}

@RequiresReadLock(generateAssertion = false)
fun toFileUri(psiMethod: PsiMethod): String {
    val containingFile = psiMethod.containingFile
    return containingFile?.let {
        PsiUtils.psiFileToUri(it)
    } ?: ""
}