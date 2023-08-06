package org.digma.intellij.plugin.idea.deps

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.StartupActivity
import org.digma.intellij.plugin.log.Log
import org.jetbrains.annotations.NotNull
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import com.intellij.openapi.diagnostic.Logger


class ModulesDepsService(private val project: Project) : Disposable {

    companion object {
        private val logger = Logger.getInstance(ModulesDepsService::class.java)
        @JvmStatic
        fun getInstance(project: Project): ModulesDepsService {
            Log.test(logger,"Getting instance of ${ModulesDepsService::class.simpleName}")
            val service = project.getService(ModulesDepsService::class.java)
            Log.test(logger, "Returning ${ModulesDepsService::class.simpleName}")
            return service
        }

        @JvmStatic
        fun toUnifiedCoordinates(lib: LibraryOrderEntry): UnifiedCoordinates {
            // Gradle library name for example : "Gradle: org.aspectj:aspectjweaver:1.8.14"
            // Maven  library name for example : "Maven: com.google.guava:guava:32.0.1"
            val splitSpaces = lib.libraryName!!.split(' ')
            val netoLibName = splitSpaces.last().trim()

            val splitNames = netoLibName.split(':')
            val groupId = splitNames[0]
            val artifactId = splitNames[1]
            var version: String? = null
            if (splitNames.size >= 3) {
                version = splitNames[2]
            }
            return UnifiedCoordinates(groupId, artifactId, version)
        }

        @JvmStatic
        fun buildMetadata(module: Module): ModuleMetadata {
            val moduleRootManager = ModuleRootManager.getInstance(module)
            val orderEntries = moduleRootManager.orderEntries
            val libDeps = orderEntries.filterIsInstance<LibraryOrderEntry>()
                .filter { !it.libraryName.isNullOrBlank() }

            val moduleMetadata = evalModuleMetadata(libDeps)

            return moduleMetadata
        }

        @JvmStatic
        fun evalModuleMetadata(libDeps: Collection<LibraryOrderEntry>): ModuleMetadata {
            // quarkus
            var hasOpenTelemetryAnnotations = false
            var quarkusVersion: String? = null
            var hasQuarkusOpenTelemetry = false

            // spring boot
            var hasSpringBootStarterActuator = false
            var hasMicrometerTracingBridgeOtel = false
            var hasOtelExporterOtlp = false
            var springBootVersion: String? = null


            for (currLib in libDeps) {
                val libCoord = toUnifiedCoordinates(currLib)

                if (!hasOpenTelemetryAnnotations) {
                    hasOpenTelemetryAnnotations = checkOpenTelemetryAnnotations(libCoord)
                }
                if (quarkusVersion == null) {
                    val hasQuarkus = checkQuarkus(libCoord)
                    if (hasQuarkus) {
                        quarkusVersion = libCoord.version
                    }
                }
                if (!hasQuarkusOpenTelemetry) {
                    hasQuarkusOpenTelemetry = checkQuarkusOpenTelemetry(libCoord)
                }

                // spring boot
                if (springBootVersion == null) {
                    val hasSpringBoot = checkSpringBoot(libCoord)
                    if (hasSpringBoot) {
                        springBootVersion = libCoord.version
                    }
                }
                if (!hasSpringBootStarterActuator) {
                    hasSpringBootStarterActuator = checkSpringBootStarterActuator(libCoord)
                }
                if (!hasMicrometerTracingBridgeOtel) {
                    hasMicrometerTracingBridgeOtel = checkMicrometerTracingBridgeOtel(libCoord)
                }
                if (!hasOtelExporterOtlp) {
                    hasOtelExporterOtlp = checkOtelExporterOtlp(libCoord)
                }

                // other
            }

            return ModuleMetadata(
                hasOpenTelemetryAnnotations, quarkusVersion, hasQuarkusOpenTelemetry,
                springBootVersion, hasSpringBootStarterActuator, hasMicrometerTracingBridgeOtel, hasOtelExporterOtlp
            )
        }

        @JvmStatic
        fun checkQuarkus(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.quarkus"
        }

        @JvmStatic
        fun checkQuarkusOpenTelemetry(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.quarkus" && libCoord.artifactId == "quarkus-opentelemetry"
        }

        @JvmStatic
        fun checkOpenTelemetryAnnotations(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.opentelemetry.instrumentation" && libCoord.artifactId == "opentelemetry-instrumentation-annotations"
        }

        @JvmStatic
        fun checkSpringBoot(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework.boot"
        }

        @JvmStatic
        fun checkSpringBootStarterActuator(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework.boot" &&
                    libCoord.artifactId == "spring-boot-starter-actuator"
        }

        @JvmStatic
        fun checkMicrometerTracingBridgeOtel(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.micrometer" &&
                    libCoord.artifactId == "micrometer-tracing-bridge-otel"
        }

        @JvmStatic
        fun checkOtelExporterOtlp(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.opentelemetry" &&
                    libCoord.artifactId == "opentelemetry-exporter-otlp"
        }


    }

