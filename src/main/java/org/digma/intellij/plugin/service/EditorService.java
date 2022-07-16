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

    //todo: check these

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



    /*

    /todo: check this exception, maybe change the id of the document and keep them open , or copy to temp folder

    com.jetbrains.rdclient.util.BackendException: An item with the same key has already been added. Key: /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/SampleInsights.cs

--- EXCEPTION #1/2 [ArgumentException]
Message = “An item with the same key has already been added. Key: /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/SampleInsights.cs”
ExceptionPath = Root.InnerException
ClassName = System.ArgumentException
Data.ThreadLocalDebugInfo = “
  map `RoslynHost.RoslynModel.session.$.openDocuments` (13243236516587360802)
   -> map `MainProtocol.SolutionModel.solutions.[1].documentsOperationModel.documents` (14314735552276044795)
   -> RdDispatcher::FlushAll
”
HResult = E_INVALIDARG=COR_E_ARGUMENT=WIN32_ERROR_INVALID_PARAMETER=80070057
Source = System.Private.CoreLib
StackTraceString = “
  at System.Collections.Generic.Dictionary`2.TryInsert(TKey key, TValue value, InsertionBehavior behavior)
     at System.Collections.Generic.Dictionary`2.Add(TKey key, TValue value)
     at JetBrains.Collections.Viewable.ViewableMap`2.Add(TK key, TV value)
     at JetBrains.Collections.Viewable.ViewableMap`2.Add(KeyValuePair`2 item)
     at JetBrains.Rd.Impl.RdMap`2.Add(KeyValuePair`2 item)
     at JetBrains.DictionaryEx.<>c__DisplayClass4_0`2.<Add>b__0()
     at JetBrains.Lifetimes.LifetimeDefinition.Bracket(Action opening, Action closing)
     at JetBrains.DictionaryEx.Add[TKey,TValue](IDictionary`2 dictionary, Lifetime lifetime, TKey key, TValue value)
     at JetBrains.Rider.Roslyn.Host.RiderWorkerHost.<>c__DisplayClass14_0.<ViewOpenedDocuments>b__0(Lifetime documentLifetime, RdDocumentId key, RiderDocument document)
     at JetBrains.Collections.Viewable.ReactiveEx.<>c__DisplayClass18_0`2.<View>b__0(AddRemove kind, K key, V value)
     at JetBrains.Collections.Viewable.ReactiveEx.<>c__DisplayClass15_0`2.<AdviseAddRemove>b__0(MapEvent`2 e)
     at JetBrains.Collections.Viewable.SignalBase`1.Fire(T value)
”

--- Outer ---

--- EXCEPTION #2/2 [LoggerException]
Message = “An item with the same key has already been added. Key: /home/shalom/workspace/digma/otel-sample-application-dotnet/Sample.MoneyTransfer.Api/Controllers/SampleInsights.cs”
ExceptionPath = Root
ClassName = JetBrains.Util.LoggerException
InnerException = “Exception #1 at Root.InnerException”
HResult = COR_E_APPLICATION=80131600
StackTraceString = “
  at JetBrains.Util.LoggerBase.Log(LoggingLevel level, String message, Exception ex)
     at JetBrains.Diagnostics.Log.SwitchingLog.JetBrains.Diagnostics.ILog.Log(LoggingLevel level, String message, Exception exception)
     at JetBrains.Diagnostics.LogEx.Error(ILog this, Exception ex, String message)
     at JetBrains.Collections.Viewable.SignalBase`1.Fire(T value)
     at JetBrains.Collections.Viewable.ViewableMap`2.Add(TK key, TV value)
     at JetBrains.Collections.Viewable.ViewableMap`2.Add(KeyValuePair`2 item)
     at JetBrains.DictionaryEx.<>c__DisplayClass4_0`2.<Add>b__0()
     at JetBrains.Lifetimes.LifetimeDefinition.Bracket(Action opening, Action closing)
     at JetBrains.DictionaryEx.Add[TKey,TValue](IDictionary`2 dictionary, Lifetime lifetime, TKey key, TValue value)
     at JetBrains.Rider.Backend.Features.Documents.RiderDocumentHost.<AdviseModel>b__5_0(Lifetime lifetime, RdDocumentId documentId, IDocumentViewModel documentModel)
     at JetBrains.Collections.Viewable.ReactiveEx.<>c__DisplayClass18_0`2.<View>b__0(AddRemove kind, K key, V value)
     at JetBrains.Collections.Viewable.ReactiveEx.<>c__DisplayClass15_0`2.<AdviseAddRemove>b__0(MapEvent`2 e)
     at JetBrains.Collections.Viewable.SignalBase`1.Fire(T value)
     at JetBrains.Collections.Viewable.ViewableMap`2.Add(TK key, TV value)
     at JetBrains.Rider.Backend.Features.Documents.RiderDocumentHost.<InitSolutionModel>b__4_0(MapEvent`2 change)
     at JetBrains.Collections.Viewable.SignalBase`1.Fire(T value)
     at JetBrains.Collections.Viewable.ViewableMap`2.set_Item(TK key, TV value)
     at JetBrains.Rd.Impl.RdMap`2.OnWireReceived(UnsafeReader stream)
     at JetBrains.Rd.Impl.MessageBroker.Execute(IRdWireable reactive, Byte[] msg)
     at JetBrains.Util.Concurrency.ExecutionContextEx.<>c__DisplayClass0_0.<Run>b__0(Object _)
     at System.Threading.ExecutionContext.RunInternal(ExecutionContext executionContext, ContextCallback callback, Object state)
     at JetBrains.Util.Concurrency.ExecutionContextEx.Run(ExecutionContext context, ContextCallback callback, Object state)
     at JetBrains.Platform.RdFramework.Impl.RdDispatcher.FlushAll()
     at JetBrains.Util.Concurrency.ExecutionContextEx.<>c__DisplayClass0_0.<Run>b__0(Object _)
     at System.Threading.ExecutionContext.RunInternal(ExecutionContext executionContext, ContextCallback callback, Object state)
     at JetBrains.Util.Concurrency.ExecutionContextEx.Run(ExecutionContext context, ContextCallback callback, Object state)
     at JetBrains.Threading.ReentrancyGuard.Execute(String name, Action action)
     at JetBrains.Threading.ReentrancyGuard.ExecutePendingActions()
     at JetBrains.Util.Concurrency.ExecutionContextEx.<>c__DisplayClass0_0.<Run>b__0(Object _)
     at System.Threading.ExecutionContext.RunInternal(ExecutionContext executionContext, ContextCallback callback, Object state)
     at JetBrains.Util.Concurrency.ExecutionContextEx.Run(ExecutionContext context, ContextCallback callback, Object state)
     at JetBrains.Threading.JetDispatcher.Closure.Execute()
     at JetBrains.Threading.JetDispatcher.Run()
     at JetBrains.Rider.Backend.Product.RiderMain.Main(Lifetime lifetime, IRiderSettings settings, ILogger logger, ProtocolComponent protocol, RdShellModel shellModel, ApplicationShutdownRequests shutdownRequests)
     at System.RuntimeMethodHandle.InvokeMethod(Object target, Span`1& arguments, Signature sig, Boolean constructor, Boolean wrapExceptions)
     at System.Reflection.RuntimeMethodInfo.Invoke(Object obj, BindingFlags invokeAttr, Binder binder, Object[] parameters, CultureInfo culture)
     at JetBrains.Application.Environment.RunsPublicStaticIntMain.<>c__DisplayClass0_0.<.ctor>b__0()
     at JetBrains.Application.Environment.RunsPublicStaticIntMain.<>c__DisplayClass0_2.<.ctor>b__5()
     at JetBrains.Util.Logging.Logger.Catch(Action action)
     at JetBrains.Application.Threading.IThreadingEx.<>c__DisplayClass18_1.<ExecuteOrQueueWhenNotGuarded>b__4()
     at JetBrains.Util.Concurrency.ExecutionContextEx.<>c__DisplayClass1_0.<Run>b__0(Object _)
     at JetBrains.Util.Concurrency.ExecutionContextEx.<>c__DisplayClass0_0.<Run>b__0(Object _)
     at System.Threading.ExecutionContext.RunInternal(ExecutionContext executionContext, ContextCallback callback, Object state)
     at System.Threading.ExecutionContext.Run(ExecutionContext executionContext, ContextCallback callback, Object state)
     at JetBrains.Util.Concurrency.ExecutionContextEx.Run(ExecutionContext context, ContextCallback callback, Object state)
     at JetBrains.Util.Concurrency.ExecutionContextEx.Run(ExecutionContext context, Action action)
     at JetBrains.Threading.JetDispatcher.Closure.<>c__DisplayClass10_1.<.ctor>b__0()
     at JetBrains.Threading.JetDispatcher.Closure.Execute()
     at JetBrains.Threading.JetDispatcher.Run(Func`1 condition, TimeSpan timeout, Boolean bThrowOnTimeout)
     at JetBrains.Application.Environment.IJetHostEx.RunHostMessageLoop(IComponentContainer containerEnv)
     at JetBrains.Application.Environment.HostParameters.MessagePumpMainLoopHostMixin.JetBrains.Application.Environment.HostParameters.IRunMainLoopHostMixin.RunMainLoop(ComponentContainer containerEnv)
     at JetBrains.Application.Environment.HostParameters.JetHostParametersCaller.RunMainLoop(ComponentContainer containerEnv)
     at JetBrains.Application.Environment.JetEnvironment.InternalRun(JetHostParametersCaller host, ComponentContainer containerEnv)
     at JetBrains.Application.Environment.JetEnvironment.CreateAndRun(Full hostparams)



     */


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
