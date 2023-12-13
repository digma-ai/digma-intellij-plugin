package org.digma.intellij.plugin.instrumentation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod

class JvmCanInstrumentMethodResult(
    val methodId: String, val uMethod: UMethod, val withSpanClass: PsiClass,
    val containingFile: UFile,
) : CanInstrumentMethodResult() {


}