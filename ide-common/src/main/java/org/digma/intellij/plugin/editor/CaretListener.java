package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens to CaretEvents and updates view respectively.
 */
class CaretListener {

    private static final Logger LOGGER = Logger.getInstance(CaretListener.class);

    private final Project project;
    private final CurrentContextUpdater currentContextUpdater;


    /**
     * CaretListener registers CaretListeners on editors, those listeners need to be removed from
     * the editor on editor close and on ide shutdown. the recommended jetbrains way is to register
     * listeners with a parent disposable. CaretListener registers a parent disposable for each
     * installed listener, on fileClosed the parent disposable should be disposed so that the listener
     * is removed, disposables keeps those parent disposables, they are disposed on fileClosed.
     */
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();


    CaretListener(Project project, CurrentContextUpdater currentContextUpdater) {
        this.project = project;
        this.currentContextUpdater = currentContextUpdater;
    }


    //don't install listeners on non-supported files, this method shouldn't be called for non-supported files.
    void maybeAddCaretListener(@NotNull Editor editor) {

        if (editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing listener for {} because it is disposed", editor);
            return;
        }

        var psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return;
        }

        var file = psiFile.getVirtualFile();

        //this is a check if this editor already has a caret listener
        if (disposables.containsKey(file)) {
            return;
        }

        addCaretListener(editor, file);
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


        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(file, parentDisposable);
        Log.log(LOGGER::debug, "adding caret listener for file:{}", file);
        editor.getCaretModel().addCaretListener(new com.intellij.openapi.editor.event.CaretListener() {

            @Override
            public void caretPositionChanged(@NotNull CaretEvent caretEvent) {

                try {
                    if (caretEvent.getCaret() != null) {
                        int caretOffset = caretEvent.getEditor().logicalPositionToOffset(caretEvent.getNewPosition());
                        var file = FileDocumentManager.getInstance().getFile(caretEvent.getEditor().getDocument());
                        currentContextUpdater.addRequest(caretEvent.getEditor(), caretOffset, file);
                    }
                }catch (Exception e){
                    Log.warnWithException(LOGGER, e, "Exception in caretPositionChanged");
                    ErrorReporter.getInstance().reportError(project, "CaretListener.caretPositionChanged", e);
                }
            }
        }, parentDisposable);
    }


    void removeCaretListener(VirtualFile file) {
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }

}
