package org.digma.intellij.plugin.updates.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import org.apache.maven.artifact.versioning.ComparableVersion
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.Retries
import org.digma.intellij.plugin.common.buildVersionRequest
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.common.newerThan
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.paths.DigmaPathManager
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.reload.ReloadService
import org.digma.intellij.plugin.reload.ReloadSource
import org.digma.intellij.plugin.scheduling.disposingPeriodicTask
import org.digma.intellij.plugin.settings.InternalFileSettings
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.APP)
class UIVersioningService(val cs: CoroutineScope) : DisposableAdaptor {

    private val logger = Logger.getInstance(this::class.java)

    private val bundledUiVersion = getBundledUiVersion()

    companion object {

        private const val UNKNOWN_UI_VERSION = "unknown"
        private const val BUNDLE_UI_DIR_PATH = "ui-bundle"
        private const val UI_VERSION_FILE = "/${BUNDLE_UI_DIR_PATH}/ui-version"
        private const val UI_BUNDLE_FILE_NAME_PREFIX = "digma-ui"
        private val UI_BUNDLES_LOCAL_DIR = File(DigmaPathManager.getLocalFilesDirectoryPath(), "ui")

        @JvmStatic
        fun getInstance(): UIVersioningService {
            return service<UIVersioningService>()
        }

        //do it once on startup instead of reading the file each time the bundled ui version is needed
        private fun getBundledUiVersion(): String {
            try {
                val resourceAsStream = this::class.java.getResourceAsStream(UI_VERSION_FILE)
                //should not happen
                if (resourceAsStream == null) {
                    Log.log(Logger.getInstance(UIVersioningService::class.java)::warn, "can not load ui version file from $UI_VERSION_FILE")
                    ErrorReporter.getInstance()
                        .reportError("UIVersioningService.getBundledUiVersion", "can not load ui version file from $UI_VERSION_FILE", mapOf())
                    return UNKNOWN_UI_VERSION
                }
                return resourceAsStream.bufferedReader().use { it.readText() }
            } catch (e: Throwable) {
                ErrorReporter.getInstance()
                    .reportError("UIVersioningService.getBundledUiVersion", "can not load ui version file from $UI_VERSION_FILE", mapOf())
                return UNKNOWN_UI_VERSION
            }
        }

        fun getDefaultDelayBetweenUpdatesSeconds(): Duration {
            return InternalFileSettings.getUIVersioningServiceMonitorDelaySeconds(300).seconds
        }
    }


    init {

        UI_BUNDLES_LOCAL_DIR.mkdirs()

        doStartup()
        //startMonitoring is called from constructor, it will be called only once per IDE session
        startMonitoring()
    }

    fun isNewUIBundleAvailable(): Boolean {
        return getLatestDownloadedVersion()?.let {
            buildUiBundleLocalFile(it).exists()
        } ?: false
    }

    fun getCurrentUiBundlePath(): String {
        return buildUiBundleLocalFile(getCurrentUiVersion()).absolutePath
    }


    fun getUiVersionForVersionRequest(): String {
        return getCurrentUiVersion()
    }

    fun getCurrentUiVersion(): String {
        return PersistenceService.getInstance().getCurrentUiVersion() ?: bundledUiVersion
    }

    private fun setCurrentUiVersion(uiVersion: String) {
        return PersistenceService.getInstance().setCurrentUiVersion(uiVersion)
    }

    fun getLatestDownloadedVersion(): String? {
        return PersistenceService.getInstance().getLatestDownloadedUiVersion()
    }

    private fun setLatestDownloadedVersion(uiVersion: String?) {
        return PersistenceService.getInstance().setLatestDownloadedUiVersion(uiVersion)
    }


    private fun buildUiBundleLocalFile(version: String): File {
        return File(UI_BUNDLES_LOCAL_DIR, "$UI_BUNDLE_FILE_NAME_PREFIX-$version.zip")
    }

    private fun doStartup() {
        validateUiBundleExists()
        startupCompleted()
    }

