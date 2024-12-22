package org.digma.intellij.plugin.updates.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.zip.ZipFile

@Service(Service.Level.APP)
class UIResourcesService {


    private val logger = Logger.getInstance(this::class.java)

    private val uiVersioningServiceStartupLock = CountDownLatch(1)

    companion object {
        @JvmStatic
        fun getInstance(): UIResourcesService {
            return service<UIResourcesService>()
        }
    }

    fun startupCompleted() {
        Log.log(logger::info, "startup completed")
        uiVersioningServiceStartupLock.countDown()
    }

    private fun waitForUiStartupToComplete() {

        if (uiVersioningServiceStartupLock.count == 0L) {
            return
        }

        Log.log(logger::info, "waiting for startup to complete")
        uiVersioningServiceStartupLock.await()
        Log.log(logger::info, "done waiting for startup to complete")
    }

    fun isResourceExists(resourcePath: String): Boolean {

        Log.log(logger::info, "request for isResourceExists {}",resourcePath)

        waitForUiStartupToComplete()

        val uiBundlePath = getUIBundlePath()

        return ZipFile(uiBundlePath).use { zipFile ->
            val entry = zipFile.getEntry(resourcePath)
            entry != null
        }
    }

    fun getResourceAsStream(resourcePath: String): InputStream? {

        Log.log(logger::info, "request for getResourceAsStream {}",resourcePath)

        waitForUiStartupToComplete()

        val uiBundlePath = getUIBundlePath()

        return ZipFile(uiBundlePath).use { zipFile ->
            val entry = zipFile.getEntry(resourcePath)
            entry?.let {
                val inputStream = zipFile.getInputStream(it)
                val tempFile = File.createTempFile("digmatmp", null)
                tempFile.deleteOnExit()
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.inputStream()
            }
        }
    }


    private fun getUIBundlePath(): String {
        //todo: support also downloading from url

        val localUiFilePath = System.getProperty("org.digma.plugin.ui.bundle.path")
        return if (localUiFilePath != null) {
            Log.log(logger::trace, "Using local UI bundle from {}", localUiFilePath)
            localUiFilePath
        } else {
            val uiBundlePath = UIVersioningService.getInstance().getCurrentUiBundlePath()
            Log.log(logger::trace, "Using UI bundle from {}", uiBundlePath)
            uiBundlePath
        }
    }

}