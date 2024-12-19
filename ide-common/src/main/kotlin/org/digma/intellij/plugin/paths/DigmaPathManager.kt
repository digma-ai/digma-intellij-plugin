package org.digma.intellij.plugin.paths

import com.intellij.openapi.application.ApplicationInfo
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

            val ideName = createIdeName()

            return try {
                val baseDir = getBaseDirectory()
                val ideDir = File(baseDir, ideName)
                ideDir.mkdirs()
                Log.log(logger::trace, "getLocalFilesDirectoryPath created ide dir {}", ideDir.absolutePath)
                val ideInfo = File(ideDir, "ide.info")
                if (!ideInfo.exists()) {
                    val ideInfoText =
                        "${ApplicationNamesInfo.getInstance().fullProductNameWithEdition} ${ApplicationInfo.getInstance().fullVersion} (${PathManager.getHomePath()})"
                    ideInfo.writeText(ideInfoText)
                }

                ideDir.absolutePath
            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("DigmaPathManager.getLocalFilesDirectoryPath", e)
                val fallback = File(System.getProperty("java.io.tmpdir"), "$DIGMA_DIR/$ideName")
                fallback.mkdirs()
                Log.warnWithException(logger, e, "getLocalFilesDirectoryPath failed, using fallback location {}", fallback.absolutePath)
                fallback.absolutePath
            }
        }


        private fun createIdeName(): String {

            //when running the IDE with gradle runIde task, it may happen that PathManager.getHomePath() will return different location
            // every time or every few runs. this is an issue of intellij platform gradle plugin that transforms the IDE too many times.
            // for example it will transform to /home/shalom/.gradle/caches/8.11/transforms/be685aabc5c1d8430f8d0bbf869a65da/transformed/ideaIC-2024.3.1
            // and after few builds may transform to another location.
            // this will result in different ideName every time and will cause the plugin to create new directories every time. over time there
            // will be many directories for the same IDE.
            //so the solution is to use the version to create the ideName, it should be good enough for development mode when running with runIde.
            //the property org.digma.plugin.DigmaPathManager.dev.runIde is set in the runIde task in build.gradle.kts.
            val isRunIde = System.getProperty("org.digma.plugin.DigmaPathManager.dev.runIde")?.toBoolean() ?: false

            return if (isRunIde) {

                Log.log(logger::trace, "isRunIde is true, creating ideName for development mode")

                val ideFullName = ApplicationNamesInfo.getInstance().fullProductNameWithEdition
                val ideVersion = "${ApplicationInfo.getInstance().majorVersion}-${ApplicationInfo.getInstance().minorVersionMainPart}"
                val ideName = "$ideFullName-$ideVersion-dev".replace(" ", "-").replace(".", "-")
                Log.log(
                    logger::trace,
                    "created ide name from: ideFullName={},ideVersion={}. resulting ide name is: {}",
                    ideFullName,
                    ideVersion,
                    ideName
                )
                ideName
            } else {
                val ideFullName = ApplicationNamesInfo.getInstance().fullProductNameWithEdition
                val ideHomeDir = PathManager.getHomePath()
                val ideHomeDirHash = DigestUtils.sha1Hex(ideHomeDir)
                val ideName = "$ideFullName-$ideHomeDirHash".replace(" ", "-").replace(".", "-")

                Log.log(
                    logger::trace,
                    "created ide name from: ideFullName={},ideHomeDir={},ideHomeDirHash={}. resulting ide name is: {}",
                    ideFullName,
                    ideHomeDir,
                    ideHomeDirHash,
                    ideName
                )
                ideName
            }
        }


        private fun getUserHome(): String {
            //user.home should never be null, but just in case it is use temp dir
            return System.getProperty("user.home") ?: System.getProperty("java.io.tmpdir")
        }


        private fun getBaseDirectory(): File {

            Log.log(logger::trace, "getBaseDirectory called")

            return try {
                if (SystemInfo.isMac) {
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, "Library/Application Support/$DIGMA_DIR")
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is mac, using base directory {}", digmaDir)
                    digmaDir
                } else if (SystemInfo.isWindows) {
                    val userHome = System.getenv("LOCALAPPDATA") ?: getUserHome()
                    val digmaDir = File(userHome, DIGMA_DIR)
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is windows, using base directory {}", digmaDir)
                    digmaDir
                } else {
                    //for linux or any other os
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, ".$DIGMA_DIR")
                    digmaDir.mkdirs()
                    Log.log(logger::trace, "os is ${SystemInfo.OS_NAME}, using base directory {}", digmaDir)
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