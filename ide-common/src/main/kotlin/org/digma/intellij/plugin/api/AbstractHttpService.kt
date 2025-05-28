package org.digma.intellij.plugin.api

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService

/**
 * base http service that allows all origin hosts
 */
abstract class AbstractHttpService : RestService() {

    protected val logger: Logger = Logger.getInstance(this::class.java)

    final override fun execute(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {
        //execute in coroutine. RestService doesn't support coroutine context, and it needs
        // to execute synchronously. ApiService.executeRequest is a bridge to coroutine.
        val result = service<ApiService>().executeRequest(this, urlDecoder, request, context)
        return result
    }

    abstract suspend fun executeRequest(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String?

    fun getName(): String {
        return this.getServiceName()
    }

    override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
        return OriginCheckResult.ALLOW
    }

    override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
        return true
    }

    @Deprecated("Use {@link #isHostTrusted(FullHttpRequest, QueryStringDecoder)}", ReplaceWith("true"))
    override fun isHostTrusted(request: FullHttpRequest): Boolean {
        return true
    }
}