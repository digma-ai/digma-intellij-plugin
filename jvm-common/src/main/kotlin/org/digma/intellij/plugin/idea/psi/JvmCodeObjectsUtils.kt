package org.digma.intellij.plugin.idea.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType


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

