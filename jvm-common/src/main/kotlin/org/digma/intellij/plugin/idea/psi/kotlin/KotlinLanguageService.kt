package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.idea.psi.AbstractJvmLanguageService
import org.digma.intellij.plugin.instrumentation.CanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.JvmCanInstrumentMethodResult
import org.digma.intellij.plugin.log.Log
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeFqNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

@Suppress("LightServiceMigrationCode")
class KotlinLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<KotlinCodeObjectDiscovery>()) {


    override fun isSupportedFile(project: Project, psiFile: PsiFile): Boolean {
        return psiFile is KtFile &&
                KotlinLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                !psiFile.name.contains("package-info")
    }


    private fun isScript(psiFile: PsiFile): Boolean {
        return psiFile is KtFile && psiFile.isScript()
    }

    override fun isRelevant(psiFile: PsiFile): Boolean {
        return super.isRelevant(psiFile) && !isScript(psiFile)
    }

    override fun isServiceFor(language: Language): Boolean {
        return KotlinLanguage::class.java == language.javaClass
    }

    override fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass? {
        val classes: Collection<KtClassOrObject> = KotlinFullClassNameIndex.get(className, project, scope)
        if (classes.isEmpty()) {
            val files = KotlinFileFacadeFqNameIndex.get(className, project, scope)
            if (files.isNotEmpty()) {
                val file = files.first()
                val fileClasses = file.classes.filter { psiClass: PsiClass -> psiClass.qualifiedName == className }
                return fileClasses.firstOrNull()?.toUElementOfType<UClass>()
            } else {
                return null
            }
        } else {
            return classes.firstOrNull()?.toUElementOfType<UClass>()
        }
    }


    override fun refreshCodeLens() {
        project.service<KotlinCodeLensService>().refreshCodeLens()
    }


    override fun getCodeLens(psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        return project.service<KotlinCodeLensService>().getCodeLens(psiFile)
    }


    override fun findParentMethod(psiElement: PsiElement): UMethod? {
        return PsiTreeUtil.getParentOfType(psiElement, KtFunction::class.java)?.toUElementOfType<UMethod>()
    }



    override fun instrumentMethod(result: CanInstrumentMethodResult): Boolean {
        if (result !is JvmCanInstrumentMethodResult) {
            Log.log(logger::warn, "instrumentMethod was called with failing result from canInstrumentMethod")
            return false
        }

        if (result.containingFile.sourcePsi is KtFile && result.uMethod.sourcePsi is KtFunction) {

            val ktFile: KtFile = result.containingFile.sourcePsi as KtFile
            val ktFunction: KtFunction = result.uMethod.sourcePsi as KtFunction
            val methodId: String = result.methodId
            val withSpanClass: PsiClass = result.withSpanClass

            val importList = ktFile.importList
            if (importList == null) {
                Log.log(logger::warn, "Failed to get ImportList from PsiFile (methodId: {})", methodId)
                return false
            }

            WriteCommandAction.runWriteCommandAction(project) {
                val ktPsiFactory = KtPsiFactory(project)
                val shortClassNameAnnotation = withSpanClass.name
                if (shortClassNameAnnotation != null) {
                    ktFunction.addAnnotationEntry(ktPsiFactory.createAnnotationEntry("@$shortClassNameAnnotation"))
                }

                val existing =
                    importList.imports.find { ktImportDirective: KtImportDirective? -> ktImportDirective?.importedFqName?.asString() == withSpanClass.qualifiedName }
                if (existing == null) {
                    val importStatement = ktPsiFactory.createImportDirective(ImportPath.fromString(withSpanClass.qualifiedName!!))
                    importList.add(importStatement)
                }
            }
            return true
        } else {
            return false
        }

    }

}