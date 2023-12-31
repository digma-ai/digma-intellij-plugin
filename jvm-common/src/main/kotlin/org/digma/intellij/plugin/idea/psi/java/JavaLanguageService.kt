package org.digma.intellij.plugin.idea.psi.java

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.idea.psi.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.GrpcFramework
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJakartaFramework
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJavaxFramework
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.MicronautFramework
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.SpringBootFramework
import org.digma.intellij.plugin.instrumentation.CanInstrumentMethodResult
import org.digma.intellij.plugin.instrumentation.JvmCanInstrumentMethodResult
import org.digma.intellij.plugin.log.Log
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

@Suppress("LightServiceMigrationCode")
class JavaLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<JavaCodeObjectDiscovery>()) {

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return psiFile is PsiJavaFile &&
                JavaLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                !psiFile.name.contains("package-info")
    }

    override fun isServiceFor(language: Language): Boolean {
        return JavaLanguage::class.java == language.javaClass
    }

    override fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass? {
        return JavaPsiFacade.getInstance(project).findClass(className, scope)?.toUElementOfType<UClass>()
//        val classes:Collection<PsiClass> = JavaFullClassNameIndex.getInstance().get(className, project, GlobalSearchScope.projectScope(project))
//        return classes.firstOrNull()?.toUElementOfType<UClass>()
    }

    override fun refreshCodeLens() {
        project.service<JavaCodeLensService>().refreshCodeLens()
    }


    override fun getCodeLens(psiFile: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        return project.service<JavaCodeLensService>().getCodeLens(psiFile)
    }

    override fun findParentMethod(psiElement: PsiElement): UMethod? {
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)?.toUElementOfType<UMethod>()
    }

    override fun instrumentMethod(result: CanInstrumentMethodResult): Boolean {
        if (result !is JvmCanInstrumentMethodResult) {
            Log.log(logger::warn, "instrumentMethod was called with failing result from canInstrumentMethod")
            return false
        }


        if (result.containingFile.sourcePsi is PsiJavaFile && result.uMethod.sourcePsi is PsiMethod) {


            val psiJavaFile: PsiJavaFile = result.containingFile.sourcePsi as PsiJavaFile
            val psiMethod: PsiMethod = result.uMethod.sourcePsi as PsiMethod
            val methodId: String = result.methodId
            val withSpanClass: PsiClass = result.withSpanClass

            val importList = psiJavaFile.importList
            if (importList == null) {
                Log.log(logger::warn, "Failed to get ImportList from PsiFile (methodId: {})", methodId)
                return false
            }

            WriteCommandAction.runWriteCommandAction(project) {
                val psiFactory = PsiElementFactory.getInstance(project)
                val shortClassNameAnnotation = withSpanClass.name
                if (shortClassNameAnnotation != null) {
                    psiMethod.modifierList.addAnnotation(shortClassNameAnnotation)
                }

                val existing = importList.findSingleClassImportStatement(withSpanClass.qualifiedName)
                if (existing == null) {
                    val importStatement = psiFactory.createImportStatement(withSpanClass)
                    importList.add(importStatement)
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun getEndpointFrameworks(project: Project): Collection<EndpointDiscovery> {

        //don't need frameworks that are definitely only used in kotlin like ktor.
        //if someone writes ktor application in java then ktor endpoints will not work but that is probably
        // a very rare case and maybe even not possible.
        val micronautFramework = MicronautFramework(project)
        val jaxrsJavaxFramework = JaxrsJavaxFramework(project)
        val jaxrsJakartaFramework = JaxrsJakartaFramework(project)
        val grpcFramework = GrpcFramework(project)
        val springBootFramework = SpringBootFramework(project)
        return listOf(
            micronautFramework,
            jaxrsJavaxFramework,
            jaxrsJakartaFramework,
            grpcFramework,
            springBootFramework
        )
    }
}