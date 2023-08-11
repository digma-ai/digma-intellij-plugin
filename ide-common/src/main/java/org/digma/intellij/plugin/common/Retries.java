package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import org.digma.intellij.plugin.log.Log;

public class Retries {

    private Retries() {
    }

    private static final Logger LOGGER = Logger.getInstance(Retries.class);

    public static void simpleRetry(Runnable runnable,Class<? extends Throwable> retryOn,int backOffMillis,int maxRetries){
        simpleRetry(runnable,retryOn,backOffMillis,maxRetries,1);
    }

    private static void simpleRetry(Runnable runnable,Class<? extends Throwable> retryOn,int backOffMillis,int maxRetries,int retryCount){

        try{
            runnable.run();
        }catch (Throwable e){

            Log.log(LOGGER::warn,"get exception "+e+ " retry "+retryCount);
            Log.warnWithException(LOGGER,e,"exception in simpleRetry");

            if (retryCount == maxRetries){
                throw e;
            }



            if (retryOn.isAssignableFrom(e.getClass())){
                try {
                    Log.log(LOGGER::warn,"sleeping");
                    Thread.sleep(backOffMillis);
                } catch (InterruptedException ex) {/* ignore*/}
                simpleRetry(runnable,retryOn,backOffMillis,maxRetries,retryCount+1);
            }else{
                throw e;
            }
        }
    }


}