    //this method will make sure we have a UI bundle available for work.
    //if there is a new version that was already downloaded it will update to use the new version
    private fun validateUiBundleExists() {

        try {

            //update this property so it has a value on the first installation of the ui versioning feature
            if (PersistenceService.getInstance().getCurrentUiVersion() == null) {
                setCurrentUiVersion(bundledUiVersion)
            }

            //Note:always use the methods getCurrentUiVersion() and getLatestDownloadedVersion() and don't assign to local variables
            // because values may change concurrently

            Log.log(
                logger::info,
                "validating ui bundle current version: {}, bundled version: {}, latest downloaded version {}",
                getCurrentUiVersion(),
                bundledUiVersion,
                getLatestDownloadedVersion()
            )


            //if we have the latest downloaded file, switch to use it and delete the old version
            val latestDownloadedUiVersion = getLatestDownloadedVersion()
            if (latestDownloadedUiVersion != null) {
                Log.log(
                    logger::info,
                    "updating ui to latest downloaded version {}", latestDownloadedUiVersion
                )
                val latestDownloadedUi = buildUiBundleLocalFile(latestDownloadedUiVersion)
                if (latestDownloadedUi.exists()) {
                    deleteUiBundle(getCurrentUiVersion())
                    setCurrentUiVersion(latestDownloadedUiVersion)
                    setLatestDownloadedVersion(null)
                } else {
                    //something is wrong, we have the property latestDownloadedVersion but there is no file, maybe it was deleted.
                    //reset latestDownloadedVersion
                    Log.log(
                        logger::warn,
                        "latest downloaded version property exists but file does not exist, not updating"
                    )

                    setLatestDownloadedVersion(null)
                }
            }


            //maybe user updated the plugin, and the bundled ui is newer then the current ui version.
            //this is a valid check even if we had a latestDownloadedVersion and we switched to it above
            if (ComparableVersion(bundledUiVersion).newerThan(ComparableVersion(getCurrentUiVersion()))) {
                Log.log(
                    logger::info,
                    "bundled ui version is higher then current version, using bundled ui. current version: {}, bundled version: {}",
                    getCurrentUiVersion(),
                    bundledUiVersion
                )
                if (unpackUiBundle()) {
                    deleteUiBundle(getCurrentUiVersion())
                    setCurrentUiVersion(bundledUiVersion)
                } else {
                    Log.log(logger::warn, "could not unpack bundled ui version {}", bundledUiVersion)
                }
            }


            //lastly, validate that we have a ui bundle file.
            //on the first installation of the feature, this should unpack the bundled version
            val uiBundleLocalFile = buildUiBundleLocalFile(getCurrentUiVersion())
            if (uiBundleLocalFile.exists()) {
                Log.log(logger::info, "ui bundle file exists {}", uiBundleLocalFile.absolutePath)
            } else {
                Log.log(
                    logger::warn,
                    "current ui bundle {} file does not exists ,using the packaged ui bundle {}",
                    getCurrentUiVersion(),
                    bundledUiVersion
                )
                unpackUiBundle()
                setCurrentUiVersion(bundledUiVersion)
            }

        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "failed validating ui bundle file")
            ErrorReporter.getInstance().reportError("UIVersioningService.validateUiBundleExistsAndUpdatePath", e)

            //we don't know what went wrong, So as fallback use the packaged ui bundle
            deleteUiBundle(getCurrentUiVersion())
            getLatestDownloadedVersion()?.let {
                deleteUiBundle(it)
                setLatestDownloadedVersion(null)
            }

            unpackUiBundle()
            setCurrentUiVersion(bundledUiVersion)

        }
    }


    private fun startMonitoring() {

        val delayMillis = getDefaultDelayBetweenUpdatesSeconds().inWholeMilliseconds
        disposingPeriodicTask("UIVersioningService.periodic", 1.minutes.inWholeMilliseconds, delayMillis, true) {
            findActiveProject()?.let { project ->
                try {

                    val versionsResponse = AnalyticsService.getInstance(project).getVersions(buildVersionRequest())

                    Log.log(logger::trace, "got version response {}", versionsResponse)

                    if (versionsResponse.forceUpdate?.minUIVersionRequired != null) {
                        val requiredUiVersion = versionsResponse.forceUpdate?.minUIVersionRequired
                        val currentUiVersion = getCurrentUiVersion()
                        if (!requiredUiVersion.isNullOrBlank() && ComparableVersion(requiredUiVersion).newerThan(ComparableVersion(currentUiVersion))) {
                            Log.log(logger::info, "got ui force update to {}", requiredUiVersion)
                            //if there is already a latest downloaded version, delete it before downloading the new one
                            // and reset the property
                            getLatestDownloadedVersion()?.let {
                                deleteUiBundle(it)
                                setLatestDownloadedVersion(null)
                            }
                            if (downloadUiBundle(requiredUiVersion)) {
                                updateToDownloadedVersion(requiredUiVersion, true)
                            }
                        }
                    } else if (versionsResponse.ui?.isNewMatchingVersionAvailable == true) {
                        val requiredUiVersion = versionsResponse.ui?.latestMatchingVersion
                        val currentUiVersion = getCurrentUiVersion()
                        if (!requiredUiVersion.isNullOrBlank() && ComparableVersion(requiredUiVersion).newerThan(ComparableVersion(currentUiVersion))) {
                            Log.log(logger::info, "got ui update to {}", requiredUiVersion)

                            //if LatestDownloadedVersion equals requiredUiVersion then it was already downloaded in the previous round,
                            // so no need to download it again.
                            if (getLatestDownloadedVersion() != requiredUiVersion) {
                                Log.log(
                                    logger::info,
                                    "requiredUiVersion {} is different from latest downloaded {}, downloading.. ",
                                    requiredUiVersion,
                                    getLatestDownloadedVersion()
                                )
                                //if there is already a latest downloaded version, delete it before downloading the new one
                                // and reset the property
                                getLatestDownloadedVersion()?.let {
                                    deleteUiBundle(it)
                                    setLatestDownloadedVersion(null)
                                }
                                if (downloadUiBundle(requiredUiVersion)) {
                                    setLatestDownloadedVersion(requiredUiVersion)
                                    fireNewUIVersionAvailable()
                                }
                            } else {
                                Log.log(
                                    logger::info,
                                    "requiredUiVersion {} is the same as latest downloaded {}, no need to download again ",
                                    requiredUiVersion,
                                    getLatestDownloadedVersion()
                                )
                            }
                        }
                    }

                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "failed on periodic update")
                    ErrorReporter.getInstance().reportError("UIVersioningService.periodic", e)
                }
            }
        }
    }

    private fun fireNewUIVersionAvailable() {
        ApplicationManager.getApplication().messageBus.syncPublisher(NewUIVersionAvailableEvent.NEW_UI_VERSION_AVAILABLE_EVENT_TOPIC).newUIVersionAvailable()
    }


    fun updateToLatestDownloaded() {
        val latestDownloadedUiVersion = getLatestDownloadedVersion()
        Log.log(logger::trace, "updating ui to latest downloaded version {}", latestDownloadedUiVersion)
        if (latestDownloadedUiVersion != null) {
            val latestDownloadedUi = buildUiBundleLocalFile(latestDownloadedUiVersion)
            if (latestDownloadedUi.exists()) {
                updateToDownloadedVersion(latestDownloadedUiVersion)
                setLatestDownloadedVersion(null)
            } else {
                //something is wrong, we have the property latestDownloadedVersion, but there is no file, maybe it was deleted.
                //reset latestDownloadedVersion
                ErrorReporter.getInstance().reportError(
                    "UIVersioningService.updateToLatestDownloaded",
                    "updateToLatestDownloaded called but ui bundle file does not exist", mapOf(
                        "current ui version" to getCurrentUiVersion(),
                        "latest downloaded version" to getLatestDownloadedVersion().toString(),
                        "bundled version" to bundledUiVersion,
                    )
                )
                Log.log(
                    logger::warn,
                    "latest downloaded version property exists but file does not exist, not updating"
                )
                setLatestDownloadedVersion(null)
            }
        }
    }

    private fun updateToDownloadedVersion(uiVersion: String, isForceUpdate: Boolean = false) {

        //check that uiVersion is different from the current version, it should not happen, if it does its bug.
        // in that case, don't delete the current UI and report an error
        if (uiVersion == getCurrentUiVersion()) {
            ErrorReporter.getInstance().reportError(
                "UIVersioningService.updateToDownloadedVersion", "trying to force update ui to same version as current version",
                mapOf(
                    "current ui version" to getCurrentUiVersion(),
                    "latest downloaded version" to getLatestDownloadedVersion().toString(),
                    "bundled version" to bundledUiVersion,
                    "isForceUpdate" to isForceUpdate
                )
            )
            return
        }

        val downloadedUi = buildUiBundleLocalFile(uiVersion)
        if (downloadedUi.exists()) {

            Log.log(logger::trace, "updating ui to downloaded version {}, isForceUpdate {}", uiVersion, isForceUpdate)

            findActiveProject()?.let {
                ActivityMonitor.getInstance(it).registerCustomEvent(
                    "ui update", mapOf(
                        "current ui version" to getCurrentUiVersion(),
                        "latest downloaded version" to getLatestDownloadedVersion().toString(),
                        "bundled version" to bundledUiVersion,
                        "isForceUpdate" to isForceUpdate
                    )
                )
            }

            deleteUiBundle(getCurrentUiVersion())
            setCurrentUiVersion(uiVersion)
        }

        service<ReloadService>().reloadAllProjects(ReloadSource.UI_UPDATE)
    }


    private fun startupCompleted() {
        UIResourcesService.getInstance().startupCompleted()
    }


    private fun deleteUiBundle(uiVersion: String) {
        Log.log(logger::info, "deleting old ui bundle {}", uiVersion)
        val uiBundleLocalFile = buildUiBundleLocalFile(uiVersion)
        if (!uiBundleLocalFile.exists()) {
            Log.log(logger::info, "old ui bundle {} does not exist", uiVersion)
            return
        }
        try {
            Retries.simpleRetry(kotlinx.coroutines.Runnable {
                Files.delete(uiBundleLocalFile.toPath())
            }, Throwable::class.java, 1000, 3)
            Log.log(logger::info, "ui bundle {} deleted", uiVersion)
        } catch (e: Throwable) {
            Log.warnWithException(logger, e, "failed to delete ui bundle file")
            ErrorReporter.getInstance().reportError("UIVersioningService.deleteUiBundle", e)
        }
    }


    //return true on success, false on failure
    private fun downloadUiBundle(requiredUiVersion: String): Boolean {
        Log.log(logger::info, "downloading ui bundle {}", requiredUiVersion)
        val url = "https://github.com/digma-ai/digma-ui/releases/download/v$requiredUiVersion/dist-jetbrains-v$requiredUiVersion.zip"
        val uiBundleLocalFile = buildUiBundleLocalFile(requiredUiVersion)
        return downloadAndCopyUiBundleFile(URI(url).toURL(), uiBundleLocalFile)
    }


    private fun unpackUiBundle(): Boolean {

        Log.log(logger::info, "unpacking bundled ui bundle {}", bundledUiVersion)

        val inputStream = this::class.java.getResourceAsStream("/$BUNDLE_UI_DIR_PATH/$UI_BUNDLE_FILE_NAME_PREFIX-${bundledUiVersion}.zip")
        if (inputStream == null) {
            Log.log(logger::warn, "can not find bundled ui zip")
            ErrorReporter.getInstance()
                .reportError("UIVersioningService.unpackUiBundle", "can not find bundled ui zip", mapOf())
            return false
        }

        val uiBundleLocalFile = buildUiBundleLocalFile(bundledUiVersion)
        uiBundleLocalFile.parentFile.mkdirs()
        inputStream.use { inStream ->
            Files.copy(inStream, uiBundleLocalFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        Log.log(logger::info, "bundled ui {} unpacked to {}", bundledUiVersion, uiBundleLocalFile.absolutePath)

        return true
    }


    private fun downloadAndCopyUiBundleFile(url: URL, toFile: File): Boolean {

        val tempFile = kotlin.io.path.createTempFile("tempUiBundleFile", ".zip")

        try {

            Retries.simpleRetry({

                Log.log(logger::info, "downloading ui bundle {}", url)

                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                val responseCode: Int = connection.getResponseCode()

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    ErrorReporter.getInstance().reportError(
                        null, "UIVersioningService.downloadAndCopyUiBundleFile",
                        "download from $url",
                        mapOf("responseCode" to responseCode.toString())
                    )
                    Log.log(logger::warn, "error downloading ui bundle {}, response code {}", url, responseCode)
                    throw RuntimeException("could not download file from $url, response code $responseCode")
                } else {
                    connection.inputStream.use {
                        Files.copy(it, tempFile, StandardCopyOption.REPLACE_EXISTING)
                    }

                    Log.log(logger::info, "ui bundle downloaded to {}", tempFile)

                    toFile.mkdirs()

                    Log.log(logger::info, "copying downloaded file {} to {}", tempFile, toFile)
                    try {
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    } catch (e: Exception) {
                        //ATOMIC_MOVE is not always supported so try again on exception
                        Files.move(tempFile, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }

                    Log.log(logger::info, "ui bundle saved to {}", toFile)
                }
            }, Throwable::class.java, 5000, 3)

            return true

        } catch (e: Exception) {
            ErrorReporter.getInstance().reportError("UIVersioningService.downloadAndCopyUiBundleFile", e)
            Log.warnWithException(logger, e, "could not download file {}", url)
            return false
        } finally {
            tempFile.deleteIfExists()
        }
    }
}