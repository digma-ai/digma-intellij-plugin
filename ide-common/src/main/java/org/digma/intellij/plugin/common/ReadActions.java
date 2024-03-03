package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.*;
import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;

import java.util.function.Supplier;

public class ReadActions {

    private static final Logger LOGGER = Logger.getInstance(ReadActions.class);

    public static void assertReadAccessAllowed() {
        ApplicationManager.getApplication().assertReadAccessAllowed();
    }

    public static void assertNotInReadAccess() {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            RuntimeException runtimeException = new RuntimeException("Must not run in read access");
            LOGGER.error("Must not run in read access", runtimeException);
            ErrorReporter.getInstance().reportInternalFatalError("assertNotInReadAccess", runtimeException);
            throw runtimeException;
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

    public static boolean isReadAccessAllowed() {
        return ApplicationManager.getApplication().isReadAccessAllowed();
    }
}
