package org.digma.intellij.plugin.idea.psi.kotlin

import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.psi.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.GrpcFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJakartaFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJavaxFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.KtorFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.MicronautFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.SpringBootFrameworkEndpointDiscovery
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.instrumentation.MethodObservabilityInfo
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeFqNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.toUElementOfType
import java.util.Objects
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode")
class KotlinLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<KotlinCodeObjectDiscovery>()) {


    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            psiFile is KtFile &&
                    PsiUtils.isValidPsiFile(psiFile) &&
                    KotlinLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                    !psiFile.name.contains("package-info")
        })
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


    //note that this method prefers non compiled classes
    override fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass? {

        try {
            val classes: Collection<KtClassOrObject> = KotlinFullClassNameIndex.get(className, project, scope)

            if (classes.isEmpty()) {
                val files = KotlinFileFacadeFqNameIndex.get(className, project, scope)
                if (files.isNotEmpty()) {
                    val file: KtFile? =
                        if (files.any { ktf -> !ktf.isCompiled }) files.firstOrNull { ktf -> !ktf.isCompiled } else files.firstOrNull()
                    val fileClasses = file?.classes?.filter { psiClass: PsiClass -> psiClass.qualifiedName == className }
                    return fileClasses?.firstOrNull()?.toUElementOfType<UClass>()
                } else {
                    return null
                }
            } else {
                //prefer non compiled class
                if (classes.any { ktc -> !ktc.containingKtFile.isCompiled }) {
                    return classes.firstOrNull { ktc -> !ktc.containingKtFile.isCompiled }?.toUElementOfType<UClass>()
                }
                return classes.firstOrNull()?.toUElementOfType<UClass>()
            }
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "KotlinLanguageService.findClassByClassName", e)
            return null
        }
    }


    override fun findParentMethod(psiElement: PsiElement): UMethod? {
        return PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java)?.toUElementOfType<UMethod>()
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

            val ktFile = uMethod.getContainingUFile()?.sourcePsi
            val ktFunction = uMethod.sourcePsi
            val annotationFqn = methodObservabilityInfo.annotationClassFqn


            if (ktFile is KtFile && ktFunction is KtFunction && annotationFqn != null) {

                val withSpanClass: PsiClass? = JavaPsiFacade.getInstance(project).findClass(annotationFqn, GlobalSearchScope.allScope(project))
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(
                    withSpanClass,
                    "can't instrument method,can't find annotation class  ${methodObservabilityInfo.annotationClassFqn}"
                )

                withSpanClass as PsiClass

                val importList = ktFile.importList
                //will be caught here so that ErrorReporter will report it
                Objects.requireNonNull(importList, "Failed to get ImportList from PsiFile (methodId: ${methodObservabilityInfo.methodId})")

                importList as KtImportList

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
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError(project, "KotlinLanguageService.instrumentMethod", e)
            return false
        }

    }

    override fun getEndpointFrameworks(project: Project): Collection<EndpointDiscovery> {
        val micronautFramework = MicronautFrameworkEndpointDiscovery(project)
        val jaxrsJavaxFramework = JaxrsJavaxFrameworkEndpointDiscovery(project)
        val jaxrsJakartaFramework = JaxrsJakartaFrameworkEndpointDiscovery(project)
        val grpcFramework = GrpcFrameworkEndpointDiscovery(project)
        val springBootFramework = SpringBootFrameworkEndpointDiscovery(project)
        val ktorFramework = KtorFrameworkEndpointDiscovery(project)
        return listOf(
            micronautFramework,
            jaxrsJavaxFramework,
            jaxrsJakartaFramework,
            grpcFramework,
            springBootFramework,
            ktorFramework
        )
    }


    override fun getInstrumentationProvider(): InstrumentationProvider {
        return KotlinInstrumentationProvider(project, this)
    }

    //this method is called only from CodeLensService, CodeLensService should handle exceptions
    // the @Throws here is a reminder that this method may throw exception
    @Throws(Throwable::class)
    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: MutableList<String>): Map<String, PsiElement> {

        if (methodIds.isEmpty() || !PsiUtils.isValidPsiFile(psiFile)) {
            return emptyMap()
        }

        return runInReadAccessWithResult {
            val methods = mutableMapOf<String, PsiElement>()

            val visitor = object : KotlinRecursiveElementWalkingVisitor() {

                override fun visitNamedFunction(function: KtNamedFunction) {

                    function.toUElementOfType<UMethod>()?.let { uMethod ->
                        val codeObjectId = createMethodCodeObjectId(uMethod)
                        if (methodIds.contains(codeObjectId)) {
                            methods[codeObjectId] = function
                        }
                    }
                }
            }

            psiFile.acceptChildren(visitor)

            return@runInReadAccessWithResult methods
        }
    }

}