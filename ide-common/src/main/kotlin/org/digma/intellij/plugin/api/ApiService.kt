package org.digma.intellij.plugin.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.util.Computable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import kotlinx.coroutines.CoroutineScope
import org.digma.intellij.plugin.common.DisposableAdaptor
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.kotlin.ext.asyncWithErrorReporting
import org.digma.intellij.plugin.log.Log
import org.jetbrains.ide.RestService.Companion.getStringParameter

@Service(Service.Level.APP)
class ApiService(private val cs: CoroutineScope) : DisposableAdaptor {

    private val logger: Logger = Logger.getInstance(this::class.java)


    /**
     * A bridge method to run our code in a suspendable context so we can all suspend methods in our services.
     * executes synchronously.
     */
    fun executeRequest(
        httpService: AbstractHttpService,
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {

        Log.log(logger::trace, "executeRequest called for urlDecoder={}, thread={}", urlDecoder, Thread.currentThread().name)

        EDT.assertNonDispatchThread()

        //runBlockingCancellable must run inside a process
        return ProgressManager.getInstance().runProcess(Computable {

            //runBlockingCancellable is the bridge to suspendable code
            @Suppress("UnstableApiUsage")
            runBlockingCancellable {
                //can run the code
                val deferred = cs.asyncWithErrorReporting("ApiService.executeRequest.${httpService.getName()}", logger) {
                    httpService.executeRequest(urlDecoder, request, context)
                }

                return@runBlockingCancellable try {
                    deferred.await()
                } catch (e: Throwable) {
                    Log.warnWithException(logger, e, "error executing request for {}", httpService.getName())
                    null
                }
            }
        }, EmptyProgressIndicator())

    }


    suspend fun executeAction(action: String, urlDecoder: QueryStringDecoder) {

        val projectName = getStringParameter(PROJECT_NAME_PARAM_NAME, urlDecoder)
        val project = projectName?.let {
            findProjectOrNull(projectName)
        } ?: findActiveOrRecentProject()

        if (project == null) {
            Log.log(logger::warn, "could not find a project to use for api call")
            throw NoProjectException("could not find project $projectName nor any other project to use")
        }

        Log.log(logger::trace, "found project to use {}", project.name)

        project.service<ApiProjectService>().executeAction(action, urlDecoder)
    }


}