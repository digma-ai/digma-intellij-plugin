package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


/**
 * Listens to FileEditorManager events and CaretEvents and updates view respectively.
 */
public class EditorListener implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorListener.class);

    private boolean active = false;
    private final Project project;
    private final LocalPsiEditorEventsHandler localPsiEditorEventsHandler;
    private MessageBusConnection messageBusConnection;

    /**
     * EditorListener registers CaretListeners on editors, those listeners need to be removed from
     * the editor on editor close and on ide shutdown. the recommended jetbrains way is to register
     * listeners with a parent disposable. EditorListener registers a parent disposable for each
     * installed listener, on fileClosed the parent disposable should be disposed so that the listener
     * is removed, disposables keeps those parent disposables, they are disposed on fileClosed and stop.
     */
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();


    public EditorListener(@NotNull Project project, @NotNull LocalPsiEditorEventsHandler editorEventsHandler) {
        this.project = project;
        this.localPsiEditorEventsHandler = editorEventsHandler;
    }


    @Override
    public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileOpened: file:{}", file);
    }


    @Override
    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileClosed: file:{}", file);
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "fileClosed: disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }


    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {
        FileEditorManager fileEditorManager = editorManagerEvent.getManager();
        Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());

        var newFile = editorManagerEvent.getNewFile();

        //ignore non supported files. newFile may be null when the last editor is closed.
        if (newFile != null && localPsiEditorEventsHandler.isSupportedFile(newFile)) {
            var editor = fileEditorManager.getSelectedTextEditor();
            if (editor != null && !disposables.containsKey(newFile)) {
                addCaretListener(editor, newFile);
            }

            if (editor != null) {
                Log.log(LOGGER::debug, "selectionChanged: updating with file:{}", newFile);
                updateCurrentElement(editor.getCaretModel().getOffset(), newFile);
            }
        } else {
            localPsiEditorEventsHandler.emptySelection();
        }
    }


    //don't install listeners on non-supported files, this method shouldn't be called for unsupported files.
    private void addCaretListener(@NotNull Editor editor, @NotNull VirtualFile newFile) {

        if (editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing listener for {} because it is disposed", editor);
            return;
        }

        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(newFile, parentDisposable);
        Log.log(LOGGER::debug, "adding caret listener for file:{}", newFile);
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent caretEvent) {
                Log.log(LOGGER::debug, "caretPositionChanged for editor:{}", caretEvent.getEditor());
                if (caretEvent.getCaret() != null) {
                    int caretOffset = caretEvent.getCaret().getOffset();
                    updateCurrentElement(caretOffset, newFile);
                }
            }
        }, parentDisposable);
    }


    private void updateCurrentElement(int caretOffset, @NotNull VirtualFile file) {
        localPsiEditorEventsHandler.updateCurrentElement(caretOffset, file);
    }


    public void start() {
        if (active) {
            Log.log(LOGGER::error, "trying to start an already active listener");
            return;
        }
        Log.log(LOGGER::debug, "starting");
        messageBusConnection = project.getMessageBus().connect(localPsiEditorEventsHandler);
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
        installOnCurrentlyOpenedEditors();
        active = true;
    }

    /*
    EditorListener is started only when the tool window is opened,not before that because we don't
     want or need to listen to events before the window is opened.
    at that stage there may be already opened editors that we didn't install a caret listener for them,
     they will be caught in the following selectionChanged events. but we still want to update the content
     with the currently opened file and install a listener for the editor.
    this code assumes there is only one selected editor.
     */
    private void installOnCurrentlyOpenedEditors() {
        var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        var fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (editor != null && fileEditor != null && fileEditor.getFile() != null &&
                localPsiEditorEventsHandler.isSupportedFile(fileEditor.getFile())) {
            addCaretListener(editor, fileEditor.getFile());
            updateCurrentElement(editor.getCaretModel().getOffset(), fileEditor.getFile());
        }
    }


    public void stop() {
        if (active) {
            Log.log(LOGGER::debug, "stopping...");
            messageBusConnection.disconnect();
            disposables.values().forEach(Disposer::dispose);
            active = false;
        }
    }
}
