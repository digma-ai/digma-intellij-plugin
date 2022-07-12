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

    //todo: checkout
    // FrontendTextControlHost.getInstance(project).visibleEditorsChange
    // RiderTextControlHost.getInstance(project).visibleEditorsChange
    // RiderTextControlHost.getInstance(project).

    //todo:
    // before opening an editor check if it is opened , for rider maybe use the classes above
    // see this log
    /*
    022-07-07 11:01:25,815 [  18479]   INFO - #c.j.r.d.FrontendDocumentHost - finished registering document file, MoneyTransferDomainService.cs
2022-07-07 11:01:25,818 [  18482]   WARN - #c.j.r.e.RiderTextControlHost - file /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/MoneyTransferDomainService.cs doesn't exist
2022-07-07 11:01:25,820 [  18484]   INFO - #c.j.r.e.FrontendTextControlHost - Closing (file, SampleInsights.cs, Host, 1, EditorTab) ...
2022-07-07 11:01:25,832 [  18496]   INFO - #c.j.r.d.FrontendDocumentHost - Unbinding document RdProjectFileDocumentId (projectModelElementId = 0filePath = "/home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/SampleInsights.cs"protocol = "file")
2022-07-07 11:01:25,863 [  18527]   INFO - #c.j.r.d.FrontendDocumentHost - Subscribing for document changes.
2022-07-07 11:01:25,864 [  18528]   INFO - #c.j.r.d.FrontendDocumentHost - finished registering document file, SampleInsights.cs
2022-07-07 11:01:25,866 [  18530]   WARN - #c.j.r.e.RiderTextControlHost - file /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/SampleInsights.cs doesn't exist
2022-07-07 11:01:25,868 [  18532]   INFO - #c.j.r.e.FrontendTextControlHost - Closing (file, IMoneyTransferDomainService.cs, Host, 1, EditorTab) ...
2022-07-07 11:01:25,868 [  18532]   INFO - #c.j.r.d.FrontendDocumentHost - Unbinding document RdProjectFileDocumentId (projectModelElementId = 0filePath = "/home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/IMoneyTransferDomainService.cs"protocol = "file")
2022-07-07 11:01:25,869 [  18533]   INFO - #c.j.r.d.FrontendDocumentHost - Subscribing for document changes.
2022-07-07 11:01:25,871 [  18535]   INFO - #c.j.r.d.FrontendDocumentHost - finished registering document file, IMoneyTransferDomainService.cs
2022-07-07 11:01:25,873 [  18537]   WARN - #c.j.r.e.RiderTextControlHost - file /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/IMoneyTransferDomainService.cs doesn't exist
2022-07-07 11:01:25,875 [  18539]   INFO - #c.j.r.e.FrontendTextControlHost - Closing (file, AccountController.cs, Host, 1, EditorTab) ...
2022-07-07 11:01:25,876 [  18540]   INFO - #c.j.r.d.FrontendDocumentHost - Unbinding document RdProjectFileDocumentId (projectModelElementId = 0filePath = "/home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/AccountController.cs"protocol = "file")
2022-07-07 11:01:25,876 [  18540]   INFO - #c.j.r.d.FrontendDocumentHost - Subscribing for document changes.
2022-07-07 11:01:25,878 [  18542]   INFO - #c.j.r.d.FrontendDocumentHost - finished registering document file, AccountController.cs
2022-07-07 11:01:25,880 [  18544]   WARN - #c.j.r.e.RiderTextControlHost - file /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/AccountController.cs doesn't exist
2022-07-07 11:01:25,970 [  18634]   FINE - #o.d.i.p.r.p.DocumentCodeObjectsListener - Digma: Got documentAnalyzed event for /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs
2022-07-07 11:01:25,970 [  18634]   FINE - #o.d.i.p.r.p.DocumentCodeObjectsListener - Digma: Found document in the protocol for /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs
2022-07-07 11:01:25,972 [  18636]   FINE - #o.d.i.p.r.p.DocumentCodeObjectsListener - Digma: Notifying DocumentCodeObjectsChanged for file:///home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs
2022-07-07 11:01:25,973 [  18637]   FINE - #o.d.i.p.r.p.c.CSharpDocumentAnalyzer - Digma: Got documentCodeObjectsChanged event for file:///home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs
2022-07-07 11:01:25,974 [  18638]   FINE - #o.d.i.p.r.p.CodeObjectHost - Digma: Got request for getDocument for /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs
2022-07-07 11:01:25,976 [  18640]   FINE - #o.d.i.p.r.p.CodeObjectHost - Digma: Got document for /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs: Document (
  isComplete = true
  fileUri = "file:///home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs"
  methods = RdMap: `RiderClientFromEBS.SolutionModel.solutions.[1].codeObjectsModel.documents.[/home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Domain/Services/CreditProviderService.cs].methods` (5557519777679624951) [

     */

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
            NotificationUtil.showNotification(project,"This stack trace is empty");
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
