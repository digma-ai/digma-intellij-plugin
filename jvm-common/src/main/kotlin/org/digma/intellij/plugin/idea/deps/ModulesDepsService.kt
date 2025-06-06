package org.digma.intellij.plugin.idea.deps

import com.intellij.buildsystem.model.unified.UnifiedCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.idea.frameworks.SpringBootMicrometerConfigureDepsService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredFramework
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.startup.DigmaProjectActivity
import org.jetbrains.annotations.NotNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("LightServiceMigrationCode")
class ModulesDepsService(private val project: Project) : Disposable {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ModulesDepsService {
            return project.getService(ModulesDepsService::class.java)
        }

        @JvmStatic
        fun toUnifiedCoordinates(lib: LibraryOrderEntry): UnifiedCoordinates {
            // Gradle library name for example : "Gradle: org.aspectj:aspectjweaver:1.8.14"
            // Maven  library name for example : "Maven: com.google.guava:guava:32.0.1"

            return lib.libraryName?.let {
                val splitSpaces = it.split(' ')
                val netoLibName = splitSpaces.last().trim()

                val splitNames = netoLibName.split(':')
                val groupId = splitNames[0]
                val artifactId: String? = if (splitNames.size > 1) {
                    splitNames[1]
                } else {
                    null
                }

                val version: String? = if (splitNames.size > 2) {
                    splitNames[2]
                } else {
                    null
                }

                UnifiedCoordinates(groupId, artifactId, version)

            } ?: UnifiedCoordinates(null, null, null)


//            val splitSpaces = lib.libraryName!!.split(' ')
//            val netoLibName = splitSpaces.last().trim()
//
//            val splitNames = netoLibName.split(':')
//            val groupId = splitNames[0]
//            val artifactId = splitNames[1]
//            var version: String? = null
//            if (splitNames.size >= 3) {
//                version = splitNames[2]
//            }
//            return UnifiedCoordinates(groupId, artifactId, version)
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
            var hasSpringBootStarterAop = false
            var hasMicrometerTracingBridgeOtel = false
            var hasDatasourceMicrometerSpringBoot = false
            var hasOtelExporterOtlp = false
            var hasDigmaSpringBootMicrometerAutoconf = false
            var springBootVersion: String? = null
            var micronautVersion: String? = null
            var dropwizardVersion: String? = null
            var springVersion: String? = null
            var hasJerseyMicrometer = false
            var hasSpringBootStarterJersey = false


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
                if (!hasSpringBootStarterAop) {
                    hasSpringBootStarterAop = checkSpringBootStarterAop(libCoord)
                }
                if (!hasMicrometerTracingBridgeOtel) {
                    hasMicrometerTracingBridgeOtel = checkMicrometerTracingBridgeOtel(libCoord)
                }
                if (!hasDatasourceMicrometerSpringBoot) {
                    hasDatasourceMicrometerSpringBoot = checkDatasourceMicrometerSpringBoot(libCoord)
                }
                if (!hasOtelExporterOtlp) {
                    hasOtelExporterOtlp = checkOtelExporterOtlp(libCoord)
                }
                // spring boot + digma (aspect for @Observed)
                if (!hasDigmaSpringBootMicrometerAutoconf) {
                    hasDigmaSpringBootMicrometerAutoconf = checkDigmaSpringBootMicrometerAutoconf(libCoord)
                }

                if (!hasJerseyMicrometer) {
                    hasJerseyMicrometer = checkJerseyMicrometer(libCoord)
                }

                if (!hasSpringBootStarterJersey) {
                    hasSpringBootStarterJersey = checkSpringBootStarterJersey(libCoord)
                }



                // Micronaut
                if (micronautVersion == null) {
                    val hasMicronaut = checkMicronaut(libCoord)
                    if (hasMicronaut) {
                        micronautVersion = libCoord.version
                    }
                }

                // Dropwizard
                if (dropwizardVersion == null) {
                    val hasDropwizard = checkDropwizard(libCoord)
                    if (hasDropwizard) {
                        dropwizardVersion = libCoord.version
                    }
                }

                // Spring
                if (springVersion == null) {
                    val hasSpring = checkSpring(libCoord)
                    if (hasSpring) {
                        springVersion = libCoord.version
                    }
                }
            }

