package org.digma.intellij.plugin.vcs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.history.ShortVcsRevisionNumber;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * VcsService tries to be abstract and use intellij vcs abstraction.
 * if necessary it may fall back to git, we have git4idea in the classpath and plugin dependency.
 */

public class VcsService {

    private static final Logger LOGGER = Logger.getInstance(VcsService.class);

    private final Project project;

    public VcsService(Project project) {
        this.project = project;
    }

    public boolean isFileUnderVcs(@NotNull VirtualFile workspaceFile) {
        var filePath = VcsUtil.getFilePath(workspaceFile);
        if (filePath.getVirtualFile() == null) {
            return false;
        }
        return VcsUtil.isFileUnderVcs(project, filePath);
    }

    public boolean isLocalContentChanged(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId, int lineNumber) throws VcsException {
        if (isFileUnderVcs(workspaceFile) && isRevisionExist(workspaceFile, lastInstanceCommitId)) {
            try {
                return isLocalContentLineChanged(workspaceFile, lastInstanceCommitId, lineNumber);
            } catch (VcsException e) {
                Log.log(LOGGER::warn, "Could not find revision {} for {}, {}", lastInstanceCommitId, workspaceFile, e.getMessage());
                throw e;
            }
        }
        return false;
    }


    @Nullable
    public String getShortRevisionString(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId) {
        var revision = getRevisionFor(workspaceFile, lastInstanceCommitId);
        return revision == null ? null : getShortRevisionString(revision);
    }


    @NotNull
    public String getShortRevisionString(@NotNull VcsRevisionNumber vcsRevisionNumber){
        if (ShortVcsRevisionNumber.class.isAssignableFrom(vcsRevisionNumber.getClass())){
            return ((ShortVcsRevisionNumber)vcsRevisionNumber).toShortString();
        }
        return vcsRevisionNumber.asString();
    }


    //this method assumes that the file is under vcs and lastInstanceCommitId exists, otherwise there may be error dialogs from the vcs plugin.
    private boolean isLocalContentLineChanged(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId, int lineNumber) throws VcsException {

        return ProgressManager.getInstance().run(new Task.WithResult<Boolean, VcsException>(project, "Check Local Changes", true) {
            @Override
            protected Boolean compute(@NotNull ProgressIndicator indicator) throws VcsException {

                if (!isFileUnderVcs(workspaceFile)) {
                    Log.log(LOGGER::debug, "File {} is not under vcs", workspaceFile);
                    return false;
                }

                if (!isRevisionExist(workspaceFile, lastInstanceCommitId)) {
                    Log.log(LOGGER::debug, "Revision {} for File {} was not found", lastInstanceCommitId, workspaceFile);
                    return false;
                }

                var filePath = VcsUtil.getFilePath(workspaceFile);
                var vcs = VcsUtil.getVcsFor(project, filePath);

                //if any of the providers we need is null throw exception
                if (vcs == null ||
                        vcs.getDiffProvider() == null) {
                    throw new VcsException("Can not find vcs for file: " + workspaceFile);
                }

                Log.log(LOGGER::debug, "File {} is under vcs, trying to detect changes for revision {}", filePath, lastInstanceCommitId);

                var requiredRevision = lastInstanceCommitId == null || lastInstanceCommitId.isBlank() ?
                        getLastCommittedRevision(vcs, filePath) :
                        vcs.parseRevisionNumber(lastInstanceCommitId);

                Log.log(LOGGER::debug, "Required revision for File {} is {}", filePath, requiredRevision);

                if (requiredRevision == null || filePath.getVirtualFile() == null) {
                    throw new VcsException("Can not find revision " + lastInstanceCommitId + " for file: " + workspaceFile);
                }

                try {
                    if (vcs.getDiffProvider().canCompareWithWorkingDir()) {
                        return hasChangeWithWorkingDir(vcs, filePath, requiredRevision, lineNumber);
                    }
                } catch (VcsException e) {
                    Log.log(LOGGER::warn, "Could not compare revision {} with working dir for file {}", requiredRevision, filePath);
                }

                var currentRevision = vcs.getDiffProvider().getCurrentRevision(filePath.getVirtualFile());
                var currentContent = vcs.getDiffProvider().createFileContent(currentRevision, filePath.getVirtualFile());
                var c = ChangeListManager.getInstance(project).getChange(filePath);
                if (c != null) {
                    currentContent = CurrentContentRevision.create(filePath);
                }

                var requiredRevisionContent = vcs.getDiffProvider().createFileContent(requiredRevision, filePath.getVirtualFile());

                if (requiredRevisionContent == null || currentContent == null) {
                    return false;
                }


                if (requiredRevisionContent.getRevisionNumber().equals(currentContent.getRevisionNumber())) {
                    return false;
                }

                return !equalsByLineNumber(requiredRevisionContent, currentContent, lineNumber);
            }
        });
    }

