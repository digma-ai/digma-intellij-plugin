package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.discovery.model.SpanLocation
import org.digma.intellij.plugin.idea.index.hasIndex
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2

@Suppress("LightServiceMigrationCode")
internal class JvmSpanNavigationProvider(private val project: Project) {

    private val logger = thisLogger()

    private val spanLocations = ConcurrentHashMap(mutableMapOf<String, SpanLocation>())

    private val maintenanceLock = Mutex()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmSpanNavigationProvider {
            return project.service<JvmSpanNavigationProvider>()
        }
    }

    fun getUrisForSpanIds(spanIds: List<String>): Map<String, Pair<String, Int>> {
        val workspaceUris = mutableMapOf<String, Pair<String, Int>>()
        spanIds.forEach { spanId ->
            val spanLocation = spanLocations[spanId]
            spanLocation?.takeIf { it.isAlive() }?.let { location ->
                location.file.let { file ->
                    workspaceUris[spanId] = Pair(file.url, location.offset)
                }
            }
        }
        return workspaceUris
    }

    fun getMethodIdBySpanId(spanId: String): String? {
        return spanLocations[spanId]?.methodCodeObjectId
    }


    suspend fun processCandidateFile(fileInfo: FileDiscoveryInfo) {
        Log.trace(logger, "processing candidateFile {}", fileInfo.file.url)
        maintenanceLock.withLock {

            removeEntriesForFile(fileInfo.file)

            fileInfo.methods.forEach { (methodId, methodInfo) ->
                methodInfo.spans.forEach { spanInfo ->
                    val file = fileInfo.file
                    val spanLocation = SpanLocation(file, spanInfo.offset, methodId)
                    Log.trace(logger, "adding span location for {} span {}", file.url, spanInfo.id)
                    spanLocations[spanInfo.id] = spanLocation
                }
            }
        }
    }

    private fun removeEntriesForFile(file: VirtualFile) {
        spanLocations.entries.removeIf { (_, spanLocation) -> spanLocation.file == file }
    }

    suspend fun maintenance() {

        Log.trace(logger, "starting maintenance, current span location count {}", spanLocations.size)
        if(logger.isTraceEnabled){
            Log.trace(logger, "span locations {}", spanLocations.entries.joinToString(", ") { "[${it.key} -> ${it.value}]" })
        }

        maintenanceLock.withLock {
            val toRemove = spanLocations.filter { (_, spanLocation) -> !spanLocation.isAlive() || !hasIndex(project, spanLocation.file) }.keys
            if (toRemove.isEmpty()) {
                return@withLock
            }
            Log.trace(logger, "maintenance removing spans {}", toRemove)
            spanLocations.entries.removeIf { toRemove.contains(it.key) }
        }
    }


}
