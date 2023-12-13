package org.digma.intellij.plugin.idea.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UField
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.ULocalVariable
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import java.util.function.Predicate


fun createMethodCodeObjectId(uMethod: UMethod): String {

    val uClass = uMethod.getParentOfType<UClass>()

    //usually these should be not null for java. but the PSI api declares them as null, so we must check.
    // they can be null for groovy/kotlin
    if (uClass == null || uClass.qualifiedName == null) {
        return uMethod.name
    }

    val packageName = uMethod.getParentOfType<UFile>()?.packageName ?: ""

//    var className = ""
    val className = uClass.name?.replace('.', '$') ?: ""
//    try {
//        className = uClass.qualifiedName?.substring(packageName.length + 1)?.replace('.', '$') ?: uClass.name
//    } catch (e: NullPointerException) {
//        //there should not be a NPE for java method because in java a method must have a containing class.
//        // It's only to satisfy intellij warnings.
//        //the methods getContainingClass and getQualifiedName may return null, but it can only happen
//        //for other jvm languages like scala/groovy/kotlin
//    }

    return packageName + "." + className + "\$_$" + uMethod.name
}


fun detectMethodUnderCaret(psiFile: PsiFile, fileUri: String, caretOffset: Int): MethodUnderCaret {

    val packageName = psiFile.toUElementOfType<UFile>()?.packageName ?: ""
    val underCaret: PsiElement =
        psiFile.findElementAt(caretOffset) ?: return MethodUnderCaret("", "", "", packageName, fileUri, true)
    val psiMethod = underCaret.toUElement()?.getParentOfType<UMethod>()
    val className: String = psiMethod?.getParentOfType<UClass>()?.name ?: ""
    if (psiMethod != null) {
        return MethodUnderCaret(
            createMethodCodeObjectId(psiMethod),
            psiMethod.name,
            className,
            packageName,
            fileUri
        )
    }
    return MethodUnderCaret("", "", className, packageName, fileUri)
}


fun createSpanNameForWithSpanAnnotation(uMethod: UMethod, withSpanAnnotation: UAnnotation, containingClass: UClass): String {

    //must be called in ReadAction and smart mode
    val value: String? = getPsiAnnotationAttributeValue(withSpanAnnotation, "value")
    return if (!value.isNullOrBlank()) {
        value
    } else {
        containingClass.name + "." + uMethod.name
    }
}


fun createSpanIdForWithSpanAnnotation(
    uMethod: UMethod,
    withSpanAnnotation: UAnnotation,
    containingClass: UClass,
    instLibrary: String,
): String {
    val spanName: String = createSpanNameForWithSpanAnnotation(uMethod, withSpanAnnotation, containingClass)
    return "$instLibrary\$_$$spanName"
}

fun createSpanIdFromInstLibraryAndSpanName(instLibrary: String, spanName: String): String {
    return "$instLibrary\$_$$spanName"
}


fun getPsiAnnotationAttributeValue(uAnnotation: UAnnotation, attributeName: String): String? {
    val attrValue = uAnnotation.findAttributeValue(attributeName)
    return extractValue(attrValue)
}

fun extractValue(value: UExpression?): String? {
    if (value is ULiteralExpression) {
        return getLiteralValue(value)
    } else if (value is UExpression) {
        return getExpressionValue(value)
    }
    return null
}

fun getExpressionValue(uExpression: UExpression): String? {
    if (uExpression is UReferenceExpression) {
        return getReferenceExpressionValue(uExpression)
    } else if (uExpression is ULiteralExpression) {
        return getLiteralValue(uExpression)
    }
    return null
}

fun getReferenceExpressionValue(uReferenceExpression: UReferenceExpression): String? {
    val element: UElement? = uReferenceExpression.resolveToUElement()
    if (element is UField) {
        return getFieldValue(element)
    } else if (element is ULocalVariable) {
        return getLocalVariableValue(element)
    }

    return null
}

fun getLocalVariableValue(uLocalVariable: ULocalVariable): String? {
    val initializer: UElement? = uLocalVariable.uastInitializer
    if (initializer is ULiteralExpression) {
        return getLiteralValue(initializer)
    } else if (initializer is UReferenceExpression) {
        return getReferenceExpressionValue(initializer)
    }
    return null
}

fun getFieldValue(uField: UField): String? {
    val initializer: UExpression? = uField.uastInitializer
    if (initializer is ULiteralExpression) {
        return getLiteralValue(initializer)
    } else if (initializer is UReferenceExpression) {
        return getReferenceExpressionValue(initializer)
    }
    return uField.sourcePsi?.text
}

fun getLiteralValue(uLiteralExpression: ULiteralExpression): String? {
    return uLiteralExpression.value?.toString()
}


//todo: move to JvmPsiUtils.kt
fun findMethodInClass(psiClass: PsiClass, methodName: String, methodPredicate: Predicate<PsiMethod>): PsiMethod? {
    val methods = psiClass.findMethodsByName(methodName)
    for (method in methods) {
        if (method is PsiMethod && methodPredicate.test(method)) {
            return method
        }
    }
    return null
}


fun isMethodWithFirstArgumentString(uElement: UElement, methodName: String, containingClassName: String): Boolean {
    return isMethod(uElement, methodName, containingClassName) { uMethod: UMethod ->
        if (uMethod.parameters.isNotEmpty() && uMethod.parameters[0].type is PsiClassReferenceType) {
            val type =
                (uMethod.parameters[0].type as PsiClassReferenceType).resolve()
            if (type != null) {
                return@isMethod type.qualifiedName != null && type.qualifiedName == "java.lang.String"
            }
        }
        false
    }
}

fun isMethod(uElement: UElement, methodName: String, containingClassName: String, methodPredicate: Predicate<UMethod>?): Boolean {
    if ((uElement is UMethod && uElement.name == methodName && uElement.getContainingUClass() != null && uElement.getContainingUClass()?.qualifiedName != null) && uElement.getContainingUClass()?.qualifiedName == containingClassName) {
        return methodPredicate == null || methodPredicate.test(uElement)
    }
    return false
}