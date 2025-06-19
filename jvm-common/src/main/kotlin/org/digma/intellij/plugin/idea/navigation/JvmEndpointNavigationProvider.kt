package org.digma.intellij.plugin.idea.navigation

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.digma.intellij.plugin.discovery.model.EndpointLocation
import org.digma.intellij.plugin.discovery.model.FileDiscoveryInfo
import org.digma.intellij.plugin.idea.index.hasIndex
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Suppress("LightServiceMigrationCode")
internal class JvmEndpointNavigationProvider(private val project: Project) {

    private val logger = thisLogger()

    private val endpointsMap = ConcurrentHashMap(mutableMapOf<String, MutableSet<EndpointLocation>>())

    private val maintenanceLock = Mutex()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): JvmEndpointNavigationProvider {
            return project.service<JvmEndpointNavigationProvider>()
        }
    }


    fun getEndpointInfos(endpointId: String?): Set<EndpointLocation> {
        val endpointInfos = endpointsMap[endpointId] ?: return setOf()
        return endpointInfos.toSet()
    }

    suspend fun processFileInfo(fileInfo: FileDiscoveryInfo) {
        Log.trace(logger, "processing candidateFile {}", fileInfo.file.url)
        maintenanceLock.withLock {

            //remove all entries for this file if any
            removeEntriesForFile(fileInfo.file)

            fileInfo.methods.forEach { (methodId, methodInfo) ->
                methodInfo.endpoints.forEach { endpointInfo ->
                    val file = fileInfo.file
                    val endpointLocation = EndpointLocation(file, endpointInfo.id, endpointInfo.offset, methodId)
                    Log.trace(logger, project, "adding endpoint location for {} endpoint {}", file.url, endpointLocation.endpointId)
                    val methods = endpointsMap.computeIfAbsent(endpointLocation.endpointId) { CopyOnWriteArraySet() }
                    methods.add(endpointLocation)
                }
            }
        }
    }

    private fun removeEntriesForFile(file: VirtualFile) {
        endpointsMap.entries.removeIf { (_, endpointLocations) -> endpointLocations.any { it.file == file } }
    }


    suspend fun maintenance() {

        Log.trace(logger, project, "starting maintenance, current endpoint location count {}", endpointsMap.size)
        if (logger.isTraceEnabled) {
            Log.trace(logger, project, "endpoint locations [{}]", endpointsMap.entries.joinToString(", ") { "[${it.key} -> ${it.value}]" })
        }

        maintenanceLock.withLock {
            val toRemove = endpointsMap.filter { (_, endpointLocations) ->
                !endpointLocations.any { it.isAlive() } || !endpointLocations.any { hasIndex(project, it.file) }
            }.keys
            if (toRemove.isEmpty()) {
                return@withLock
            }
            Log.trace(logger, project, "maintenance removing endpoints {}", toRemove)
            endpointsMap.entries.removeIf { toRemove.contains(it.key) }
        }
    }

    fun status(): String {
        return "  JvmEndpointNavigationProvider: ${endpointsMap.size} endpoint groups"
    }

}