            return ModuleMetadata(
                hasOpenTelemetryAnnotations, quarkusVersion, hasQuarkusOpenTelemetry,
                springBootVersion, hasSpringBootStarterActuator, hasSpringBootStarterAop,
                hasMicrometerTracingBridgeOtel, hasDatasourceMicrometerSpringBoot, hasOtelExporterOtlp,
                hasDigmaSpringBootMicrometerAutoconf,hasJerseyMicrometer,hasSpringBootStarterJersey,
                micronautVersion, dropwizardVersion, springVersion
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
        fun checkMicronaut(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.micronaut"
        }

        @JvmStatic
        fun checkDropwizard(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.dropwizard"
        }

        @JvmStatic
        fun checkSpring(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework"
        }

        @JvmStatic
        fun checkSpringBootStarterActuator(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework.boot" &&
                    libCoord.artifactId == "spring-boot-starter-actuator"
        }

        @JvmStatic
        fun checkJerseyMicrometer(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == SpringBootMicrometerConfigureDepsService.JerseyMicrometerCoordinates.groupId &&
                    libCoord.artifactId == SpringBootMicrometerConfigureDepsService.JerseyMicrometerCoordinates.artifactId
        }

        @JvmStatic
        fun checkSpringBootStarterAop(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework.boot" &&
                    libCoord.artifactId == "spring-boot-starter-aop"
        }

        @JvmStatic
        fun checkSpringBootStarterJersey(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "org.springframework.boot" &&
                    libCoord.artifactId == "spring-boot-starter-jersey"
        }

        @JvmStatic
        fun checkMicrometerTracingBridgeOtel(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.micrometer" &&
                    libCoord.artifactId == "micrometer-tracing-bridge-otel"
        }

        @JvmStatic
        fun checkDatasourceMicrometerSpringBoot(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "net.ttddyy.observation" &&
                    libCoord.artifactId == "datasource-micrometer-spring-boot"
        }

        @JvmStatic
        fun checkOtelExporterOtlp(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.opentelemetry" &&
                    libCoord.artifactId == "opentelemetry-exporter-otlp"
        }

        @JvmStatic
        fun checkDigmaSpringBootMicrometerAutoconf(libCoord: UnifiedCoordinates): Boolean {
            return libCoord.groupId == "io.github.digma-ai" &&
                    libCoord.artifactId == "digma-spring-boot-micrometer-tracing-autoconf"
        }

    }

    @NotNull
    private var mapName2Module: ConcurrentMap<String, ModuleExt> = ConcurrentHashMap()

    init {

        disposingPeriodicTask("", 5.seconds.inWholeMilliseconds, 1.minutes.inWholeMilliseconds, false) {
            try {
                periodicAction()
            } catch (e: Exception) {
                ErrorReporter.getInstance().reportError(project, "ModulesDepsService.periodicAction", e)
            }
        }
    }

    override fun dispose() {
    }

    private fun periodicAction() {
        val newMap = createModuleMap()
        notifyOfNewDetections(newMap.values)
        mapName2Module = newMap
    }

    private fun createModuleMap(): ConcurrentMap<String, ModuleExt> {
        val theMap = ConcurrentHashMap<String, ModuleExt>()

        val moduleManager = ModuleManager.getInstance(project)
        moduleManager.modules.forEach { module ->
            val modName = module.name
            val moduleMetadata = buildMetadata(module)
            theMap[modName] = ModuleExt(module, moduleMetadata)
        }
        return theMap
    }

