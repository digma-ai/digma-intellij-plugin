package org.digma.intellij.plugin.updates.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.log.Log
import java.io.File
import java.io.InputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

@Service(Service.Level.APP)
class UIResourcesService {


    private val logger = Logger.getInstance(this::class.java)

    private val uiVersioningServiceStartupLock = Semaphore(0)
    private val startupCompleted: AtomicBoolean = AtomicBoolean(false)

    companion object {
        @JvmStatic
        fun getInstance(): UIResourcesService {
            return service<UIResourcesService>()
        }
    }

    fun startupCompleted() {
        Log.log(logger::info,"startup completed")
        startupCompleted.set(true)
        uiVersioningServiceStartupLock.release()
    }

    private fun waitForUiStartupToComplete() {

        if (startupCompleted.get()){
            return
        }

        Log.log(logger::info,"waiting for startup to complete")
        uiVersioningServiceStartupLock.acquire()
        Log.log(logger::info,"done waiting for startup to complete")
    }

    fun isResourceExists(resourcePath: String): Boolean {
        waitForUiStartupToComplete()

        val uiBundlePath = getUIBundlePath()

        return ZipFile(uiBundlePath).use { zipFile ->
            val entry = zipFile.getEntry(resourcePath)
            entry != null
        }
    }

    fun getResourceAsStream(resourcePath: String): InputStream? {
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


    private fun getUIBundlePath():String{
        //todo: support also downloading from url
        return System.getProperty("org.digma.plugin.ui.bundle.path") ?:UIVersioningService.getInstance().getCurrentUiBundlePath()
    }

}