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
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.GrpcFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJakartaFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJavaxFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.MicronautFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.SpringBootFrameworkEndpointDiscovery
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.toUElementOfType
import java.util.Objects

@Suppress("LightServiceMigrationCode")
class JavaLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<JavaCodeObjectDiscovery>()) {

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return runInReadAccessWithResult {
            psiFile is PsiJavaFile &&
                    PsiUtils.isValidPsiFile(psiFile) &&
                    JavaLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                    !psiFile.name.contains("package-info")
        }
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

    override fun instrumentMethod(methodObservabilityInfo: MethodObservabilityInfo): Boolean {

        try {
            if (methodObservabilityInfo.hasMissingDependency || methodObservabilityInfo.annotationClassFqn == null) {
                Log.log(logger::warn, "instrumentMethod was called with failing result from canInstrumentMethod")
                return false
            }

            val uMethod = findMethodByMethodCodeObjectId(methodObservabilityInfo.methodId)
            //will be caught here so that ErrorReporter will report it
            Objects.requireNonNull(uMethod, "can't instrument method,can't find psi method for ${methodObservabilityInfo.methodId}")

            uMethod as UMethod

            val psiJavaFile = uMethod.getContainingUFile()?.sourcePsi
            val psiMethod = uMethod.sourcePsi
            val annotationFqn = methodObservabilityInfo.annotationClassFqn

            if (psiJavaFile is PsiJavaFile && psiMethod is PsiMethod && annotationFqn != null) {

                val withSpanClass: PsiClass? = JavaPsiFacade.getInstance(project).findClass(annotationFqn, GlobalSearchScope.allScope(project))
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(
                    withSpanClass,
                    "can't instrument method,can't find annotation class  ${methodObservabilityInfo.annotationClassFqn}"
                )

                withSpanClass as PsiClass

                val importList = psiJavaFile.importList
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(importList, "Failed to get ImportList from PsiFile (methodId: ${methodObservabilityInfo.methodId})")

                importList as PsiImportList

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
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("JavaLanguageService.instrumentMethod", e)
            return false
        }
    }

    override fun getEndpointFrameworks(project: Project): Collection<EndpointDiscovery> {

        //don't need frameworks that are definitely only used in kotlin like ktor.
        //if someone writes ktor application in java then ktor endpoints will not work but that is probably
        // a very rare case and maybe even not possible.
        val micronautFramework = MicronautFrameworkEndpointDiscovery(project)
        val jaxrsJavaxFramework = JaxrsJavaxFrameworkEndpointDiscovery(project)
        val jaxrsJakartaFramework = JaxrsJakartaFrameworkEndpointDiscovery(project)
        val grpcFramework = GrpcFrameworkEndpointDiscovery(project)
        val springBootFramework = SpringBootFrameworkEndpointDiscovery(project)
        return listOf(
            micronautFramework,
            jaxrsJavaxFramework,
            jaxrsJakartaFramework,
            grpcFramework,
            springBootFramework
        )
    }


    override fun getInstrumentationProvider(): InstrumentationProvider {
        return JavaInstrumentationProvider(project, this)
    }
}