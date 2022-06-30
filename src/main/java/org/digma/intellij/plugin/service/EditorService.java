package org.digma.intellij.plugin.service;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.vcs.VcsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EditorService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(EditorService.class);

    private final Project project;

    private final VcsService vcsService;

    private final Set<VirtualFile> patchOpeningFiles = Collections.synchronizedSet(new HashSet<>());

    public EditorService(Project project) {
        this.project = project;
        vcsService = project.getService(VcsService.class);

        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {

            @Override
            public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {

                for (VirtualFile openFile : source.getOpenFiles()) {
                    if (openFile.getPath().equals(file.getPath())){
                        patchOpeningFiles.add(file);
                        source.closeFile(openFile);
                    }
                }
            }
        });
    }

    public void openErrorFrameWorkspaceFileInEditor(@NotNull URL workspaceUrl, @Nullable String lastInstanceCommitId, int lineNumber) {

        var fileToOpen = VfsUtil.findFileByURL(workspaceUrl);

        if (vcsService.isFileUnderVcs(workspaceUrl)) {

            try {
                if (vcsService.isLocalContentChanged(workspaceUrl, lastInstanceCommitId, lineNumber)) {

                    var showVcsFileResult = Messages.showYesNoCancelDialog(project, "", "File version is different from the version recorded in this flow. " +
                            System.lineSeparator() +
                            "Open the repository version ?", AllIcons.General.QuestionDialog);

                    if (showVcsFileResult == MessageConstants.YES) {
                        fileToOpen = vcsService.getRevisionVirtualFile(workspaceUrl, lastInstanceCommitId);
                    } else if (showVcsFileResult == MessageConstants.CANCEL) {
                        return;
                    }
                }
            } catch (VcsException e) {
                var vcsErrorMsgResult = Messages.showOkCancelDialog(project, "Can not query VCS for file, Show Workspace file ?", "VCS error:" + e.getMessage(), "Ok", "Cancel", AllIcons.General.Error);
                if (vcsErrorMsgResult == MessageConstants.CANCEL) {
                    return;
                }
            }
        }

        openVirtualFile(fileToOpen, workspaceUrl, lineNumber);
    }




    private void openVirtualFile(VirtualFile virtualFile, URL workspaceUrl, int lineNumber) {

        //todo:
        // Rider throws an exception when opening a vcs file if the workspace file is already opened.
        // (Exception: An item with the same key has already been added)
        // waiting for help from jetbrains guys.
        // to workaround it this code closes the workspace file if its opened and restores it when the vcs file
        // is closed.
        // plus this class subscribes to beforeFileOpened , in case the vcs file is opened and the user
        // opens the workspace file then the vcs file will be closed.
        // if jetrains guys have a better idea how to open a readonly c# file in Rider without getting the exception
        // then all this is not necessary

        if (virtualFile instanceof ContentRevisionVirtualFile){

            boolean fileWasOpened = false;
            var workspaceVirtualFile = VfsUtil.findFileByURL(workspaceUrl);
            if (workspaceVirtualFile != null && FileEditorManager.getInstance(project).isFileOpen(workspaceVirtualFile)){
                fileWasOpened = true;
                FileEditorManager.getInstance(project).closeFile(workspaceVirtualFile);
            }


            virtualFile.putUserData(new Key("org.digma.plugin.editor.vcsFile"),"true");
            OpenFileDescriptor navigatable = new OpenFileDescriptor(project, virtualFile, Math.max(0, lineNumber - 1), 0);
            FileEditorManager.getInstance(project).openTextEditor(navigatable,true);

            boolean finalFileWasOpened = fileWasOpened;
            FileEditorManager.getInstance(project).addFileEditorManagerListener(new FileEditorManagerListener() {
                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (file.equals(virtualFile) && finalFileWasOpened){
                        if (!patchOpeningFiles.contains(workspaceVirtualFile)) {
                            FileEditorManager.getInstance(project).openFile(workspaceVirtualFile, true, true);
                        }
                        patchOpeningFiles.remove(workspaceVirtualFile);
                        FileEditorManager.getInstance(project).removeFileEditorManagerListener(this);
                    }
                }
            });


        }else{
            OpenFileDescriptor navigatable = new OpenFileDescriptor(project, virtualFile, Math.max(0, lineNumber - 1), 0);
            FileEditorManager.getInstance(project).openTextEditor(navigatable,true);
        }

    }



    public void openSpanWorkspaceFileInEditor(String workspaceUri, int offset) {

        try {
            var fileToOpen = VfsUtil.findFileByURL(new URL(workspaceUri));
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, fileToOpen, offset);
            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        } catch (MalformedURLException e) {
            Log.log(LOGGER::warn, "Could not open file. " + e.getMessage());
            NotificationUtil.notifyError(project,"Could not open file. " + e.getMessage());
        }
    }


    public void openRawTrace(String stackTrace) {

        if (stackTrace == null) {
            //todo: show notification
            return;
        }
        try {
            String name = "digma-stacktrace-"+stackTrace.hashCode()+".txt";
            var vf = new LightVirtualFile(name, stackTrace);
            vf.setWritable(false);
            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, vf);
            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
        } catch (Exception e) {
            Log.log(LOGGER::warn, "Could not open stack trace. " + e.getMessage());
            NotificationUtil.notifyError(project,"Could not open stack trace. " + e.getMessage());
        }
    }


    @Override
    public void dispose() {

    }

}
