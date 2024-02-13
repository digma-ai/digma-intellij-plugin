package org.digma.intellij.plugin.idea.psi.discovery.endpoint

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.digma.intellij.plugin.common.SearchScopeProvider
import org.digma.intellij.plugin.common.TextRangeUtils
import org.digma.intellij.plugin.common.executeCatchingWithRetryIgnorePCE
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResult
import org.digma.intellij.plugin.common.runInReadAccessInSmartModeWithResultAndRetryIgnorePCE
import org.digma.intellij.plugin.idea.psi.createMethodCodeObjectId
import org.digma.intellij.plugin.idea.psi.getExpressionValue
import org.digma.intellij.plugin.model.discovery.EndpointFramework
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.progress.ProcessContext
import org.digma.intellij.plugin.psi.PsiUtils
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getUCallExpression
import org.jetbrains.uast.textRange
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.util.isMethodCall

class KtorFrameworkEndpointDiscovery(private val project: Project) : EndpointDiscovery() {


    companion object {

        private const val KTOR_SERVER_ROUTING_BUILDER_PREFIX = "io.ktor.server.routing."
        //const val KTOR_SERVER_RESOURCES_PREFIX = "io.ktor.server.resources"

        val KTOR_ROUTING_BUILDER_ENDPOINTS = mapOf(
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("get") to "get",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("post") to "post",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("head") to "head",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("put") to "put",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("patch") to "patch",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("delete") to "delete",
            KTOR_SERVER_ROUTING_BUILDER_PREFIX.plus("options") to "options",
        )

    }

    override fun getName(): String {
        return "ktor"
    }


    override fun lookForEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext): List<EndpointInfo> {
        val endpoints = mutableListOf<EndpointInfo>()
        executeCatchingWithRetryIgnorePCE({
            endpoints.addAll(lookForRoutingBuilderEndpoints(searchScopeProvider, context))
        }, { e ->
            context.addError(getName(), e)
        })

        return endpoints
    }


    private fun lookForRoutingBuilderEndpoints(searchScopeProvider: SearchScopeProvider, context: ProcessContext): List<EndpointInfo> {

        val resultEndpoints = mutableListOf<EndpointInfo>()

        KTOR_ROUTING_BUILDER_ENDPOINTS.forEach { (fqName, httpMethod) ->


            val declarations =
                runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                    //todo: maybe cache the declarations, maybe in SmartElementPointer
                    val declarations = KotlinTopLevelFunctionFqnNameIndex.get(fqName, project, GlobalSearchScope.allScope(project))
                    declarations.filter { ktNamedFunction: KtNamedFunction -> ktNamedFunction.containingKtFile.isCompiled }
                }


            declarations.forEach { methodDeclarations ->

                context.indicator.checkCanceled()

                val references =
                    runInReadAccessInSmartModeWithResultAndRetryIgnorePCE(project) {
                        ReferencesSearch.search(methodDeclarations, searchScopeProvider.get()).findAll()
                    }


                references.forEach { ref ->

                    executeCatchingWithRetryIgnorePCE({
                        runInReadAccessInSmartModeWithResult(project) {
                            buildEndpointFromReference(resultEndpoints, ref, httpMethod)
                        }
                    }, { e ->
                        context.addError(getName(), e)
                    })
                }
            }
        }

        return resultEndpoints
    }

    private fun buildEndpointFromReference(resultEndpoints: MutableList<EndpointInfo>, ref: PsiReference, httpMethod: String) {

        val methodId = ref.element.toUElementOfType<UReferenceExpression>()?.getContainingUMethod()?.let { uMethod ->
            createMethodCodeObjectId(uMethod)
        } ?: ""

        val fileUri = ref.element.toUElementOfType<UReferenceExpression>()?.getContainingUFile()?.let { uFile ->
            PsiUtils.psiFileToUri(uFile.sourcePsi)
        } ?: ""

        val textRange =
            TextRangeUtils.fromJBTextRange(ref.element.toUElementOfType<UReferenceExpression>()?.getUCallExpression()?.textRange)

        ref.element.toUElementOfType<UReferenceExpression>()?.let { uReference ->

            uReference.getUCallExpression()?.let { callExpression ->

                getEndpointNameFromCallExpression(callExpression)?.let { endpointName ->

                    val id = "epHTTP:" + "HTTP " + httpMethod.uppercase() + " " + endpointName

                    resultEndpoints.add(EndpointInfo(id, methodId, fileUri, textRange, EndpointFramework.Ktor))

                }
            }
        }
    }


    private fun getEndpointNameFromCallExpression(callExpression: UCallExpression): String? {

        if (!callExpression.isMethodCall()) return null

        //always use the first argument. to be more accurate its possible to query the method parameters for a parameter named path
        // and find its index , but we see that in all cases it's the first argument

        //todo: first arg may be Regex for io/ktor/server/routing/RegexRouting.kt or Resource for io/ktor/server/resources/Routing.kt

        val firstArg = callExpression.valueArguments.firstOrNull()
        return if (firstArg is UExpression) {
            getExpressionValue(firstArg)
        } else {
            ""
        }
    }


}