    private Boolean hasChangeWithWorkingDir(@NotNull AbstractVcs vcs, @NotNull FilePath filePath, @NotNull VcsRevisionNumber requiredRevision, int lineNumber) throws VcsException {

        var changes = Objects.requireNonNull(vcs.getDiffProvider()).compareWithWorkingDir(Objects.requireNonNull(filePath.getVirtualFile()), requiredRevision);
        if (changes != null && !changes.isEmpty()) {
            //usually it's a singleton result, but anyway test all changes to detect a change in line number
            for (Change change : changes) {
                if (change.getBeforeRevision() == null) {
                    throw new VcsException("Could not compare revision {} with working dir, could not find before revision.");
                }
                boolean equals = equalsByLineNumber(change.getBeforeRevision(), change.getAfterRevision(), lineNumber);
                if (!equals) {
                    return true;
                }

            }
        }
        //if no change detected and no exception throws then probably no change
        return false;
    }


    private boolean equalsByLineNumber(@Nullable ContentRevision contentRevision1, @Nullable ContentRevision contentRevision2, int lineNumber) throws VcsException {

        if (contentRevision1 == null || contentRevision2 == null) {
            return false;
        }

        String line1 = getLine(contentRevision1.getContent(), lineNumber);
        String line2 = getLine(contentRevision2.getContent(), lineNumber);
        return (line1 == null && line2 == null) || (line1 != null && line1.equals(line2));

    }


    private String getLine(String text, int lineNumber) {
        try (Stream<String> lines = text.lines()) {
            return lines.skip((long) lineNumber - 1).findFirst().orElse(null);
        }
    }


    @Nullable
    private VcsRevisionNumber getLastCommittedRevision(@NotNull AbstractVcs vcs, @NotNull FilePath filePath) {

        if (vcs.getDiffProvider() == null){
            return null;
        }

        var lastRevision = vcs.getDiffProvider().getLastRevision(filePath);
        if (lastRevision != null) {
            return lastRevision.getNumber();
        }

        return null;
    }

