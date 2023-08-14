package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.log.Log;

import java.util.function.Supplier;

public class Retries {

    private Retries() {
    }

    private static final Logger LOGGER = Logger.getInstance(Retries.class);

    public static void simpleRetry(Runnable runnable,Class<? extends Throwable> retryOn,int backOffMillis,int maxRetries){
        simpleRetry(runnable,retryOn,backOffMillis,maxRetries,1);
    }


    public static <T> T retryWithResult(Supplier<T> tSupplier, Class<? extends Throwable> retryOn, int backOffMillis, int maxRetries){
        return simpleRetryWithResult(tSupplier,retryOn,backOffMillis,maxRetries,1);
    }

    private static <T> T simpleRetryWithResult(Supplier<T> tSupplier, Class<? extends Throwable> retryOn, int backOffMillis, int maxRetries, int retryCount) {
        try{
            Log.log(LOGGER::debug,"starting retry "+retryCount);
            return tSupplier.get();
        }catch (Throwable e){

            Log.log(LOGGER::warn,"got exception "+e+ " retry "+retryCount);
            Log.warnWithException(LOGGER,e,"exception in simpleRetry");

            if (retryCount == maxRetries){
                throw e;
            }

            if (retryOn.isAssignableFrom(e.getClass())){
                try {
                    Log.log(LOGGER::warn,"sleeping {} millis",backOffMillis);
                    Thread.sleep(backOffMillis);
                } catch (InterruptedException ex) {/* ignore*/}
                simpleRetryWithResult(tSupplier,retryOn,backOffMillis,maxRetries,retryCount+1);
            }else{
                throw e;
            }
        }
        return null;
    }


    private static void simpleRetry(Runnable runnable,Class<? extends Throwable> retryOn,int backOffMillis,int maxRetries,int retryCount){

        try{
            Log.log(LOGGER::debug,"starting retry "+retryCount);
            runnable.run();
        }catch (Throwable e){

            Log.log(LOGGER::warn,"got exception "+e+ " retry "+retryCount);
            Log.warnWithException(LOGGER,e,"exception in simpleRetry");

            if (retryCount == maxRetries){
                throw e;
            }



            if (retryOn.isAssignableFrom(e.getClass())){
                try {
                    Log.log(LOGGER::warn,"sleeping {} millis",backOffMillis);
                    Thread.sleep(backOffMillis);
                } catch (InterruptedException ex) {/* ignore*/}
                simpleRetry(runnable,retryOn,backOffMillis,maxRetries,retryCount+1);
            }else{
                throw e;
            }
        }
    }


}
