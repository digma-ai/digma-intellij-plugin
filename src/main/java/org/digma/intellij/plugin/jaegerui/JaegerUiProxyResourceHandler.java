package org.digma.intellij.plugin.jaegerui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.openapi.diagnostic.Logger;
import com.posthog.java.shaded.okhttp3.*;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.*;
import org.cef.network.*;
import org.digma.intellij.plugin.auth.account.CredentialsHolder;
import org.digma.intellij.plugin.common.JsonUtilsKt;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.ui.jcef.JCefException;
import org.jetbrains.annotations.*;

import java.net.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class JaegerUiProxyResourceHandler implements CefResourceHandler {

    private static final Logger LOGGER = Logger.getInstance(JaegerUiProxyResourceHandler.class);
    private final OkHttpClient okHttpClient;
    private final URL jaegerQueryUrl;
    private Response okHttp3Response;

    public JaegerUiProxyResourceHandler(URL jaegerQueryUrl){
        this.jaegerQueryUrl = jaegerQueryUrl;
        okHttpClient = new OkHttpClient.Builder().build();
    }

    public static boolean isJaegerQueryCall(URL url) {
        return url.getPath().startsWith("/api/");
    }

    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback callback) {
        try {
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
            Log.warnWithException(LOGGER, e, "processRequest failed");
            callback.cancel();
            return false;
        }
    }

    @Nullable
    private static String getAuthFailureReason(Response response){
        final var headerName = "X-Auth-Fail-Reason";
        var reason = response.header(headerName);
        if(reason == null && response.priorResponse() != null)
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
        return new URL(jaegerQueryUrl.getProtocol(), jaegerQueryUrl.getHost(), jaegerQueryUrl.getPort(),
                requestUrl.getPath() + "?" + requestUrl.getQuery());
    }

    @NotNull
    private static HashMap<String, String> getHeaders(CefRequest cefRequest) throws JsonProcessingException {
        var headers = new HashMap<String, String>();
        cefRequest.getHeaderMap(headers);
        var digmaCredentials = CredentialsHolder.INSTANCE.getDigmaCredentials();
        if (digmaCredentials != null){
            headers.put("Cookie", "auth_token_a="+digmaCredentials.getAccessToken());
        }
        return headers;
    }

    @Nullable
    private static RequestBody getBody(CefRequest cefRequest, HashMap<String, String> headers) {
        return cefRequest.getPostData() != null
                ? RequestBody.create(cefRequest.getPostData().toString(), MediaType.parse(headers.get("Content-Type")))
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
        if (body != null){
            cefResponse.setMimeType(body.contentType().toString());
            responseLength.set((int)body.contentLength());
        }
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback cefCallback) {
        try{
            var inputStream = okHttp3Response.body().byteStream();
            var read = inputStream.read(dataOut, 0, bytesToRead);
            if (read == -1) {
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
        okHttp3Response.close();
    }
}
