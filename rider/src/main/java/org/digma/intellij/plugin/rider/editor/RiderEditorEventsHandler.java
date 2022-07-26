package org.digma.intellij.plugin.rider.editor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.document.DocumentInfoChanged;
import org.digma.intellij.plugin.editor.EditorEventsHandler;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.rider.ServicesStarter;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RiderEditorEventsHandler extends LifetimedProjectComponent implements EditorEventsHandler, DocumentInfoChanged {

    private final Logger LOGGER = Logger.getInstance(RiderEditorEventsHandler.class);

    private final CaretContextService caretContextService;
    private final LanguageService languageService;

    private MessageBusConnection documentInfoChangedMessageBusConnection;
    private MessageBusConnection fileEditorMessageBusConnection;

    private boolean initialized = false;

    public RiderEditorEventsHandler(Project project) {
        super(project);
        languageService = project.getService(LanguageService.class);
        caretContextService = project.getService(CaretContextService.class);
    }

    //start is called once per project when the tool window is created
    @Override
    public void start(@NotNull Project project) {

        initialized = true;

        Log.log(LOGGER::debug,"Starting RiderEditorEventsHandler");

        ServicesStarter.loadStartupServices(project);

        documentInfoChangedMessageBusConnection = project.getMessageBus().connect();
        documentInfoChangedMessageBusConnection.subscribe(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC,this);

        //initializing ElementUnderCaretDetector only here when the tool window is opened.
        //ElementUnderCaret events are only necessary if the tool windows was created already, otherwise its useless
        //processing.
        var elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
        Objects.requireNonNull(elementUnderCaretDetector);



        //todo: keep this code as example until we are sure we don't need it.
        fileEditorMessageBusConnection = project.getMessageBus().connect();
        fileEditorMessageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
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
                if (newFile == null){
                    return;
                }

                //the file may be supported but not writable,for example when we open vcs files.
                if (!languageService.isSupportedFile(project,newFile) || !newFile.isWritable()){
                    Log.log(LOGGER::debug, "Non supported file opened, clearing context. {}", newFile);
                    caretContextService.contextEmptyNonSupportedFile(newFile.getUrl());
                    elementUnderCaretDetector.emptyModel();
                }else{
                    //sometimes when selection changes from a non-supported file to a supported file the supported
                    //file editor will not gain focus, elementUnderCaretDetector.refresh will compensate.
                    var oldFile = event.getOldFile();
                    if (oldFile != null && (!languageService.isSupportedFile(project,oldFile) || !oldFile.isWritable())){
                        elementUnderCaretDetector.refresh();
                    }

                }
            }
        });
    }



    @Override
    public void documentInfoChanged(PsiFile psiFile) {
        //there's no need to refresh ElementUnderCaret if the tool window wasn't opened yet
        if (initialized) {
            ElementUnderCaretDetector elementUnderCaretDetector = getProject().getService(ElementUnderCaretDetector.class);
            elementUnderCaretDetector.refresh();
        }
    }




    @Override
    public void dispose() {
        super.dispose();
        if (documentInfoChangedMessageBusConnection != null){
            documentInfoChangedMessageBusConnection.dispose();
        }
        if (fileEditorMessageBusConnection != null){
            fileEditorMessageBusConnection.dispose();
        }
    }
}
