package org.digma.intellij.plugin.vcs

import com.intellij.collaboration.util.resolveRelative
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.history.GitHistoryUtils
import git4idea.remote.hosting.GitHostingUrlUtil
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.posthog.ActivityMonitor
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * VcsService tries to be abstract and use intellij vcs abstraction.
 * if necessary it may fall back to git, we have git4idea in the classpath and plugin dependency.
 */
class VcsService(project: Project) : BaseVcsService(project) {


    fun getVcsType(): String? {
        return ProjectLevelVcsManager.getInstance(project).singleVCS?.name ?: "unknown"
    }


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

                vcs as GitVcs

                val filePath = VcsUtil.getFilePath(vcsRoot.path)

                val commitHash = GitHistoryUtils.getCurrentRevision(project, filePath, "HEAD")?.asString()
                    ?: return@executeOnPooledThread null

//                val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(filePath)
                val repository = GitRepositoryManager.getInstance(project).getRepositoryForRootQuick(vcsRoot.path)
                if (repository !is GitRepository) return@executeOnPooledThread null

                val url = GitUtil.getDefaultRemote(repository.remotes)?.firstUrl ?: return@executeOnPooledThread null

                val uri = GitHostingUrlUtil.getUriFromRemoteUrl(url)

                return@executeOnPooledThread uri?.resolveRelative("commit")?.resolveRelative(commitHash)?.toString()

            } catch (e: Exception) {
                ErrorReporter.getInstance().reportError(project, "VcsService.getCommitIdForCurrentProject", e)
                return@executeOnPooledThread null
            }
        }

        return try {
            future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            null
        }
    }


}