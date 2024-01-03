package org.digma.intellij.plugin.vcs

import com.intellij.collaboration.util.resolveRelative
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.history.GitHistoryUtils
import git4idea.remote.hosting.GitHostingUrlUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * VcsService tries to be abstract and use intellij vcs abstraction.
 * if necessary it may fall back to git, we have git4idea in the classpath and plugin dependency.
 */
@Service(Service.Level.PROJECT)
class VcsService(project: Project) : BaseVcsService(project) {


    fun getVcsType(): String {
        return ProjectLevelVcsManager.getInstance(project).singleVCS?.name ?: "unknown"
    }


    /**
     * return commit id for current project.
     * should work the same for all vcs
     */
    fun getCommitIdForCurrentProject(): String? {

        val future = Backgroundable.executeOnPooledThread<String> {
            try {
                val vcsRoots = ProjectLevelVcsManager.getInstance(project).allVcsRoots
                if (vcsRoots.isEmpty()) {
                    return@executeOnPooledThread null
                }

                if (vcsRoots.size > 1) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(
                        "Multiple vcs roots detected",
                        Collections.singletonMap("vcsRootsNum", vcsRoots.size)
                    )
                    return@executeOnPooledThread null
                }

                val vcsRoot = vcsRoots[0]
                val vcs = vcsRoot.vcs

                val filePath = VcsUtil.getFilePath(vcsRoot.path)

                return@executeOnPooledThread vcs!!.vcsHistoryProvider!!.createSessionFor(filePath)!!.currentRevisionNumber.asString()

            } catch (e: java.lang.Exception) {
                ErrorReporter.getInstance()
                    .reportError(project, "VcsService.getCommitIdForCurrentProject", e)
                Log.warnWithException(LOGGER, project, e, "error in getCommitIdForCurrentProject")
                return@executeOnPooledThread null
            }
        }

