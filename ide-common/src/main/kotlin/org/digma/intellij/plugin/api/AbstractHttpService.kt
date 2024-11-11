package org.digma.intellij.plugin.api

import com.intellij.openapi.diagnostic.Logger
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService

/**
 * base http service that allows all origin hosts
 */
abstract class AbstractHttpService : RestService() {

    protected val logger: Logger = Logger.getInstance(this::class.java)

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