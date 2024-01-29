package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import org.digma.intellij.plugin.log.Log;

import java.util.function.Supplier;

public class ReadActions {

    private static final Logger LOGGER = Logger.getInstance(ReadActions.class);

    public static void assertReadAccessAllowed() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
    }

    public static void assertNotInReadAccess() {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            RuntimeException runtimeException = new RuntimeException("Should not be in read access here");
            Log.error(LOGGER, runtimeException, "Read access is allowed but should not be here");
        }
    }


    public static <T> T ensureReadAction(Supplier<T> tSupplier){
        if (ApplicationManager.getApplication().isReadAccessAllowed()){
            return tSupplier.get();
        }else{
            return ReadAction.compute(tSupplier::get);
        }
    }


    public static <T> T ensureReadActionWithProgress(Supplier<T> tSupplier) {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            return ProgressManager.getInstance().computeInNonCancelableSection(tSupplier::get);
        } else {
            return ReadAction.compute(() -> ProgressManager.getInstance().computeInNonCancelableSection(tSupplier::get));
        }
    }




    public static void ensureReadAction(Runnable runnable){
        if (ApplicationManager.getApplication().isReadAccessAllowed()){
            runnable.run();
        }else{
            ReadAction.run(runnable::run);
        }
    }

}