    public @Nullable VirtualFile getRevisionVirtualFile(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId) throws VcsException {

        return ProgressManager.getInstance().run(new Task.WithResult<VirtualFile, VcsException>(project, "Load Revision Content From VCS", true) {
            @Override
            protected VirtualFile compute(@NotNull ProgressIndicator indicator) throws VcsException {

                if (!isFileUnderVcs(workspaceFile)) {
                    Log.log(LOGGER::debug, "File {} is not under vcs", workspaceFile);
                    return null;
                }

                if (!isRevisionExist(workspaceFile, lastInstanceCommitId)) {
                    Log.log(LOGGER::debug, "Revision {} for File {} was not found", lastInstanceCommitId, workspaceFile);
                    return null;
                }


                var filePath = VcsUtil.getFilePath(workspaceFile);
                var vcs = VcsUtil.getVcsFor(project, filePath);

                //if any of the providers we need is null return false coz we don't have a way to check revisions
                if (vcs == null ||
                        vcs.getDiffProvider() == null) {
                    throw new VcsException("Can not find vcs for file: " + workspaceFile);
                }

                var requiredRevision = lastInstanceCommitId == null || lastInstanceCommitId.isBlank() ?
                        getLastCommittedRevision(vcs, filePath) :
                        vcs.parseRevisionNumber(lastInstanceCommitId);

                Log.log(LOGGER::debug, "Required revision for File {} is {}", filePath, requiredRevision);

                if (requiredRevision == null || filePath.getVirtualFile() == null) {
                    throw new VcsException("Can not find revision " + lastInstanceCommitId + " for file: " + workspaceFile);
                }


                //depending on the changes in the file some intellij vcs APIs will succeed and some don't ,
                // so try two ways to get the content.
                //using DiffProvider.CompareWithWorkingDir in some cases loads the revision content with better success
                //if it fails then just load the revision content directly with DiffProvider.createFileContent

                try {
                    if (vcs.getDiffProvider().canCompareWithWorkingDir()) {
                        var result = getFromCompareWithWorkingDir(vcs, filePath, requiredRevision);
                        if (result != null){
                            return ContentRevisionVirtualFile.create(result);
                        }
                    }
                } catch (VcsException e) {
                    Log.error(LOGGER, project, e, "Could not compare revision {} with working dir for file {}", requiredRevision, filePath);
                }

                var requiredRevisionContent = vcs.getDiffProvider().createFileContent(requiredRevision, filePath.getVirtualFile());
                if (requiredRevisionContent == null){
                    return null;
                }

                return ContentRevisionVirtualFile.create(requiredRevisionContent);
            }
        });
    }

    private @Nullable ContentRevision getFromCompareWithWorkingDir(AbstractVcs vcs, FilePath filePath, VcsRevisionNumber requiredRevision) throws VcsException {

        if (vcs.getDiffProvider() == null || filePath.getVirtualFile() == null){
            return null;
        }

        var changes = vcs.getDiffProvider().compareWithWorkingDir(filePath.getVirtualFile(), requiredRevision);
        if (changes != null && changes.size() == 1 ) {
            var change = changes.iterator().next();
            if (change.getBeforeRevision() == null) {
                throw new VcsException("Could not compare revision {} with working dir, could not find before revision.");
            }

            return change.getBeforeRevision();
        }
        return null;
    }


    public boolean isRevisionExist(@NotNull VirtualFile workspaceFile, @Nullable String lastInstanceCommitId) {

        return ProgressManager.getInstance().run(new Task.WithResult<>(project, "Is Revision Exist", true) {
            @Override
            protected Boolean compute(@NotNull ProgressIndicator indicator) {
                try {

                    if (!isFileUnderVcs(workspaceFile)) {
                        return false;
                    }

                    var filePath = VcsUtil.getFilePath(workspaceFile);
                    var vcs = VcsUtil.getVcsFor(project, filePath);
                    if (vcs == null) {
                        return false;
                    }
                    var requiredRevision = lastInstanceCommitId == null || lastInstanceCommitId.isBlank() ?
                            getLastCommittedRevision(vcs, filePath) :
                            vcs.parseRevisionNumber(lastInstanceCommitId);

                    if (requiredRevision == null) {
                        return false;
                    }

                    return vcs.loadRevisions(filePath.getVirtualFile(), requiredRevision) != null;
                } catch (VcsException e) {
                    Log.log(LOGGER::warn, "Could not find revision {} for {}, {}", lastInstanceCommitId, workspaceFile, e.getMessage());
                    return false;
                }
            }
        });


    }


    @Nullable
    public VcsRevisionNumber getRevisionFor(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId) {

        try {
            if (lastInstanceCommitId != null && isFileUnderVcs(workspaceFile)) {
                var filePath = VcsUtil.getFilePath(workspaceFile);
                var vcs = VcsUtil.getVcsFor(project, filePath);
                return vcs != null ? vcs.parseRevisionNumber(lastInstanceCommitId) : null;
            }
        } catch (Exception e) {
            Log.log(LOGGER::warn, "Could not parse revision {} for {}, {}", lastInstanceCommitId, workspaceFile, e.getMessage());
        }
        return null;
    }

}
