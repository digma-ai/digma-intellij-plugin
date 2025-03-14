package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Urls
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.idea.navigation.model.NavigationDiscoveryTrigger
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscovery
import org.digma.intellij.plugin.idea.psi.discovery.endpoint.EndpointDiscoveryService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Predicate

@Suppress("LightServiceMigrationCode") // as light service it will also register in Rider and that's not necessary
internal class JvmEndpointNavigationProvider(project: Project) : AbstractNavigationDiscovery(project) {

    override val type: String = "Endpoint"


    private val endpointsMap = ConcurrentHashMap(mutableMapOf<String, MutableSet<EndpointInfo>>())

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmEndpointNavigationProvider {
            return project.service<JvmEndpointNavigationProvider>()
        }
    }


    fun getEndpointInfos(endpointId: String?): Set<EndpointInfo> {
        val endpointInfos = endpointsMap[endpointId] ?: return setOf()
        // cloning the result, to keep consistency
        return endpointInfos.toSet()
    }


    override fun getTask(myContext: NavigationProcessContext, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, retry: Int): Runnable {
        return Runnable {
            buildEndpointNavigation(myContext, navigationDiscoveryTrigger, retry)
        }
    }

    override fun getNumFound(): Int {
        return endpointsMap.values.flatten().size
    }

    private fun buildEndpointNavigation(context: NavigationProcessContext, navigationDiscoveryTrigger: NavigationDiscoveryTrigger, retry: Int) {

        EDT.assertNonDispatchThread()
        //should not run in read action so that every section can wait for smart mode
        ReadActions.assertNotInReadAccess()

        Log.log(logger::info, project, "Building endpoint navigation, trigger {},retry {}", navigationDiscoveryTrigger, retry)

        buildLock.lock()
        try {

            //some frameworks may fail. for example ktor will fail if kotlin plugin is disabled
            val endpointDiscoveries = EndpointDiscoveryService.getInstance(project).getAllEndpointDiscovery()

            endpointDiscoveries.forEach { endpointDiscovery: EndpointDiscovery ->

                executeCatchingWithRetry(context, endpointDiscovery.getName(), 30000, 5) {
                    val endpointInfos = endpointDiscovery.lookForEndpoints(context.searchScope, context)
                    endpointInfos?.forEach {
                        addToMethodsMap(it)
                    }
                }

                context.indicator.checkCanceled()
            }
        } finally {
            if (buildLock.isHeldByCurrentThread) {
                buildLock.unlock()
            }
            Log.log(
                logger::info, project,
                "Building endpoint navigation completed, trigger {},retry {}, have {} endpoints locations",
                navigationDiscoveryTrigger,
                retry,
                endpointsMap.size
            )
        }
    }


    private fun addToMethodsMap(endpointInfo: EndpointInfo) {
        val methods = endpointsMap.computeIfAbsent(endpointInfo.id) { CopyOnWriteArraySet() }
        methods.add(endpointInfo)
    }


    override fun removeDiscoveryForFile(file: VirtualFile) {
        removeDiscoveryForUrl(file.url)
    }


    override fun removeDiscoveryForPath(path: String) {
        try {
            val url = Urls.newUri("file", path).toString()
            removeDiscoveryForUrl(url)
        } catch (e: Throwable) {
            //UrlImpl throws RuntimeException here ,
            //something like 'Relative path in absolute URI: file://What's%20New%20in%20IntelliJ%20IDEA'
            //catch this error and log, no need to report to posthog
            Log.warnWithException(logger, project, e, "error removing path")
        }
    }

    private fun removeDiscoveryForUrl(url: String) {
        val urlPredicate = UrlPredicate(url)
        for (methods in endpointsMap.values) {
            methods.removeIf(urlPredicate)
        }
    }


    private class UrlPredicate(private val theFileUri: String) : Predicate<EndpointInfo> {
        override fun test(endpointInfo: EndpointInfo): Boolean {
            return theFileUri == endpointInfo.containingFileUri
        }
    }
}