package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.PluginId;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.rider.ServicesStarter;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;
import org.digma.intellij.plugin.rider.psi.csharp.CSharpLanguageService;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A tool windows listener that initializes some services and resources only when the tool window is opened
 * at least once.
 * Some listeners are pointless if the user did not open the tool windows. for example there is no need to update
 * the panels UI if the tool window is hidden. if the user opened the tool window these listeners will start and keep
 * until the project is closed. user needs to open the tool window at least once, that means he is using the plugin.
 */
public class RiderEditorEventsStarter implements ToolWindowManagerListener, DocumentInfoChanged {

    private final Logger LOGGER = Logger.getInstance(RiderEditorEventsStarter.class);

    private final Project project;
    private final CaretContextService caretContextService;
    private final CSharpLanguageService cSharpLanguageService;


    private boolean initialized = false;

    public RiderEditorEventsStarter(Project project) {
        this.project = project;
        cSharpLanguageService = project.getService(CSharpLanguageService.class);
        caretContextService = project.getService(CaretContextService.class);
    }


    /**
     * This event will be called every time the tool window is shows after it was hidden. but we want to initialize
     * our listeners only once, and we check it with initialized flag.
     * There is no need to synchronize access to initialized flag, showing the tool window can't happen by multiple
     * threads simultaneously
     */
    @Override
    public void toolWindowShown(@NotNull ToolWindow toolWindow) {
        if (PluginId.TOOL_WINDOW_ID.equals(toolWindow.getId()) && !initialized) {
            start();
            initialized = true;
        }
    }


    //start is calle d once per project when the tool window is shown
    private void start() {

        Log.log(LOGGER::debug, "Starting RiderEditorEventsStarter");

        ServicesStarter.loadStartupServices(project);

        project.getMessageBus().connect(caretContextService).subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC, this);

        //initializing ElementUnderCaretDetector only here when the tool window is opened.
        //ElementUnderCaret events are only necessary if the tool windows was created already, otherwise its useless
        //processing.
        var elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        Objects.requireNonNull(elementUnderCaretDetector);


        project.getMessageBus().connect(caretContextService).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            //resharper does not send an event on non-supported files,and when a non-supported file is opened our context is still shown
            // in the tool window.
            //sometimes resharper will send a caret event when a source file looses focus, so in ElementUnderCaretHost, if there
            // is a caret event for a non-visible source file the context is cleared and sends an event to the fronend, this has the same
            // effect as the listener here. but that doesn't always happen so we still need this listener here.

            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditorManager fileEditorManager = event.getManager();
                Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                        event.getNewFile(), event.getOldFile());

                var newFile = event.getNewFile();
                //the file may be supported but not writable,for example when we open vcs files.
                if (newFile != null && (!cSharpLanguageService.isSupportedFile(project, newFile) || !newFile.isWritable())) {
                    Log.log(LOGGER::debug, "Non supported file opened, clearing context. {}", newFile);
                    caretContextService.contextEmptyNonSupportedFile(newFile.getUrl());
                    elementUnderCaretDetector.emptyModel();
                } else {
                    //sometimes when selection changes from a non-supported file to a supported file the supported
                    //file editor will not gain focus, elementUnderCaretDetector.refresh will compensate.
                    var oldFile = event.getOldFile();
                    if (oldFile != null && (!cSharpLanguageService.isSupportedFile(project, oldFile) || !oldFile.isWritable())) {
                        elementUnderCaretDetector.refresh();
                    }
                }
            }


            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (!cSharpLanguageService.isSupportedFile(project, file) || !file.isWritable()) {
                    elementUnderCaretDetector.refresh();
                }
            }
        });
    }


    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        //refresh element under caret, sometimes the document that opens does not grab focus and
        // element under caret event is not fired, this will cause an element under caret event.
        // there's no need to refresh if the tool window wasn't opened yet
        if (initialized && cSharpLanguageService.isSupportedFile(project, psiFile)) {
            ElementUnderCaretDetector elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
            elementUnderCaretDetector.refresh();
        }
    }

}
