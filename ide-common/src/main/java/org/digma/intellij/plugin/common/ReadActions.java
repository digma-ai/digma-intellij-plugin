package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;

import java.util.function.Supplier;

public class ReadActions {

    private static final Logger LOGGER = Logger.getInstance(ReadActions.class);

    public static void assertReadAccessAllowed() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
    }

    public static void assertNotInReadAccess() {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new RuntimeException("Should not be in read access here");
        }
    }


    public static <T> T ensureReadAction(Supplier<T> tSupplier){
        if (ApplicationManager.getApplication().isReadAccessAllowed()){
            return tSupplier.get();
        }else{
            return ReadAction.compute(tSupplier::get);
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
