package org.digma.intellij.plugin.service;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageConstants;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.BinaryLightVirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import kotlin.Pair;
import kotlin.Triple;
import org.apache.commons.io.IOUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.vcs.VcsService;
import org.digma.intellij.plugin.vf.DigmaStackTraceVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EditorService implements Disposable {

    public static final String STACKTRACE_PREFIX = "digma-stacktrace";
    public static final String VCS_PREFIX = "digma-vcs";

    private static final Logger LOGGER = Logger.getInstance(EditorService.class);

    private final Project project;

    private final VcsService vcsService;


    public EditorService(Project project) {
        this.project = project;
        vcsService = project.getService(VcsService.class);
    }

    public static EditorService getInstance(Project project) {
        Log.log(LOGGER::warn, "Getting instance of " + EditorService.class.getSimpleName());
        return project.getService(EditorService.class);
    }

    public void openErrorFrameWorkspaceFileInEditor(@NotNull String workspaceUrl, @Nullable String lastInstanceCommitId, int lineNumber) {

        var workspaceFile = VirtualFileManager.getInstance().findFileByUrl(workspaceUrl);
        if (workspaceFile == null) {
            NotificationUtil.notifyError(project, "Could not find file " + workspaceUrl);
            return;
        }

        if (lastInstanceCommitId == null) {
            openVirtualFile(workspaceFile, lineNumber);
        } else {
            tryOpenFromVcs(workspaceFile, lastInstanceCommitId, lineNumber);
        }
    }

    private void tryOpenFromVcs(@NotNull VirtualFile workspaceFile, @NotNull String lastInstanceCommitId, int lineNumber) {

        try {
            //if file not under vcs then the workspaceFile will open
            if (vcsService.isFileUnderVcs(workspaceFile)) {
                //if revision doesn't exist show a warning and then the workspaceFile will open
                if (vcsService.isRevisionExist(workspaceFile, lastInstanceCommitId)) {
                    //if no changes then the workspaceFile will open
                    if (vcsService.isLocalContentChanged(workspaceFile, lastInstanceCommitId, lineNumber)) {
                        //for fast re-opening of files that are already opened, the user already answered yes for this file.
                        //if there are changes with working dir then try to check if the file was already opened.
                        //try to build a vcs file name and showIfAlreadyOpen without loading the revision again, it may be faster.
                        //if buildVcsFileName did not succeed that doesn't mean it's not possible,
                        // vcsService.getRevisionVirtualFile may succeed because it's a query to vcs and may find a suitable revision.
                        var vcsFileName = buildVcsFileName(workspaceFile, lastInstanceCommitId);
                        if (showIfAlreadyOpen(vcsFileName, lineNumber)) {
                            return;
                        }

                        VirtualFile vcsFile = vcsService.getRevisionVirtualFile(workspaceFile, lastInstanceCommitId);
                        if (vcsFile != null) {
                            maybeOpenVcsFile(workspaceFile, (ContentRevisionVirtualFile) vcsFile, lineNumber);
                            return;
                        } else {
                            Messages.showWarningDialog(project, "Could not load vcs file for  " + workspaceFile + ", Opening workspace file.", "");
                        }
                    }
                } else {
                    Messages.showWarningDialog(project, "Revision " + lastInstanceCommitId + " for " + workspaceFile + " was not found, Opening workspace file.", "");
                }
            }
        } catch (VcsException e) {
            Log.log(LOGGER::warn, "Could not query vcs for file {} ", workspaceFile);
            var vcsErrorMsgResult = Messages.showOkCancelDialog(project, "Can not query VCS for file, Show Workspace file ?", "VCS error:" + e.getMessage(), "Ok", "Cancel", AllIcons.General.Error);
            if (vcsErrorMsgResult == MessageConstants.CANCEL) {
                return;
            }
        }

        //if we didn't try to maybeOpenVcsFile or if there was a VcsException then open the workspace file
        openVirtualFile(workspaceFile, lineNumber);
    }


    private String buildVcsFileName(@NotNull VirtualFile workspaceFile, String lastInstanceCommitId) {
        var hash = vcsService.getShortRevisionString(workspaceFile, lastInstanceCommitId);
        return hash == null ? null : buildVcsFileName(hash, workspaceFile);
    }


    private String buildVcsFileName(String hash, @NotNull VirtualFile workspaceFile) {
        if (hash != null) {
            var filePath = workspaceFile.getPath();
            var indexAfterSlash = Math.max(filePath.lastIndexOf('/') + 1, 0);
            var fileName = filePath.substring(indexAfterSlash);
            return VCS_PREFIX + "-" + hash + "-" + fileName;
        } else {
            return null;
        }
    }


    private void maybeOpenVcsFile(@NotNull VirtualFile workspaceFile, @NotNull ContentRevisionVirtualFile vcsFile, int lineNumber) {

        var hash = vcsService.getShortRevisionString(vcsFile.getContentRevision().getRevisionNumber());
        String name = buildVcsFileName(hash, workspaceFile);

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

    private Editor openVirtualFile(VirtualFile virtualFile, int lineNumber) {
        OpenFileDescriptor navigable = new OpenFileDescriptor(project, virtualFile, Math.max(0, lineNumber - 1), 0);
        return FileEditorManager.getInstance(project).openTextEditor(navigable, true);
    }

    public void openVirtualFile(@NotNull VirtualFile virtualFile, boolean readOnly) {
        Editor editor = openVirtualFile(virtualFile, 1);
        if (readOnly) {
            if (editor instanceof EditorEx) {
                var editorEx = (EditorEx) editor;
                editorEx.setViewer(true);
            }
        }
    }

    private boolean showIfAlreadyOpen(String name, int lineNumber) {

        if (name == null) {
            return false;
        }

        var openedFile = Arrays.stream(FileEditorManager.getInstance(project).getOpenFiles())
                .filter(virtualFile -> name.equals(virtualFile.getName())).findAny().orElse(null);

        if (openedFile != null) {
            openVirtualFile(openedFile, lineNumber);
            return true;
        }

        return false;
    }


    @Nullable
    public Triple<VirtualFile, Editor, Boolean> openWorkspaceFileInEditor(@NotNull String workspaceUri, int offset) {

        var fileToOpen = VirtualFileManager.getInstance().findFileByUrl(workspaceUri);
        if (fileToOpen == null) {
            NotificationUtil.notifyError(project, "Could not find file " + workspaceUri);
            return null;
        }
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, fileToOpen, Math.max(offset, 0));
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        boolean fileWasAlreadyOpen = fileEditorManager.isFileOpen(fileToOpen);
        Editor editor = fileEditorManager.openTextEditor(openFileDescriptor, true);
        return new Triple<>(fileToOpen, editor, fileWasAlreadyOpen);
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

            var vf = new DigmaStackTraceVirtualFile(name, stackTrace);
            //when setting file type it doesn't work..
//            vf.setFileType(DigmaStackTraceFileType.INSTANCE);
            vf.setWritable(false);
            openVirtualFile(vf, 0);

        } catch (Exception e) {
            Log.log(LOGGER::warn, "Could not open stack trace. " + e.getMessage());
            NotificationUtil.notifyError(project, "Could not open stack trace. " + e.getMessage());
        }
    }


    /**
     * opens a file created from a classpath resource that must be a text file.
     * @param name the name of the editor
     * @param resourcePath the classpath resource path
     */
    public void openClasspathResourceReadOnly(String name, String resourcePath) {

        if (resourcePath == null || resourcePath.isBlank()) {
            NotificationUtil.showNotification(project, "openClasspathResourceReadOnly was called with empty resourcePath");
            return;
        }
        //create unique name

        if (showIfAlreadyOpen(name, 0)) {
            return;
        }

        try (var inputStream = getClass().getResourceAsStream(resourcePath)) {

            if (inputStream != null) {
                var content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                var vf = new LightVirtualFile(name, content);
                vf.setWritable(false);
                openVirtualFile(vf, 0);
            }else{
                Log.log(LOGGER::debug, "Could not load input stream for classpath resource {}, {}",resourcePath);
            }

        } catch (Exception e) {
            Log.log(LOGGER::warn, "Could not open classpath resource {}, {}",resourcePath, e.getMessage());
            NotificationUtil.notifyError(project, "Could not open classpath resource " + resourcePath);
        }
    }


    @Override
    public void dispose() {
        //nothing to do
    }

    @Nullable
    public Pair<String, Integer> getCurrentCaretLocation() {
        var selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        if (selectedTextEditor != null){
            var file = FileDocumentManager.getInstance().getFile(selectedTextEditor.getDocument());
            var offset = selectedTextEditor.getCaretModel().getOffset();

            if (file != null){
                return new Pair<>(file.getUrl(),offset);
            }

        }

        return null;
    }
}
