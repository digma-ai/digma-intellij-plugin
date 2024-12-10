package org.digma.intellij.plugin.paths

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfo
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import java.io.File

const val DIGMA_DIR = "digma-intellij-plugin"

class DigmaPathManager {


    companion object {

        fun getLocalFilesDirectoryPath(): String {
            val ideFullName = ApplicationNamesInfo.getInstance().fullProductNameWithEdition
            val ideHomeDir = PathManager.getHomePath().substringAfterLast("/")
            val ideName = "$ideFullName-$ideHomeDir".replace(" ", "-").replace(".","-")
            return try {
                val baseDir = getBaseDirectory()
                val ideDir = File(baseDir,ideName)
                ideDir.mkdirs()
                ideDir.absolutePath
            }catch (e:Throwable){
                ErrorReporter.getInstance().reportError("DigmaPathManager.getLocalFilesDirectoryPath", e)
                val fallback = File(System.getProperty("java.io.tmpdir"), "$DIGMA_DIR/$ideName")
                fallback.mkdirs()
                fallback.absolutePath
            }
        }


        private fun getUserHome(): String {
            //user.home should never be null, but just in case,use temp dir
            return System.getProperty("user.home") ?: System.getProperty("java.io.tmpdir")
        }


        private fun getBaseDirectory(): File {

            return try {
                if (SystemInfo.isMac) {
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, "Library/Application Support/$DIGMA_DIR")
                    digmaDir.mkdirs()
                    digmaDir
                } else if (SystemInfo.isWindows) {
                    val userHome = System.getenv("LOCALAPPDATA") ?: getUserHome()
                    val digmaDir = File(userHome, DIGMA_DIR)
                    digmaDir.mkdirs()
                    digmaDir
                } else {
                    //for linux or any other os
                    val userHome = getUserHome()
                    val digmaDir = File(userHome, ".$DIGMA_DIR")
                    digmaDir.mkdirs()
                    digmaDir
                }

            } catch (e: Throwable) {
                ErrorReporter.getInstance().reportError("DigmaPathManager.getBaseDirectory", e)
                val userHome = getUserHome()
                val digmaDir = File(userHome, ".$DIGMA_DIR")
                digmaDir.mkdirs()
                digmaDir
            }
        }
    }
}