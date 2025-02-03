package org.digma.intellij.plugin.ui.jcef;

import com.intellij.openapi.diagnostic.Logger;
import com.posthog.java.shaded.okhttp3.*;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.*;
import org.cef.network.*;
import org.digma.intellij.plugin.auth.account.CredentialsHolder;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.settings.SettingsState;
import org.jetbrains.annotations.*;

import java.net.*;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.ui.jcef.ProxyUtilsKt.postDataToByteArray;

public class JaegerProxyResourceHandler implements CefResourceHandler {

    //the jaeger backend api starts with /api , the proxy just keeps it as is.
    //if necessary removes /jaeger prefix from the path.

    public static final String JAEGER_API_PATH_PREFIX = "/jaeger";
    public static final String JAEGER_API_PATH_TO_PROXY = JAEGER_API_PATH_PREFIX + "/api";
    public static final String JAEGER_API_PATH_TO_PROXY_ONLY_FROM_JAEGER_UI_APP = "/api/";


    private static final Logger LOGGER = Logger.getInstance(JaegerProxyResourceHandler.class);
    private final OkHttpClient okHttpClient;
    private final URL jaegerQueryUrl;
    private Response okHttp3Response;

    public JaegerProxyResourceHandler(URL jaegerQueryUrl) {
        this.jaegerQueryUrl = jaegerQueryUrl;
        okHttpClient = new OkHttpClient.Builder().build();
    }

    //this method is called from JaegerUiSchemeHandlerFactory only. it is to support
    // jaeger ui that still sends requests to /api/ instead of /jaeger/api/
    public static boolean isJaegerQueryCallFromJaegerUI(URL url) {
        return url.getPath().startsWith(JAEGER_API_PATH_TO_PROXY_ONLY_FROM_JAEGER_UI_APP);
    }

    //this method is called from BaseSchemeHandlerFactory only.
    //it supports all jcef apps.
    public static boolean isJaegerQueryCall(URL url) {
        return url.getPath().startsWith(JAEGER_API_PATH_TO_PROXY);
    }


    public static URL getJaegerQueryUrlOrNull() {
        var urlStr = SettingsState.getInstance().getJaegerQueryUrl();
        if (urlStr == null)
            return null;

        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            Log.warnWithException(LOGGER, e, "JaegerQueryUrl parsing failed");
            ErrorReporter.getInstance().reportError("JaegerProxyResourceHandler.getJaegerQueryUrlOrNull", e);
        }
        return null;
    }


    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback callback) {
        try {

            Log.log(LOGGER::trace, "processing request {}, [request id:{}]", cefRequest.getURL(), cefRequest.getIdentifier());

            var apiUrl = getApiUrl(cefRequest);
            var headers = getHeaders(cefRequest);
            var body = getBody(cefRequest, headers);
            var okHttp3Request = new Request.Builder()
                    .method(cefRequest.getMethod(), body)
                    .headers(Headers.of(headers))
                    .url(apiUrl)
                    .build();
            okHttp3Response = okHttpClient.newCall(okHttp3Request).execute();
            var authFailureReason = getAuthFailureReason(okHttp3Response);
            if (authFailureReason != null && !authFailureReason.isBlank()) {
                okHttp3Response = buildAuthFailureResponse(okHttp3Request, okHttp3Response, authFailureReason);
            }
            callback.Continue();
            return true;
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "processRequest failed for request {}, [request id:{}]", cefRequest.getURL(),cefRequest.getIdentifier());
            ErrorReporter.getInstance().reportError("JaegerProxyResourceHandler.processRequest", e);
            callback.cancel();
            return false;
        }
    }

    @Nullable
    private static String getAuthFailureReason(Response response) {
        final var headerName = "X-Auth-Fail-Reason";
        var reason = response.header(headerName);
        if (reason == null && response.priorResponse() != null)
            reason = response.priorResponse().header(headerName);
        return reason;
    }

    private static Response buildAuthFailureResponse(Request okHttp3Request, Response response, String authFailureReason) {
        return new Response(
                okHttp3Request,
                response.protocol(),
                "Authentication failed", 401,
                response.handshake(),
                Headers.of(),
                ResponseBody.create(
                        "Authentication failed: " + authFailureReason,
                        MediaType.parse("text/html")
                ),
                null, null, null,
                0, 0, null);
    }

    @NotNull
    private URL getApiUrl(CefRequest cefRequest) throws MalformedURLException {
        var requestUrl = new URL(cefRequest.getURL());
        var path = requestUrl.getPath() == null ? "" : requestUrl.getPath();
        if (isJaegerQueryCall(requestUrl)) {
            //remove the /jaeger prefix and keep /api
            path = path.replaceFirst(JAEGER_API_PATH_PREFIX, "");
        }
        var query = (requestUrl.getQuery() == null || requestUrl.getQuery().isBlank()) ? "" : "?" + requestUrl.getQuery();
        return new URL(jaegerQueryUrl.getProtocol(), jaegerQueryUrl.getHost(), jaegerQueryUrl.getPort(),
                path + query);
    }

    @NotNull
    private static HashMap<String, String> getHeaders(CefRequest cefRequest) {
        var headers = new HashMap<String, String>();
        cefRequest.getHeaderMap(headers);
        var digmaCredentials = CredentialsHolder.INSTANCE.getDigmaCredentials();
        if (digmaCredentials != null) {
            headers.put("Cookie", "auth_token_a=" + digmaCredentials.getAccessToken());
        }
        return headers;
    }

    @Nullable
    private static RequestBody getBody(CefRequest cefRequest, HashMap<String, String> headers) {
        return cefRequest.getPostData() != null
                ? RequestBody.create(postDataToByteArray(cefRequest, cefRequest.getPostData()), MediaType.parse(headers.get("Content-Type")))
                : null;
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl) {
        if (okHttp3Response == null)
            return;

        cefResponse.setStatus(okHttp3Response.code());
        var headersMap = okHttp3Response.headers().names().stream().collect(Collectors.toMap(s -> s, okHttp3Response::header));
        cefResponse.setHeaderMap(headersMap);

        var body = okHttp3Response.body();
        if (body != null) {
            cefResponse.setMimeType(body.contentType().toString());
            responseLength.set((int) body.contentLength());
        }
    }


    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback cefCallback) {
        try {
            var inputStream = okHttp3Response.body().byteStream();
            var read = inputStream.read(dataOut, 0, bytesToRead);
            if (read <= 0) {
                bytesRead.set(0);
                inputStream.close();
                return false;
            }
            bytesRead.set(read);
            return true;
        } catch (Exception e) {
            Log.warnWithException(LOGGER, e, "exception readResponse");
            throw new JCefException(e);
        }
    }

    @Override
    public void cancel() {
        if (okHttp3Response != null) {
            okHttp3Response.close();
        }
    }
}
