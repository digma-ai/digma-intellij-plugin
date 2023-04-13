package org.digma.intellij.plugin.common;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;

import java.util.function.Supplier;

public class ReadActions {


    public static <T> T ensureReadAction(Supplier<T> tSupplier){
        if (ApplicationManager.getApplication().isReadAccessAllowed()){
            return tSupplier.get();
        }else{
            return ReadAction.compute(tSupplier::get);
        }
    }

}
