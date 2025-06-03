package org.digma.intellij.plugin.idea.execution

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.common.retryWithBackoff
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.kotlin.ext.launchWithErrorReporting
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.paths.DigmaPathManager
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.semanticversion.SemanticVersionUtil
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.deleteIfExists

private const val JARS_DIR = "digma-otel-jars"
private const val RESOURCE_LOCATION = "otelJars"

private const val OTEL_AGENT_JAR_NAME = "opentelemetry-javaagent.jar"
private const val DIGMA_AGENT_EXTENSION_JAR_NAME = "digma-otel-agent-extension.jar"
private const val DIGMA_AGENT_JAR_NAME = "digma-agent.jar"


/**
 * provides the otel instrumentation jars and digma agent jar.
 * the jars packaged in build time to the plugin zip. the urls to download from are in file jvm-common/src/main/resources/jars-urls.properties.
 *
 * on startup the jars are unpacked to a persistence folder provided by the DigmaPathManager. the files are unpacked once and will not be unpacked
 * again unless the plugin version changes or any of the files is missing.
 *
 * it is possible to provide urls for custom jars by setting system properties. usually it is meant for development purposes but can also be used
 * for testing different versions of the jars even on user installation.
 * if provided, the custom jars are downloaded every time on startup. they need to download on every startup because the system properties may change
 * between IDE sessions.
 * the directory for custom jars is in the system temp directory and the jars will not override the persistence files.
 * the system properties are:
 * otel agent - org.digma.otel.agentUrl
 * digma otel extension - org.digma.otel.extensionUrl
 * digma agent - org.digma.otel.digmaAgentUrl
 *
 * it is also possible to provide path to local files by setting system properties. this is useful for development purposes when the jars are built
 * locally and the plugin is run from the IDE. see class OtelAgentPathProvider.
 * the system properties are:
 * otel agent - digma.otel.agent.override.path
 * digma otel extension - digma.otel.extension.override.path
 * digma agent - digma.agent.override.path
 *
 */

//Do not change to light service because it will always register.
// we want it to register only in Idea.
// see: org.digma.intellij-with-jvm.xml
@Suppress("LightServiceMigrationCode")
class OTELJarProvider(cs: CoroutineScope) {

    private val logger: Logger = Logger.getInstance(this::class.java)

    private val customFilesDownloadDir: File = File(System.getProperty("java.io.tmpdir"), JARS_DIR)
    private val downloadDir: File = File(DigmaPathManager.getLocalFilesDirectoryPath(), JARS_DIR)


    private val otelAgentJar: File = File(downloadDir, OTEL_AGENT_JAR_NAME)
    private val digmaAgentExtensionJar: File = File(downloadDir, DIGMA_AGENT_EXTENSION_JAR_NAME)
    private val digmaAgentJar: File = File(downloadDir, DIGMA_AGENT_JAR_NAME)

    private val customOtelAgentJar: File = File(customFilesDownloadDir, OTEL_AGENT_JAR_NAME)
    private val customDigmaAgentExtensionJar: File = File(customFilesDownloadDir, DIGMA_AGENT_EXTENSION_JAR_NAME)
    private val customDigmaAgentJar: File = File(customFilesDownloadDir, DIGMA_AGENT_JAR_NAME)


    private val lock = ReentrantLock(true)


    companion object {
        fun getInstance(): OTELJarProvider {
            return service<OTELJarProvider>()
        }
    }

