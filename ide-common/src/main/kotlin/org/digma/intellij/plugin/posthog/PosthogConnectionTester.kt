package org.digma.intellij.plugin.posthog

import com.intellij.openapi.diagnostic.Logger
import com.posthog.java.shaded.okhttp3.OkHttpClient
import com.posthog.java.shaded.okhttp3.Request
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.TimeUnit

object PosthogConnectionTester {

    var LOGGER: Logger = Logger.getInstance(PosthogConnectionTester::class.java)

    //Note that this code uses the OkHttpClient class that is packaged with posthog
    // com.posthog.java.shaded.okhttp3.OkHttpClient
    fun isConnectionToPosthogOk(): Boolean {

        return try {

            val url = "https://app.posthog.com"

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request: Request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().isSuccessful

        } catch (e: Throwable) {
            Log.warnWithException(LOGGER, e, "could not connect to posthog {}", e)
            false
        }
    }

}