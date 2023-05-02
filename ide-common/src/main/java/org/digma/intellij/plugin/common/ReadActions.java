package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;

import java.util.function.Supplier;

public class ReadActions {


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
