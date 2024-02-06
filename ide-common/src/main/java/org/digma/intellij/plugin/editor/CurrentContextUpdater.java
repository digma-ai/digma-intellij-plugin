package org.digma.intellij.plugin.editor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.*;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.psi.*;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.jetbrains.annotations.NotNull;

/**
 * Processes requests to update the current method context as a result of caret
 * event or document change event.
 */
public class CurrentContextUpdater implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(CurrentContextUpdater.class);
    private final Project project;

    private final CaretContextService caretContextService;
    private final LanguageServiceLocator languageServiceLocator;

    private final Alarm caretEventAlarm = AlarmFactory.getInstance().create(Alarm.ThreadToUse.POOLED_THREAD,this);

    /*
   keep the latest method under caret that was fired. it helps us to not call contextChange if the caret is on the same
   method as before.
    */
    private MethodUnderCaret latestMethodUnderCaret;

    public CurrentContextUpdater(Project project) {
        this.project = project;
        caretContextService = CaretContextService.getInstance(project);
        languageServiceLocator = LanguageServiceLocator.getInstance(project);
    }


    @Override
    public void dispose() {
        caretEventAlarm.dispose();
    }


    void cancelAllCaretPositionChangedRequests() {
        caretEventAlarm.cancelAllRequests();
    }

    public void clearLatestMethod() {
        latestMethodUnderCaret = null;
    }



    void addRequest(Editor editor, int caretOffset, VirtualFile file) {
        //process the most recent event after a quite period of delayMillis
        caretEventAlarm.cancelAllRequests();
        caretEventAlarm.addRequest(() -> {

            try {
                Log.log(LOGGER::debug, "caretPositionChanged for editor:{}", editor);
                updateCurrentContext(editor, caretOffset, file);
            }catch (Exception e){
                Log.warnWithException(LOGGER, e, "exception while processing updateCurrentContext event for file: {}", file);
                ErrorReporter.getInstance().reportError(project, "CurrentContextUpdater.addRequest", e);
            }
        },300);
    }

    private void updateCurrentContext(@NotNull Editor editor, int caretOffset, VirtualFile file) {

        EDT.assertNonDispatchThread();

        //there is no need to check if file is supported, we install caret listener only on editors of supported files.
        Log.log(LOGGER::debug, "updateCurrentContext for editor:{}, file: {}", editor, file);

        PsiFile psiFile = PsiAccessUtilsKt.runInReadAccessInSmartModeWithResult(project, () -> PsiManager.getInstance(project).findFile(file));

        if (!PsiUtils.isValidPsiFile(psiFile)) {
            Log.log(LOGGER::debug, "psi file not found or is not valid for file: {}", file);
            return;
        }
        updateCurrentContext(editor, caretOffset, psiFile);
    }

    private void updateCurrentContext(@NotNull Editor editor, int caretOffset, PsiFile psiFile) {
        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        Log.log(LOGGER::debug, "found language service {} for file: {}", languageService, psiFile.getVirtualFile());

        MethodUnderCaret methodUnderCaret = DumbService.getInstance(project).runReadActionInSmartMode(() -> languageService.detectMethodUnderCaret(project, psiFile, editor, caretOffset));

        Log.log(LOGGER::debug, "found MethodUnderCaret for file: {},'{}", psiFile.getVirtualFile(), methodUnderCaret);
        //don't call contextChange if the caret is still on the same method or in same lambda scope. when moving between
        // lambda scope the context will update. this is necessary for some frameworks in kotlin.
        if (methodUnderCaret.equals(latestMethodUnderCaret)) {
            Log.log(LOGGER::debug, "not updating MethodUnderCaret because it is the same as latest, for file: {},'{}", psiFile.getVirtualFile(), methodUnderCaret);
            return;
        }
        latestMethodUnderCaret = methodUnderCaret;
        Log.log(LOGGER::debug, "contextChanged for file: {}, with method under caret '{}", psiFile.getVirtualFile(), methodUnderCaret);
        caretContextService.contextChanged(methodUnderCaret);
    }


}