    init {

        customOtelAgentJar.deleteOnExit()
        customDigmaAgentExtensionJar.deleteOnExit()
        customDigmaAgentJar.deleteOnExit()


        cs.launchWithErrorReporting("OTELJarProvider.init", logger) {

            //on startup check if we need to unpack the jars. it will run on IDE startup when the first project is opened
            //usually this operation will finish before the first file is requested. if a file is requested before the operation is finished it will
            // still be ok because ensureFilesExist will check if the files exist and if not will unpack them.
            //we need to support an upgrade of the plugin because it may have different jars versions. when unpacking the jars we register a property of the current
            // plugin version. if the plugin version changes we unpack the jars again.

            val currentPluginVersion = SemanticVersionUtil.getPluginVersionWithoutBuildNumberAndPreRelease("unknown")
            val lastUnpackedPluginVersion = PersistenceService.getInstance().getLastUnpackedOtelJarsPluginVersion()

            if (!persistenceFilesExist() || "unknown" == currentPluginVersion || currentPluginVersion != lastUnpackedPluginVersion) {
                Log.log(
                    logger::info,
                    "unpacking otel jars because they don't exist or plugin version has changed. currentPluginVersion: {}, lastUnpackedPluginVersion: {}",
                    currentPluginVersion,
                    lastUnpackedPluginVersion
                )
                PersistenceService.getInstance().setLastUnpackedOtelJarsPluginVersion(currentPluginVersion)
                unpackFiles()
            } else {
                Log.log(logger::info, "otel jars exist and plugin version is the same. not unpacking")
            }

            //always download custom files on startup if configured because the system properties may change between IDE sessions.
            downloadCustomJars()
        }
    }


    fun getOtelAgentJarPath(): String? {
        ensureFilesExist()
        val otelJar = getOtelAgentJar()
        if (otelJar.exists()) {
            return otelJar.absolutePath
        }
        return null
    }

    private fun getOtelAgentJar(): File {
        return if (getCustomOtelAgentJarUrl() != null) {
            customOtelAgentJar
        } else {
            otelAgentJar
        }
    }


    fun getDigmaAgentExtensionJarPath(): String? {
        ensureFilesExist()
        val digmaExtensionJar = getDigmaAgentExtensionJar()
        if (digmaExtensionJar.exists()) {
            return digmaExtensionJar.absolutePath
        }
        return null
    }

    private fun getDigmaAgentExtensionJar(): File {
        return if (getCustomDigmaAgentExtensionJarUrl() != null) {
            customDigmaAgentExtensionJar
        } else {
            digmaAgentExtensionJar
        }
    }

    fun getDigmaAgentJarPath(): String? {
        ensureFilesExist()
        val digmaAgentJar = getDigmaAgentJar()
        if (digmaAgentJar.exists()) {
            return digmaAgentJar.absolutePath
        }
        return null
    }

    private fun getDigmaAgentJar(): File {
        return if (getCustomDigmaAgentJarUrl() != null) {
            customDigmaAgentJar
        } else {
            digmaAgentJar
        }
    }


    private fun ensureFilesExist() {

        if (filesExist()) {
            return
        }

        Log.log(logger::info, "otel jars do not exists, unpacking..")

        unpackFiles()
        downloadCustomJars()
    }

    //checks if the files necessary for this session exist, some files may be the regular files and some may be custom files.
    private fun filesExist(): Boolean {
        val otelJar = getOtelAgentJar()
        val digmaExtensionJar = getDigmaAgentExtensionJar()
        val digmaAgentJar = getDigmaAgentJar()
        return otelJar.exists() && digmaExtensionJar.exists() && digmaAgentJar.exists()
    }

    //check only if the persistenceFiles Exist
    private fun persistenceFilesExist(): Boolean {
        return otelAgentJar.exists() && digmaAgentExtensionJar.exists() && digmaAgentJar.exists()
    }


    private fun unpackFiles() {

        lock.withLock {

            Log.log(logger::info, "unpacking otel agent jars")
            try {
                if (!downloadDir.exists()) {
                    if (!downloadDir.mkdirs()) {
                        Log.log(logger::warn, "could not create directory for otel jars {}", downloadDir)
                    }
                }

                if (downloadDir.exists()) {
                    copyFileFromResource(OTEL_AGENT_JAR_NAME, otelAgentJar)
                    copyFileFromResource(DIGMA_AGENT_EXTENSION_JAR_NAME, digmaAgentExtensionJar)
                    copyFileFromResource(DIGMA_AGENT_JAR_NAME, digmaAgentJar)
                    Log.log(logger::info, "otel agent jars unpacked to {}", downloadDir)
                }
                Log.log(logger::info, "unpacking otel agent jars completed")
            } catch (e: Exception) {
                Log.warnWithException(logger, e, "could not unpack otel jars.")
                ErrorReporter.getInstance().reportError("OTELJarProvider.unpackFiles", e)
            }
        }
    }


