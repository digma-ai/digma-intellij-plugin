package org.digma.intellij.plugin.paths

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import org.apache.commons.codec.digest.DigestUtils
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.io.File

const val DIGMA_DIR = "digma-intellij-plugin"

class DigmaPathManager {


    companion object {

        private val logger = Logger.getInstance(this::class.java)

        /**
         * returns the base directory to save files belonging to digma installation
         */
        fun getLocalFilesDirectoryPath(): String {

            Log.log(logger::trace, "getLocalFilesDirectoryPath called")

            val ideFullName = ApplicationNamesInfo.getInstance().fullProductNameWithEdition
            val ideHomeDir = PathManager.getHomePath()
            val ideHomeDirHash = DigestUtils.sha1Hex(ideHomeDir)
            val ideName = "$ideFullName-$ideHomeDirHash".replace(" ", "-").replace(".", "-")

            Log.log(
                logger::trace,
                "building ide name from: ideFullName={},ideHomeDir={},ideHomeDirHash={}. resulting ide name is: {}",
                ideFullName,
                ideHomeDir,
                ideHomeDirHash,
                ideName
            )

            return try {
                val baseDir = getBaseDirectory()
                val ideDir = File(baseDir, ideName)
                ideDir.mkdirs()
                Log.log(logger::trace, "getLocalFilesDirectoryPath created ide dir {}", ideDir.absolutePath)
                ideDir.absolutePath
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("DigmaPathManager.getLocalFilesDirectoryPath", e)
                val fallback = File(System.getProperty("java.io.tmpdir"), "$DIGMA_DIR/$ideName")
                fallback.mkdirs()
                Log.warnWithException(logger, e, "getLocalFilesDirectoryPath failed, using fallback location {}", fallback.absolutePath)
                fallback.absolutePath
            }
        }


        private fun getUserHome(): String {
            //user.home should never be null, but just in case,use temp dir
            return System.getProperty("user.home") ?: System.getProperty("java.io.tmpdir")
        }


        private fun getBaseDirectory(): File {

            Log.log(logger::trace, "getBaseDirectory called")

            return try {
                if (SystemInfo.isMac) {
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, "Library/Application Support/$DIGMA_DIR")
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is mac, using base directory {}",digmaDir)
                    digmaDir
                } else if (SystemInfo.isWindows) {
                    val userHome = System.getenv("LOCALAPPDATA") ?: getUserHome()
                    val digmaDir = File(userHome, DIGMA_DIR)
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is windows, using base directory {}",digmaDir)
                    digmaDir
                } else {
                    //for linux or any other os
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, ".$DIGMA_DIR")
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is ${SystemInfo.OS_NAME}, using base directory {}",digmaDir)
                    digmaDir
                }

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("DigmaPathManager.getBaseDirectory", e)
                val userHome = getUserHome()
                val digmaDir = File(userHome, ".$DIGMA_DIR")
                digmaDir.mkdirs()
                Log.warnWithException(logger, e, "getBaseDirectory failed, using fallback location {}", digmaDir)
                digmaDir
            }
        }
    }
}