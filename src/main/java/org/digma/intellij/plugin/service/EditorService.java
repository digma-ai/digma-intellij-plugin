package org.digma.intellij.plugin.service;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.BinaryLightVirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class EditorService implements Disposable {

    public static final String STACKTRACE_PREFIX = "digma-stacktrace";
    public static final String VCS_PREFIX = "digma-vcs";

    private final Logger LOGGER = Logger.getInstance(EditorService.class);

    private final Project project;

    private final VcsService vcsService;


    public EditorService(Project project) {
        this.project = project;
        vcsService = project.getService(VcsService.class);
    }


    public void openErrorFrameWorkspaceFileInEditor(@NotNull URL workspaceUrl, @Nullable String lastInstanceCommitId, int lineNumber) {

        var workspaceFile = VfsUtil.findFileByURL(workspaceUrl);
        if (workspaceFile == null) {
            NotificationUtil.notifyError(project, "Could not find file " + workspaceUrl);
            return;
        }

        try {
            //if file not under vcs then the workspaceFile will open
            if (vcsService.isFileUnderVcs(workspaceUrl)) {
                //if revision doesn't exist show a warning and then the workspaceFile will open
                if (vcsService.isRevisionExist(workspaceUrl, lastInstanceCommitId)) {
                    //if no changes then the workspaceFile will open
                    if(vcsService.isLocalContentChanged(workspaceUrl, lastInstanceCommitId, lineNumber)) {
                        VirtualFile vcsFile = vcsService.getRevisionVirtualFile(workspaceUrl, lastInstanceCommitId);
                        if (vcsFile != null) {
                            maybeOpenVcsFile(workspaceFile, (ContentRevisionVirtualFile) vcsFile, workspaceUrl, lineNumber);
                            return;
                        }else{
                            Messages.showWarningDialog(project,"Could not load vcs file for  "+workspaceFile+", Opening workspace file.","");
                        }
                    }
                }else{
                    Messages.showWarningDialog(project,"Revision "+lastInstanceCommitId+" for "+workspaceFile+" was not found, Opening workspace file.","");
                }
            }
        } catch (VcsException e) {
            Log.log(LOGGER::warn , "Could not query vcs for file {} " , workspaceUrl);
            var vcsErrorMsgResult = Messages.showOkCancelDialog(project, "Can not query VCS for file, Show Workspace file ?", "VCS error:" + e.getMessage(), "Ok", "Cancel", AllIcons.General.Error);
            if (vcsErrorMsgResult == MessageConstants.CANCEL) {
                return;
            }
        }

        //if we didn't try to maybeOpenVcsFile or if there was a VcsException then open the workspace file
        openVirtualFile(workspaceFile, lineNumber);
    }


    private void maybeOpenVcsFile(@NotNull VirtualFile workspaceFile, @NotNull ContentRevisionVirtualFile vcsFile, @NotNull URL workspaceUrl, int lineNumber) {

        var hash =  vcsService.getShortRevisionString(vcsFile.getContentRevision().getRevisionNumber());
        var filePath = workspaceUrl.getPath();
        var indexOfSlash = Math.max(filePath.lastIndexOf('/') + 1, 0);
        var fileName = filePath.substring(indexOfSlash);
        String name = VCS_PREFIX + "-" + hash + "-" + fileName;

        //if the vcs file is already opened just show it and don't ask again
        if (showIfAlreadyOpen(name, lineNumber)) {
            return;
        }

        var showVcsFileResult = Messages.showYesNoCancelDialog(project, "", "File version is different from the version recorded in this flow. " +
                System.lineSeparator() +
                "Open the repository version ?", AllIcons.General.QuestionDialog);

        if (showVcsFileResult == MessageConstants.YES) {
            var vf = new BinaryLightVirtualFile(name, vcsFile.contentsToByteArray());
            vf.setOriginalFile(vcsFile);
            vf.setWritable(false);
            openVirtualFile(vf, lineNumber);
        } else if (showVcsFileResult == MessageConstants.NO) {
            openVirtualFile(workspaceFile, lineNumber);
        } else if (showVcsFileResult == MessageConstants.CANCEL) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }


    private void openVirtualFile(VirtualFile virtualFile, int lineNumber) {
        OpenFileDescriptor navigable = new OpenFileDescriptor(project, virtualFile, Math.max(0, lineNumber - 1), 0);
        FileEditorManager.getInstance(project).openTextEditor(navigable, true);
    }


    private boolean showIfAlreadyOpen(String name, int lineNumber) {
        var openedFile = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                .filter(virtualFile -> name.equals(virtualFile.getName())).findAny().orElse(null);

        if (openedFile != null) {
            openVirtualFile(openedFile, lineNumber);
            return true;
        }

        return false;
    }


    public void openSpanWorkspaceFileInEditor(@NotNull String workspaceUri, int offset) {

        try {
            var fileToOpen = VfsUtil.findFileByURL(new URL(workspaceUri));
            if (fileToOpen == null) {
                NotificationUtil.notifyError(project, "Could not find file " + workspaceUri);
                return;
            }
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, fileToOpen, Math.max(offset, 0));
            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        } catch (MalformedURLException e) {
            Log.log(LOGGER::warn, "Could not open file " + workspaceUri + ", Exception" + e.getMessage());
            NotificationUtil.notifyError(project, "Could not open file " + workspaceUri + ", Exception:" + e.getMessage());
        }
    }


    public void openRawStackTrace(String stackTrace) {

        if (stackTrace == null || stackTrace.isBlank()) {
            NotificationUtil.showNotification(project, "This stack trace is empty");
            return;
        }

        try {
            //create unique name
            String name = STACKTRACE_PREFIX + "-" + Math.abs(stackTrace.hashCode()) + ".txt";

            if (showIfAlreadyOpen(name, 0)) {
                return;
            }

            var vf = new LightVirtualFile(name, stackTrace);
            vf.setWritable(false);
            openVirtualFile(vf, 0);

        } catch (Exception e) {
            Log.log(LOGGER::warn, "Could not open stack trace. " + e.getMessage());
            NotificationUtil.notifyError(project,"Could not open stack trace. " + e.getMessage());
        }
    }


    @Override
    public void dispose() {

    }

}
