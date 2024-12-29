package org.digma.intellij.plugin.docker

import com.intellij.openapi.diagnostic.Logger
import org.digma.intellij.plugin.common.findActiveProject
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.io.File

//TODO: this migration should be removed in the future when we are sure that all users have the latest version of the plugin.
// we can do that around May 2025.
fun migrateDockerComposeFile(newDockerComposeFilePath: String, logger: Logger) {

    /*
    in version 2.0.405 of the plugin we changed the location of the docker-conmpose.yml file.
    from $TEMP/digma-docker/docker-compose.yml
     to
    ${DigmaPathManager.getLocalFilesDirectoryPath()}/digma-docker/docker-compose.yml

    the directory is the same name and so the docker project is the same project.
    this migration will just move the old file to the new location, and because it's the same project name in docker, nothing will change
    and the local engine will continue as usual.

     when this plugin version is installed we try to find the old compose file and just copy it to the new location.
     after that local engine will continue to work as usual.

     if the file is not found the plugin will download the latest and use it. the result is the same
     as if this migration didn't happen because in the old way if the file from $TEMP/digma-docker/docker-compose.yml was deleted the
     plugin would download the latest anyway.

     if the old file does not exist:
        - if local engine is not installed, nothing to do.

        - if local engine is installed and not running:
        the next time user starts the engine the new file that was downloaded will be used.
        this may be an update to the engine if the previous engine was older than the latest.

        - if local engine is installed and running:
        the next time user will try to stop the engine the new file that was downloaded will be used.
        this may not succeed if the engine was installed with a much older version than the latest. but this will happen only if the
        installed engine is very old, we don't expect that to happen.

     if the steps above succeed local engine will continue to work as usual.

     */

    // this is a one time operation, if it fails we don't want to try again.
    // this code will run once after the installation of plugin version that contains this code.

    try {
        //check if this is the first time this plugin version is running
        val isFirstRunAfterPersistDockerCompose = PersistenceService.getInstance().isFirstRunAfterPersistDockerCompose()
        if (isFirstRunAfterPersistDockerCompose) {
            Log.log(logger::info, "first run after persist docker compose")
            PersistenceService.getInstance().setIsFirstRunAfterPersistDockerComposeDone()

            if (!LocalInstallationFacade.getInstance().isLocalEngineInstalled()) {
                Log.log(logger::info, "local engine not installed, nothing to do")
                return
            }

            val oldDockerComposeDir = File(System.getProperty("java.io.tmpdir"), COMPOSE_FILE_DIR_NAME)
            val oldDockerComposeFile = File(oldDockerComposeDir, COMPOSE_FILE_NAME)
            if (oldDockerComposeFile.exists()) {
                val newDockerComposeFile = File(newDockerComposeFilePath)
                Log.log(logger::info, "old compose file found, moving to new location {}", newDockerComposeFile)
                oldDockerComposeFile.copyTo(newDockerComposeFile, overwrite = true)
                //do not delete the old file, it may be used by other IDEs. worst case it will stay as zombie file in the user's temp directory
                ////oldDockerComposeFile.delete()
                Log.log(logger::info, "old compose file moved to new location {}", newDockerComposeFile)
            } else {
                Log.log(logger::info, "old compose file not found")
            }

            findActiveProject()?.let {
                ActivityMonitor.getInstance(it).registerCustomEvent(
                    "docker compose file migrated", mapOf(
                        "oldDockerComposeFileExists" to oldDockerComposeFile.exists(),
                        "newDockerComposeFile" to newDockerComposeFilePath
                    )
                )
            }

        }
    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "error migrating docker compose file")
        ErrorReporter.getInstance().reportError("DockerComposeFileMigration.migrateDockerComposeFile", e)
    }
}