    private fun notifyOfNewDetections(newValues: Collection<ModuleExt>) {
        val previousValues = mapName2Module.values
        val activityMonitor = ActivityMonitor.getInstance(project)

        fun changedToTrue(getter: (ModuleMetadata) -> Boolean): Boolean {
            return !previousValues.any { getter(it.metadata) } && newValues.any { getter(it.metadata) }
        }

        if (changedToTrue { it.hasSpring() }) {
            activityMonitor.registerFramework(MonitoredFramework.Spring)
        }

        if (changedToTrue { it.hasSpringBoot() }) {
            activityMonitor.registerFramework(MonitoredFramework.SpringBoot)
        }

        if (changedToTrue { it.hasMicronaut() }) {
            activityMonitor.registerFramework(MonitoredFramework.Micronaut)
        }

        if (changedToTrue { it.hasDropwizard() }) {
            activityMonitor.registerFramework(MonitoredFramework.Dropwizard)
        }

        if (changedToTrue { it.hasQuarkus() }) {
            activityMonitor.registerFramework(MonitoredFramework.Quarkus)
        }
    }

    fun getModuleExt(moduleName: String): ModuleExt? {
        var mExt = mapName2Module[moduleName]
        if (mExt == null) {
//            println("DBG: module '${moduleName}' - metadata not built yet, building it...")
            //try to build MD for this entry
            mExt = tryBuildAndStoreModuleExt(moduleName)
        } else {
//            println("DBG: module '${moduleName}' - metadata already built")
        }
        return mExt
    }

    private fun tryBuildAndStoreModuleExt(moduleName: String): ModuleExt? {
        val moduleManager = ModuleManager.getInstance(project)
        val module = moduleManager.findModuleByName(moduleName) ?: return null

        val moduleMetadata = buildMetadata(module)
        val moduleExt = ModuleExt(module, moduleMetadata)
        mapName2Module[moduleName] = moduleExt
//        println("DBG: module '${moduleName}' - built moduleExt = $moduleExt")
        return moduleExt
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
                    && !isModuleHasNeededDependenciesForSpringBootWithMicrometer(it.metadata)
        }.toSet()
    }

    fun isModuleHasNeededDependenciesForSpringBootWithMicrometer(metadata: ModuleMetadata): Boolean {
        val retval = (true
                && metadata.hasSpringBootStarterActuator
                && metadata.hasSpringBootStarterAop
                && metadata.hasMicrometerTracingBridgeOtel
                && metadata.hasDatasourceMicrometerSpringBoot
                && metadata.hasOtelExporterOtlp
                && metadata.hasDigmaSpringBootMicrometerAutoconf
                )
        return retval
    }

    fun isSpringBootModule(module: Module): Boolean {
        val metadata = buildMetadata(module)
        return metadata.hasSpringBoot()
    }

    fun isQuarkusModule(module: Module): Boolean {
        val metadata = buildMetadata(module)
        return metadata.hasQuarkus()
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

class ModuleDepsStarter : DigmaProjectActivity() {
    override fun executeProjectStartup(project: Project) {
        // its enough just call getInstance and it will be initialized
        ModulesDepsService.getInstance(project)
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
    val hasSpringBootStarterAop: Boolean,
    val hasMicrometerTracingBridgeOtel: Boolean,
    val hasDatasourceMicrometerSpringBoot: Boolean,
    val hasOtelExporterOtlp: Boolean,
    val hasDigmaSpringBootMicrometerAutoconf: Boolean,
    val hasJerseyMicrometer: Boolean,
    val hasSpringBootStarterJersey: Boolean,
    // other
    val micronautVersion: String?,
    val dropwizardVersion: String?,
    val springVersion: String?,
) {
    fun hasQuarkus(): Boolean {
        return !quarkusVersion.isNullOrBlank()
    }

    fun hasSpringBoot(): Boolean {
        return !springBootVersion.isNullOrBlank()
    }

    fun hasMicronaut(): Boolean {
        return !micronautVersion.isNullOrBlank()
    }

    fun hasDropwizard(): Boolean {
        return !dropwizardVersion.isNullOrBlank()
    }

    fun hasSpring(): Boolean {
        return !springVersion.isNullOrBlank()
    }
}

data class ModuleExt(
    val module: Module,
    val metadata: ModuleMetadata,
)
