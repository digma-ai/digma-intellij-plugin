package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.digma.intellij.plugin.idea.psi.AbstractCodeObjectDiscovery
import org.digma.intellij.plugin.model.discovery.DocumentInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.discovery.SpanInfo
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import java.util.Objects


class KotlinCodeObjectDiscovery : AbstractCodeObjectDiscovery() {

}