    // delay for first check for update since startup
    private val DelayMilliseconds = TimeUnit.SECONDS.toMillis(5)

    private val PeriodMilliseconds =
        TimeUnit.MINUTES.toMillis(1) // production value is 1 minutes
//        TimeUnit.SECONDS.toMillis(12) // use short period (few seconds) when debugging

    private val timer = Timer()

    @NotNull
    private var mapName2Module: ConcurrentMap<String, ModuleExt> = ConcurrentHashMap<String, ModuleExt>()

    init {
        Log.test(logger, "Initializing ${ModulesDepsService::class.simpleName}")
        val fetchTask = object : TimerTask() {
            override fun run() {
                periodicAction()
            }
        }

        timer.schedule(
            fetchTask, DelayMilliseconds, PeriodMilliseconds
        )
        Log.test(logger, "Finished ${ModulesDepsService::class.simpleName} initialization")
    }

    override fun dispose() {
        timer.cancel()
    }

    fun periodicAction() {
        mapName2Module = createModuleMap()

//        println("mapName2Module = ${mapName2Module}")
    }

    private fun createModuleMap(): ConcurrentMap<String, ModuleExt> {
        val theMap = ConcurrentHashMap<String, ModuleExt>()

        val moduleManager = ModuleManager.getInstance(project)
        moduleManager.modules.forEach { module ->
            val modName = module.name
            val moduleMetadata = buildMetadata(module)
            theMap.put(modName, ModuleExt(module, moduleMetadata))
        }
        return theMap
    }

    fun getModuleExt(moduleName: String): ModuleExt? {
        val mExt = mapName2Module.get(moduleName)
        return mExt
    }

    fun getQuarkusModulesWithoutOpenTelemetry(): Set<ModuleExt> {
        return mapName2Module.values.filter {
            it.metadata.hasQuarkus() && !it.metadata.hasQuarkusOpenTelemetry
        }
//            .map { it.module }
            .toSet()
    }

    fun getSpringBootModulesWithoutObservabilityDeps(): Set<ModuleExt> {
        return mapName2Module.values.filter {
            true
                    && !aotOrTest(it.module)
                    && it.metadata.hasSpringBoot()
                    && (false
                    || !it.metadata.hasSpringBootStarterActuator
                    || !it.metadata.hasMicrometerTracingBridgeOtel
                    || !it.metadata.hasOtelExporterOtlp
                    )
        }.toSet()
    }

    // some Gradle modules might have be non relevant ones
    fun aotOrTest(mod: Module): Boolean {
        val moduleName = mod.name

        return (false
                || moduleName.endsWith(".aot")
                || moduleName.endsWith(".aotTest")
                || moduleName.endsWith(".test")
                )
    }
}

class ModuleDepsStarter : StartupActivity {
    override fun runActivity(project: Project) {
        // its enough just to have reference to the service, and it will get initialized
        val service = ModulesDepsService.getInstance(project)
    }
}

data class ModuleMetadata(
    // quarkus
    val hasOpenTelemetryAnnotations: Boolean,
    val quarkusVersion: String?,
    val hasQuarkusOpenTelemetry: Boolean,
    // spring boot
    val springBootVersion: String?,
    val hasSpringBootStarterActuator: Boolean,
    val hasMicrometerTracingBridgeOtel: Boolean,
    val hasOtelExporterOtlp: Boolean,
    // other
) {
    fun hasQuarkus(): Boolean {
        return !quarkusVersion.isNullOrBlank()
    }

    fun hasSpringBoot(): Boolean {
        return !springBootVersion.isNullOrBlank()
    }
}

data class ModuleExt(
    val module: Module,
    val metadata: ModuleMetadata,
)
