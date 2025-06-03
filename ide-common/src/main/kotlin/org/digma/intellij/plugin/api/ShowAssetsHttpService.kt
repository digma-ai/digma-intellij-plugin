package org.digma.intellij.plugin.api

import com.intellij.openapi.components.service
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import kotlinx.coroutines.CancellationException
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.util.Objects

//http://localhost:63344/api/digma/show?action=GoToSpan&spanUid=995822fa-9d0d-11ef-9d3e-0adc1d604f09&targetTab=assets&projectName=spring-petclinic
//http://localhost:63344/api/digma/show?action=GoToError&errorId=6dc7a826-fb37-11ef-8a3f-7215f134c660&projectName=spring-petclinic
//http://localhost:63344/api/digma/show?action=GoToHome&environment_id=LOCAL%23ID%2325A68508-B8A3-4DF2-ACA3-ECDAAB203793&targetTab=assets&projectName=spring-petclinic
//http://localhost:63344/api/digma/show?action=OpenReport&projectName=spring-petclinic
class ShowAssetsHttpService : AbstractHttpService() {

    override fun getServiceName() = "digma/show"


    override suspend fun executeRequest(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {

        try {
            executeImpl(urlDecoder, request, context)
            sendOk(request, context)
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ErrorReporter.getInstance().reportError("ShowAssetsHttpService.execute", e)
            return "Error $e"
        }
    }

    private suspend fun executeImpl(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext) {

        val action = getStringParameter(ACTION_PARAM_NAME, urlDecoder)
        Objects.requireNonNull(action, "action parameter must not be null")

        Log.log(
            logger::trace,
            "execute called with action={} , thread={}",
            action,
            Thread.currentThread().name
        )

        action?.let {
            service<ApiService>().executeAction(it, urlDecoder)
        }
    }
}