package org.digma.intellij.plugin.idea.kotlin

import com.intellij.lang.Language
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.discovery.FileDiscoveryProvider
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.discovery.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.GrpcFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.JaxrsJakartaFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.JaxrsJavaxFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.KtorFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.MicronautFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.discovery.endpoint.SpringBootFrameworkEndpointDiscovery
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.structuralsearch.visitor.KotlinRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.idea.stubindex.KotlinFileFacadeFqNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode")
class KotlinLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<KotlinCodeObjectDiscovery>()) {


    override fun getLanguage(): Language {
        return KotlinLanguage.INSTANCE
    }

    override fun getFileType(): FileType {
        return KotlinFileType.INSTANCE
    }

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return ReadActions.ensureReadAction(Supplier {
            psiFile is KtFile &&
                    PsiUtils.isValidPsiFile(psiFile) &&
                    KotlinLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                    !psiFile.name.contains("package-info") &&
                    !isScript(psiFile)
        })
    }


    private fun isScript(psiFile: PsiFile): Boolean {
        return psiFile is KtFile && psiFile.isScript()
    }


    //note that this method prefers non-compiled classes
    //todo: improve, maybe JavaPsiFacade.getInstance(project).findClass works
    @RequiresReadLock
    override fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass? {

        try {
            val classes: Collection<KtClassOrObject> = KotlinFullClassNameIndex[className, project, scope]

            if (classes.isEmpty()) {
                val files = KotlinFileFacadeFqNameIndex[className, project, scope]
                if (files.isNotEmpty()) {
                    val file: KtFile? =
                        if (files.any { ktf -> !ktf.isCompiled }) files.firstOrNull { ktf -> !ktf.isCompiled } else files.firstOrNull()
                    val fileClasses = file?.classes?.filter { psiClass: PsiClass -> psiClass.qualifiedName == className }
                    return fileClasses?.firstOrNull()?.toUElementOfType<UClass>()
                } else {
                    return null
                }
            } else {
                //prefer non-compiled class
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


    @RequiresReadLock
    override fun findParentMethod(psiElement: PsiElement): UMethod? {
        return PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java)?.toUElementOfType<UMethod>()
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


    override fun getEndpointFrameworksRelevantOnlyForLanguage(project: Project): Collection<EndpointDiscovery> {
        return listOf(KtorFrameworkEndpointDiscovery(project))
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        return KotlinInstrumentationProvider(project, this)
    }

    override fun getDiscoveryProvider(): FileDiscoveryProvider {
        return KotlinFileDiscoveryProvider()
    }

    //this method is called only from CodeLensService, CodeLensService should handle exceptions
    // the @Throws here is a reminder that this method may throw exception
    @Throws(Throwable::class)
    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: List<String>): Map<String, PsiElement> {

        if (methodIds.isEmpty() || !PsiUtils.isValidPsiFile(psiFile)) {
            return emptyMap()
        }

        return ReadActions.ensureReadAction<Map<String, PsiElement>> {
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
            return@ensureReadAction methods
        }
    }

}