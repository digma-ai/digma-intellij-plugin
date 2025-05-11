package org.digma.intellij.plugin.idea.psi.java

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.common.runInReadAccessWithResult
import org.digma.intellij.plugin.idea.psi.AbstractJvmLanguageService
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.GrpcFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJakartaFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.JaxrsJavaxFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.MicronautFrameworkEndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.SpringBootFrameworkEndpointDiscovery
import org.digma.intellij.plugin.instrumentation.InstrumentationProvider
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import java.util.function.Supplier

@Suppress("LightServiceMigrationCode")
class JavaLanguageService(project: Project) : AbstractJvmLanguageService(project, project.service<JavaCodeObjectDiscovery>()) {

    override fun getLanguage(): Language {
        return JavaLanguage.INSTANCE
    }

    override fun getFileType(): FileType {
        return JavaFileType.INSTANCE
    }

    override fun isSupportedFile(psiFile: PsiFile): Boolean {
        return runInReadAccessWithResult {
            psiFile is PsiJavaFile &&
                    PsiUtils.isValidPsiFile(psiFile) &&
                    JavaLanguage.INSTANCE == psiFile.viewProvider.baseLanguage &&
                    !psiFile.name.contains("package-info")
        }
    }

    @RequiresReadLock
    override fun findClassByClassName(className: String, scope: GlobalSearchScope): UClass? {
        return JavaPsiFacade.getInstance(project).findClass(className, scope)?.toUElementOfType<UClass>()
    }


    @RequiresReadLock
    override fun findParentMethod(psiElement: PsiElement): UMethod? {
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)?.toUElementOfType<UMethod>()
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

    override fun getEndpointFrameworksRelevantOnlyForLanguage(project: Project): Collection<EndpointDiscovery> {
        return listOf()
    }

    override fun getInstrumentationProvider(): InstrumentationProvider {
        return JavaInstrumentationProvider(project, this)
    }


    //this method is called only from CodeLensService, CodeLensService should handle exceptions
    // the @Throws here is a reminder that this method may throw exception
    @Throws(Throwable::class)
    override fun findMethodsByCodeObjectIds(psiFile: PsiFile, methodIds: List<String>): Map<String, PsiElement> {

        if (methodIds.isEmpty() || !PsiUtils.isValidPsiFile(psiFile)) {
            return emptyMap()
        }

        return ReadActions.ensureReadAction(Supplier {
            val methods = mutableMapOf<String, PsiElement>()
            val visitor = object : JavaRecursiveElementWalkingVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    method.toUElementOfType<UMethod>()?.let { uMethod ->
                        val codeObjectId = createMethodCodeObjectId(uMethod)
                        if (methodIds.contains(codeObjectId)) {
                            methods[codeObjectId] = method
                        }
                    }
                }
            }
            psiFile.acceptChildren(visitor)
            return@Supplier methods
        })
    }
}