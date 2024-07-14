package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Urls
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.ReadActions
import org.digma.intellij.plugin.idea.navigation.model.NavigationProcessContext
import org.digma.intellij.plugin.idea.navigation.model.SpanLocation
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

@Suppress("LightServiceMigrationCode") // as light service it will also register in Rider and that's not necessary
internal class JvmSpanNavigationProvider(project: Project) : AbstractNavigationDiscovery(project) {

    override val type: String = "Span"

    private val spanLocations = ConcurrentHashMap(mutableMapOf<String, SpanLocation>())

    private val spanNavigationDiscoveryProviders: List<SpanNavigationDiscoveryProvider> =
        listOf(OpenTelemetrySpanNavigationDiscovery(project), MicrometerSpanNavigationDiscovery(project))


    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmSpanNavigationProvider {
            return project.service<JvmSpanNavigationProvider>()
        }
    }


    fun getUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {

        val workspaceUris = mutableMapOf<String, Pair<String, Int>>()

        spanIds.forEach(Consumer { id: String ->
            val spanLocation = spanLocations[id]
            spanLocation?.let {
                workspaceUris[id] = Pair(spanLocation.fileUri, spanLocation.offset)
            }
        })

        return workspaceUris
    }


    override fun getTask(myContext: NavigationProcessContext): Runnable {
        return Runnable {
            buildSpanNavigation(myContext)
        }
    }

    override fun getNumFound(): Int {
        return spanLocations.size
    }

    private fun buildSpanNavigation(context: NavigationProcessContext) {

        EDT.assertNonDispatchThread()
        //should not run in read action so that every section can wait for smart mode
        ReadActions.assertNotInReadAccess()

        Log.log(logger::info, "Building span navigation")

        buildLock.lock()
        try {
            spanNavigationDiscoveryProviders.forEach { provider ->

                executeCatchingWithRetry(context, provider.getName(), 30000, 5) {
                    val otelSpans: Map<String, SpanLocation> = provider.discover(context)
                    spanLocations.putAll(otelSpans)
                }
                context.indicator.checkCanceled()
            }
        } finally {
            Log.log(logger::info, "Building span navigation completed, have {} span locations", spanLocations.size)
            if (buildLock.isHeldByCurrentThread) {
                buildLock.unlock()
            }
        }

    }


    override fun removeDiscoveryForFile(file: VirtualFile) {
        removeDiscoveryForUrl(file.url)
    }


    override fun removeDiscoveryForPath(path: String) {
        val url = Urls.newUri("file", path).toString()
        removeDiscoveryForUrl(url)
    }

    private fun removeDiscoveryForUrl(url: String) {
        val fileSpans: Set<String> = spanLocations.entries.filter { it.value.fileUri == url }.map { it.key }.toSet()
        fileSpans.forEach(Consumer { key: String -> spanLocations.remove(key) })
    }
}
