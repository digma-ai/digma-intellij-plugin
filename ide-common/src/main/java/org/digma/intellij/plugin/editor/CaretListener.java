package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import com.intellij.util.AlarmFactory;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to CaretEvents and updates view respectively.
 * This class is only used from the current package by EditorEventsHandler so it and its methods are
 * 'package' access.
 */
class CaretListener {

    private static final Logger LOGGER = Logger.getInstance(CaretListener.class);
    private final Project project;
    private final CaretContextService caretContextService;
    private final LanguageServiceLocator languageServiceLocator;
    private final Alarm caretEventAlarm;

    /*
    keep the latest method under caret that was fired. it helps us to not call contextChange if the caret is on the same
    method as before.
     */
    private MethodUnderCaret latestMethodUnderCaret;

    /**
     * CaretListener registers CaretListeners on editors, those listeners need to be removed from
     * the editor on editor close and on ide shutdown. the recommended jetbrains way is to register
     * listeners with a parent disposable. CaretListener registers a parent disposable for each
     * installed listener, on fileClosed the parent disposable should be disposed so that the listener
     * is removed, disposables keeps those parent disposables, they are disposed on fileClosed.
     */
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();


    CaretListener(Project project) {
        this.project = project;
        caretContextService = project.getService(CaretContextService.class);
        languageServiceLocator = project.getService(LanguageServiceLocator.class);
        caretEventAlarm = AlarmFactory.getInstance().create();
    }


    //don't install listeners on non-supported files, this method shouldn't be called for non-supported files.
    void maybeAddCaretListener(@NotNull Editor editor, @NotNull VirtualFile file) {

        if (disposables.containsKey(file)) {
            return;
        }

        addCaretListener(editor, file);
    }


    void cancelAllCaretPositionChangedRequests() {
        caretEventAlarm.cancelAllRequests();
    }


    private void addCaretListener(@NotNull Editor editor, @NotNull VirtualFile file) {

        /*
        We need to know where the caret is all the time.
        there are two options: a mouse listener or caret listener,each has advantages and disadvantages.
        a mouse listener is more fine-grained in knowing whet the user is doing, if it's a click or a mouse movement.
        but it fires too many events and forces thinking about left click,right click etc.
        a caret listener is easier to process. but we need to consider a quite period before processing,if the user
        clicks the editor quickly or moves around with the keyboard.
         */

        if (editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing listener for {} because it is disposed", editor);
            return;
        }

        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(file, parentDisposable);
        Log.log(LOGGER::debug, "adding caret listener for file:{}", file);
        editor.getCaretModel().addCaretListener(new com.intellij.openapi.editor.event.CaretListener() {

            @Override
            public void caretPositionChanged(@NotNull CaretEvent caretEvent) {

                //process the most recent event after a quite period of delayMillis
                caretEventAlarm.cancelAllRequests();
                caretEventAlarm.addRequest(() -> {
                    Log.log(LOGGER::debug, "caretPositionChanged for editor:{}", caretEvent.getEditor());
                    if (caretEvent.getCaret() != null) {
                        int caretOffset = caretEvent.getEditor().logicalPositionToOffset(caretEvent.getNewPosition());
                        var file = FileDocumentManager.getInstance().getFile(caretEvent.getEditor().getDocument());
                        updateCurrentContext(caretOffset, file);
                    }
                }, 300);
            }
        }, parentDisposable);
    }


    private void updateCurrentContext(int caretOffset, VirtualFile file) {

        //there is no need to check if file is supported, we install caret listener only on editors of supported files.

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return;
        }
        updateCurrentContext(caretOffset, psiFile);
    }

    private void updateCurrentContext(int caretOffset, PsiFile psiFile) {
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        MethodUnderCaret methodUnderCaret = languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
        //don't call contextChange if the caret is still on the same method
        if (methodUnderCaret.equals(latestMethodUnderCaret) ){
            return;
        }
        latestMethodUnderCaret = methodUnderCaret;
        caretContextService.contextChanged(methodUnderCaret);
    }


    void removeCaretListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }

}