    private fun copyFileFromResource(fileName: String, toFile: File) {
        Log.log(logger::info, "extracting resource file {} to {}", fileName, toFile)
        val inputStream = this::class.java.getResourceAsStream("/$RESOURCE_LOCATION/$fileName")
        if (inputStream == null) {
            Log.log(logger::warn, "could not find file in resource folder {}", fileName)
            return
        }

        val outputStream = FileOutputStream(toFile)
        com.intellij.openapi.util.io.StreamUtil.copy(inputStream, outputStream)
        Log.log(logger::info, "resource file {} extracted to {}", fileName, toFile)
    }


    private fun downloadCustomJars() {

        lock.withLock {
            try {

                Log.log(logger::info, "downloading custom otel jars")

                if (!customFilesDownloadDir.exists()) {
                    if (!customFilesDownloadDir.mkdirs()) {
                        Log.log(logger::warn, "could not create directory for custom otel jars {}", customFilesDownloadDir)
                        ErrorReporter.getInstance().reportError(
                            "OTELJarProvider.downloadCustomJars", "could not create directory for custom otel jars",
                            mapOf()
                        )
                    }
                }

                getCustomOtelAgentJarUrl()?.let {
                    Log.log(logger::info, "downloading custom otel agent jar from {} to {}", it, customOtelAgentJar)
                    downloadAndCopyJar(URI(it).toURL(), customOtelAgentJar)
                }

                getCustomDigmaAgentExtensionJarUrl()?.let {
                    Log.log(logger::info, "downloading custom digma otel extension jar from {} to {}", it, customDigmaAgentExtensionJar)
                    downloadAndCopyJar(URI(it).toURL(), customDigmaAgentExtensionJar)
                }

                getCustomDigmaAgentJarUrl()?.let {
                    Log.log(logger::info, "downloading digma agent jar from {} to {}", it, customDigmaAgentJar)
                    downloadAndCopyJar(URI(it).toURL(), customDigmaAgentJar)
                }

                Log.log(logger::info, "downloading custom otel jars completed")

            } catch (e: Exception) {
                Log.warnWithException(logger, e, "could not download custom otel jars")
                ErrorReporter.getInstance().reportError("OTELJarProvider.downloadCustomJars", e)
            }
        }
    }


    private fun downloadAndCopyJar(url: URL, toFile: File) {

        val tempFile = kotlin.io.path.createTempFile("tempJarFile", ".jar")

        try {

            Log.log(logger::info, "downloading {} to {}", url, toFile)

            retryWithBackoff(initialDelay = 2000) {

                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                connection.getInputStream().use {
                    Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                }

                Log.log(logger::info, "copying downloaded file {} to {}", tempFile, toFile)
                try {
                    Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: Exception) {
                    //ATOMIC_MOVE is not always supported so try again on exception
                    Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }

            Log.log(logger::info, "url {} downloaded to {}", url, toFile)

        } catch (e: Exception) {
            Log.warnWithException(logger, e, "could not download file from url {}", url)
            ErrorReporter.getInstance().reportError("OTELJarProvider.downloadAndCopyJar", e)
        } finally {
            tempFile.deleteIfExists()
        }
    }


    private fun getCustomOtelAgentJarUrl(): String? {
        return System.getProperty("org.digma.otel.agentUrl")
    }

    private fun getCustomDigmaAgentExtensionJarUrl(): String? {
        return System.getProperty("org.digma.otel.extensionUrl")
    }

    private fun getCustomDigmaAgentJarUrl(): String? {
        return System.getProperty("org.digma.otel.digmaAgentUrl")
    }

}