        return try {
            future[5, TimeUnit.SECONDS]
        } catch (e: java.lang.Exception) {
            ErrorReporter.getInstance()
                .reportError(project, "VcsService.getCommitIdForCurrentProject", e)
            Log.warnWithException(LOGGER, project, e, "error in getCommitIdForCurrentProject")
            null
        }
    }


    //todo: currently build link only if github, else returns null. implement for bitbucket , gitlab etc.
    fun buildRemoteLinkToCommit(commitHash: String): String? {

        val future = Backgroundable.executeOnPooledThread<String> {
            try {

                val vcsRoots = ProjectLevelVcsManager.getInstance(project).allVcsRoots
                if (vcsRoots.isEmpty()) {
                    return@executeOnPooledThread null
                }

                if (vcsRoots.size > 1) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(
                        "Multiple vcs roots detected",
                        Collections.singletonMap("vcsRootsNum", vcsRoots.size)
                    )
                    return@executeOnPooledThread null
                }

                val vcsRoot = vcsRoots[0]
                val vcs = vcsRoot.vcs

                if (vcs !is GitVcs) {
                    return@executeOnPooledThread null
                }

                try {
                    //this is the way to check that this commit exists in this repository
                    val commitMetadata = GitHistoryUtils.collectCommitsMetadata(project, vcsRoot.path, commitHash)
                    if (commitMetadata.isNullOrEmpty()) {
                        return@executeOnPooledThread null
                    }
                } catch (e: VcsException) {
                    //if the commit hash is not in this repository collectCommitsMetadata throws an exception
                    ErrorReporter.getInstance().reportError(project, "VcsService.buildRemoteLinkToCommit.collectCommitsMetadata", e)
                    return@executeOnPooledThread null
                }

                val repository = GitRepositoryManager.getInstance(project).getRepositoryForRootQuick(vcsRoot.path)
                if (repository !is GitRepository) return@executeOnPooledThread null

                val url = GitUtil.getDefaultRemote(repository.remotes)?.firstUrl
                    ?: return@executeOnPooledThread null

                if (!url.startsWith("git@github.com")) {
                    return@executeOnPooledThread null
                }

                val uri = GitHostingUrlUtil.getUriFromRemoteUrl(url)

                val commitUri = uri?.resolveRelative("commit")?.resolveRelative(commitHash)?.toString()

                val isUrlExists = commitUri?.let {
                    isCommitUrlExists(it)
                } ?: false
                if (!isUrlExists) {
                    return@executeOnPooledThread null
                }

                return@executeOnPooledThread commitUri

            } catch (e: Exception) {
                Log.warnWithException(LOGGER, project, e, "error in buildRemoteLinkToCommit for {}", commitHash)
                ErrorReporter.getInstance().reportError(project, "VcsService.buildRemoteLinkToCommit", e)
                return@executeOnPooledThread null
            }
        }

        return try {
            future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Log.warnWithException(LOGGER, project, e, "error in buildRemoteLinkToCommit for {}", commitHash)
            ErrorReporter.getInstance().reportError(project, "VcsService.buildRemoteLinkToCommit", e)
            null
        }
    }




    //todo: currently build link only if github, else returns null. implement for bitbucket , gitlab etc.
    fun getLinkToRemoteCommitIdForCurrentProject(): String? {

        val future = Backgroundable.executeOnPooledThread<String> {
            try {

                val vcsRoots = ProjectLevelVcsManager.getInstance(project).allVcsRoots
                if (vcsRoots.isEmpty()) {
                    return@executeOnPooledThread null
                }

                if (vcsRoots.size > 1) {
                    ActivityMonitor.getInstance(project).registerCustomEvent(
                        "Multiple vcs roots detected",
                        Collections.singletonMap("vcsRootsNum", vcsRoots.size)
                    )
                    return@executeOnPooledThread null
                }

                val vcsRoot = vcsRoots[0]
                val vcs = vcsRoot.vcs

                if (vcs !is GitVcs) {
                    return@executeOnPooledThread null
                }

                val filePath = VcsUtil.getFilePath(vcsRoot.path)

                val commitHash = GitHistoryUtils.getCurrentRevision(project, filePath, "HEAD")?.asString()
                    ?: return@executeOnPooledThread null

//                val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(filePath)
                val repository = GitRepositoryManager.getInstance(project).getRepositoryForRootQuick(vcsRoot.path)
                if (repository !is GitRepository) return@executeOnPooledThread null

                val url = GitUtil.getDefaultRemote(repository.remotes)?.firstUrl
                    ?: return@executeOnPooledThread null


                if (!url.startsWith("git@github.com")) {
                    return@executeOnPooledThread null
                }

                val uri = GitHostingUrlUtil.getUriFromRemoteUrl(url)

                val commitUri = uri?.resolveRelative("commit")?.resolveRelative(commitHash)?.toString()

                val isUrlExists = commitUri?.let {
                    isCommitUrlExists(it)
                } ?: false
                if (!isUrlExists) {
                    return@executeOnPooledThread null
                }


                return@executeOnPooledThread commitUri

            } catch (e: Exception) {
                Log.warnWithException(LOGGER, project, e, "error in getLinkToRemoteCommitIdForCurrentProject")
                ErrorReporter.getInstance().reportError(project, "VcsService.getLinkToRemoteCommitIdForCurrentProject", e)
                return@executeOnPooledThread null
            }
        }

        return try {
            future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Log.warnWithException(LOGGER, project, e, "error in getLinkToRemoteCommitIdForCurrentProject")
            ErrorReporter.getInstance().reportError(project, "VcsService.getLinkToRemoteCommitIdForCurrentProject", e)
            null
        }
    }


    private fun isCommitUrlExists(commitUrl: String): Boolean {
        try {
            val url = URL(commitUrl)
            val huc: HttpURLConnection = url.openConnection() as HttpURLConnection
            huc.setRequestMethod("HEAD")
            val responseCode: Int = huc.getResponseCode()

            return responseCode == 200
        } catch (e: Exception) {
            return false
        }
    }

}