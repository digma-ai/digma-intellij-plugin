package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.CharStreams;
import okhttp3.*;
import okhttp3.internal.http.HttpMethod;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.activation.DiscoveredDataResponse;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans;
import org.digma.intellij.plugin.model.rest.common.*;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.highlights.HighlightsRequest;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.insights.issues.GetIssuesRequestPayload;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.login.*;
import org.digma.intellij.plugin.model.rest.lowlevel.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.notifications.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.tests.LatestTestsOfSpanRequest;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.*;
import retrofit2.http.Headers;

import javax.annotation.*;
import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class RestAnalyticsProvider implements AnalyticsProvider, Closeable, BaseUrlProvider.UrlChangedListener {

    public static final ThreadLocal<Long> PERFORMANCE = new ThreadLocal<>();
    private final List<AuthenticationProvider> authenticationProviders;
    private final Consumer<String> logger;

    private Client client;
    private final BaseUrlProvider baseUrlProvider;
    private final AtomicBoolean replacingClient = new AtomicBoolean(false);
    private final Object replacingClientLock = new Object();

    //this constructor is used only in tests
    RestAnalyticsProvider(String baseUrl) {

        this(Collections.singletonList(new AuthenticationProvider() {
            @CheckForNull
            @Override
            public String getHeaderName() {
                return null;
            }

            @CheckForNull
            @Override
            public String getHeaderValue() {
                return null;
            }
        }), System.out::println, new BaseUrlProvider() {
            @Nonnull
            @Override
            public String baseUrl() {
                return baseUrl;
            }

            @Override
            public void addUrlChangedListener(UrlChangedListener urlChangedListener, int order) {

            }

            @Override
            public void removeUrlChangedListener(UrlChangedListener urlChangedListener) {

            }
        }, 1);
    }

    public RestAnalyticsProvider(List<AuthenticationProvider> authenticationProviders, Consumer<String> logger, BaseUrlProvider baseUrlProvider, int urlChangeOrder) {
        this.authenticationProviders = authenticationProviders;
        this.logger = logger;
        this.baseUrlProvider = baseUrlProvider;
        this.client = constructClient(authenticationProviders, logger, baseUrlProvider);
        baseUrlProvider.addUrlChangedListener(this, urlChangeOrder);
    }


    private Client constructClient(List<AuthenticationProvider> authenticationProviders, Consumer<String> logger, BaseUrlProvider baseUrlProvider) {
        try {
            return createClient(authenticationProviders, logger, baseUrlProvider);
        } catch (Throwable e) {
            throw new CantConstructClientException(e);
        }
    }


    @Override
    public void urlChanged(BaseUrlProvider.UrlChangedEvent urlChangedEvent) {
        synchronized (replacingClientLock) {
            try {
                replacingClient.set(true);
                client.close();
                client = constructClient(authenticationProviders, logger, baseUrlProvider);
            } finally {
                replacingClient.set(false);
            }
        }
    }


    public String getApiUrl() {
        return baseUrlProvider.baseUrl();
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        return execute(() -> client.analyticsProvider.login(loginRequest));
    }

    @Override
    public LoginResponse refreshToken(RefreshRequest refreshRequest) {
        return execute(() -> client.analyticsProvider.refreshToken(refreshRequest));
    }

    public List<Env> getEnvironments() {
        return execute(client.analyticsProvider::getEnvironments);
    }


    public void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest) {
        execute(() -> client.analyticsProvider.sendDebuggerEvent(debuggerEventRequest));
    }

    @Override
    public List<InsightTypesForJaegerResponse> getInsightsForJaeger(InsightTypesForJaegerRequest request) {
        return execute(() -> client.analyticsProvider.getInsightsForJaeger(request));
    }

    @Override
    public String getInsightBySpan(String environment, String spanCodeObjectId, String insightType) {
        return execute(() -> client.analyticsProvider.getInsightBySpan(environment, spanCodeObjectId, insightType));
    }

    @Override
    public LatestCodeObjectEventsResponse getLatestEvents(LatestCodeObjectEventsRequest latestCodeObjectEventsRequest) {
        return execute(() -> client.analyticsProvider.getLatestEvents(latestCodeObjectEventsRequest));
    }

    @Override
    public String getErrors(String environment, List<String> codeObjectIds) {
        return execute(() -> client.analyticsProvider.getErrors(environment, codeObjectIds));
    }

    @Override
    public String getGlobalErrors(String payload) {
        return execute(() -> client.analyticsProvider.getGlobalErrors(payload));
    }

    @Override
    public String getGlobalErrorsFilters(String payload) {
        return execute(() -> client.analyticsProvider.getGlobalErrorsFilters(payload));
    }

    @Override
    public String getErrorTimeseries(String errorId, Map<String, Object> payload) {
        return execute(() -> client.analyticsProvider.getErrorTimeseries(errorId, payload));
    }

    @Override
    public void pinError(String errorId, String environment) {
        execute(() -> client.analyticsProvider.pinError(errorId, environment));
    }

    @Override
    public void unpinError(String errorId, String environment) {
        execute(() -> client.analyticsProvider.unpinError(errorId, environment));
    }

    @Override
    public void dismissError(String errorId, String environment) {
        execute(() -> client.analyticsProvider.dismissError(errorId, environment));
    }

    @Override
    public void undismissError(String errorId, String environment) {
        execute(() -> client.analyticsProvider.undismissError(errorId, environment));
    }

    @Override
    public void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest) {
        execute(() -> client.analyticsProvider.setInsightCustomStartTime(customStartTimeInsightRequest));
    }

    @Override
    public String getErrorDetails(String errorSourceId) {
        return execute(() -> client.analyticsProvider.getErrorDetails(errorSourceId));
    }

    @Override
    public String getHtmlGraphForSpanPercentiles(SpanHistogramQuery request) {
        final ResponseBody responseBody = execute(() -> client.analyticsProvider.getHtmlGraphForSpanPercentiles(request));
        return readEntire(responseBody);
    }

    @Override
    public String getHtmlGraphForSpanScaling(SpanHistogramQuery request) {
        final ResponseBody responseBody = execute(() -> client.analyticsProvider.getHtmlGraphForSpanScaling(request));
        return readEntire(responseBody);
    }

    @Override
    public RecentActivityResult getRecentActivity(RecentActivityRequest recentActivityRequest) {
        return execute(() -> client.analyticsProvider.getRecentActivity(recentActivityRequest));
    }

    @Override
    public UserUsageStatsResponse getUserUsageStats(UserUsageStatsRequest request) {
        return execute(() -> client.analyticsProvider.getUserUsageStats(request));
    }

    @Override
    public DurationLiveData getDurationLiveData(DurationLiveDataRequest durationLiveDataRequest) {
        return execute(() -> client.analyticsProvider.getDurationLiveData(durationLiveDataRequest));
    }

    @Override
    public CodeObjectNavigation getCodeObjectNavigation(CodeObjectNavigationRequest codeObjectNavigationRequest) {
        return execute(() -> client.analyticsProvider.getCodeObjectNavigation(codeObjectNavigationRequest));
    }

    @Override
    public String getAssetCategories(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getAssetCategories(queryParams));
    }

    @Override
    public String insightExists(String environment) {
        return execute(() -> client.analyticsProvider.insightExists(environment));
    }

    @Override
    public String getAssetFilters(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getAssetFilters(queryParams));
    }

    @Override
    public String getAssets(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getAssets(queryParams));
    }

    @Override
    public String getServices(String environment) {
        return execute(() -> client.analyticsProvider.getServices(environment));
    }

    @Override
    public String getEndpoints(String service, Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getEndpoints(service, queryParams));
    }

    @Override
    public String getEndpointIssues(String queryParams) {
        return execute(() -> client.analyticsProvider.getEndpointIssues(queryParams));
    }


    @Override
    public String getIssuesReportStats(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getIssuesReportStats(queryParams));
    }

    @Override
    public String getServiceReport(String queryParams) {
        return execute(() -> client.analyticsProvider.getServiceReport(queryParams));
    }

    @Override
    public String getEnvironmentsByService(String service) {
        return execute(() -> client.analyticsProvider.getEnvironmentsByService(service));
    }

    @Override
    public String getAssetsReportStats(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getAssetsReportStats(queryParams));
    }

    @Override
    public String getNotifications(NotificationsRequest notificationsRequest) {
        return execute(() -> client.analyticsProvider.getNotifications(notificationsRequest));
    }

    @Override
    public void setReadNotificationsTime(SetReadNotificationsRequest setReadNotificationsRequest) {
        execute(() -> client.analyticsProvider.setReadNotificationsTime(setReadNotificationsRequest));
    }

    @Override
    public UnreadNotificationsCountResponse getUnreadNotificationsCount(GetUnreadNotificationsCountRequest getUnreadNotificationsCountRequest) {
        return execute(() -> client.analyticsProvider.getUnreadNotificationsCount(getUnreadNotificationsCountRequest));
    }

    @Override
    public String getLatestTestsOfSpan(LatestTestsOfSpanRequest request) {
        return execute(() -> client.analyticsProvider.getLatestTestsOfSpan(request));
    }

    @Override
    public VersionResponse getVersions(VersionRequest request) {
        return execute(() -> client.analyticsProvider.getVersions(request));
    }

    @Override
    public AboutResult getAbout() {
        return execute(client.analyticsProvider::getAbout);
    }

    @Override
    public PerformanceMetricsResponse getPerformanceMetrics() {
        return execute(client.analyticsProvider::getPerformanceMetrics);
    }

    @Override
    public Optional<LoadStatusResponse> getLoadStatus() {
        try {
            return Optional.of(execute(client.analyticsProvider::getLoadStatus));
        } catch (AnalyticsProviderException e) {
            if (e.getResponseCode() == 404)
                return Optional.empty();
            throw e;
        }
    }

    @Override
    public DeleteEnvironmentResponse deleteEnvironment(DeleteEnvironmentRequest deleteEnvironmentRequest) {
        return execute(() -> client.analyticsProvider.deleteEnvironment(deleteEnvironmentRequest));
    }

    @Override
    public LinkUnlinkTicketResponse linkTicket(LinkTicketRequest linkRequest) {
        return execute(() -> client.analyticsProvider.linkTicket(linkRequest));
    }

    @Override
    public LinkUnlinkTicketResponse unlinkTicket(UnlinkTicketRequest unlinkRequest) {
        return execute(() -> client.analyticsProvider.unlinkTicket(unlinkRequest));
    }

    @Override
    public CodeLensOfMethodsResponse getCodeLensByMethods(CodeLensOfMethodsRequest codeLensOfMethodsRequest) {
        return execute(() -> client.analyticsProvider.getCodeLensByMethods(codeLensOfMethodsRequest));
    }

    @Override
    public CodeContextSpans getSpansForCodeLocation(String env, List<String> idsWithType) {

        return execute(() -> client.analyticsProvider.getSpansForCodeLocation(env, idsWithType));
    }

    @Override
    public AssetDisplayInfo getAssetDisplayInfo(String env, String codeObjectId) {

        return execute(() -> client.analyticsProvider.getAssetDisplayInfo(env, codeObjectId));
    }

    @Override
    public String getInsights(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getInsights(queryParams));
    }

    @Override
    public String getIssues(GetIssuesRequestPayload queryParams) {
        return execute(() -> client.analyticsProvider.getIssues(queryParams));

    }

    @Override
    public String getIssuesFilters(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getIssuesFilters(queryParams));
    }

    @Override
    public AssetNavigationResponse getAssetNavigation(String env, String spanCodeObjectId) {
        return execute(() -> client.analyticsProvider.getAssetNavigation(env, spanCodeObjectId));
    }

    @Override
    public String createEnvironments(Map<String, Object> request) {
        return execute(() -> client.analyticsProvider.createEnvironment(request));
    }

    @Override
    public String register(Map<String, Object> request) {
        return execute(() -> client.analyticsProvider.register(request));
    }

    @Override
    public void deleteEnvironmentV2(String id) {
        execute(() -> client.analyticsProvider.deleteEnvironment(id));
    }

    @Override
    public void markInsightsAsRead(List<String> insightIds) {
        execute(() -> client.analyticsProvider.markInsightsAsRead(new MarkInsightsAsReadRequest(insightIds)));
    }

    @Override
    public void markAllInsightsAsRead(String environment, MarkInsightsAsReadScope scope) {
        execute(() -> client.analyticsProvider.markAllInsightsAsRead(
                new MarkAllInsightsAsReadRequest(environment, scope)));
    }

    @Override
    public void dismissInsight(String insightId) {
        execute(() -> client.analyticsProvider.dismissInsight(new DismissRequest(insightId)));
    }

    @Override
    public void undismissInsight(String insightId) {
        execute(() -> client.analyticsProvider.undismissInsight(new UnDismissRequest(insightId)));
    }

    @Override
    public InsightsStatsResult getInsightsStats(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getInsightsStats(queryParams));
    }

    @Override
    public String getHighlightsPerformance(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getHighlightsPerformance(queryParams));
    }

    @Override
    public String getHighlightsTopInsights(Map<String, Object> queryParams) {
        return execute(() -> client.analyticsProvider.getHighlightsTopInsights(queryParams));
    }

    @Override
    public String getHighlightsImpact(HighlightsRequest request) {
        return execute(() -> client.analyticsProvider.getHighlightsImpact(request));
    }

    @Override
    public String getHighlightsPerformanceV2(HighlightsRequest request) {
        return execute(() -> client.analyticsProvider.getHighlightsPerformanceV2(request));
    }

    @Override
    public String getHighlightsTopInsightsV2(HighlightsRequest request) {
        return execute(() -> client.analyticsProvider.getHighlightsTopInsightsV2(request));
    }

    @Override
    public String getHighlightsScaling(HighlightsRequest request) {
        return execute(() -> client.analyticsProvider.getHighlightsScaling(request));
    }

    @Override
    public String getSpanInfo(String spanCodeObjectId) {
        return execute(() -> client.analyticsProvider.getSpanInfo(spanCodeObjectId));
    }

    @Override
    public List<SpanEnvironment> getSpanEnvironmentsStats(String spanCodeObjectId) {
        return execute(() -> client.analyticsProvider.getSpanEnvironmentsStats(spanCodeObjectId));
    }

    @Override
    public void resetThrottlingStatus() {
        execute(client.analyticsProvider::resetThrottlingStatus);
    }

    @Override
    public DiscoveredDataResponse getDiscoveredData() {
        return execute(client.analyticsProvider::getDiscoveredData);
    }

    @Override
    public SpanInfoByUid resolveSpanByUid(String uid) {
        return execute(() -> client.analyticsProvider.resolveSpanByUid(uid));
    }

    @Override
    public EnvironmentInfoByErrorId resolveEnvironmentByErrorId(String errorId) {
        return execute(() -> client.analyticsProvider.resolveEnvironmentByErrorId(errorId));
    }

    @Override
    public HttpResponse proxyCall(HttpRequest request) {

        try {
            var okHttp3Request = toOkHttp3Request(request);
            okhttp3.Response okHttp3Response = client.okHttpClient.newCall(okHttp3Request).execute();
            //handle only authentication error so the plugin will login or redirect the ui to login.
            //any other error should be propagated to the UI
            if (okHttp3Response.code() == HTTPConstants.UNAUTHORIZED) {
                try (ResponseBody errorBody = okHttp3Response.body()) {
                    throw createUnsuccessfulResponseException(okHttp3Response.code(), errorBody);
                }
            }

            return toHttpResponse(okHttp3Response);

        } catch (Exception e) {
            throw new AnalyticsProviderException("Error in proxy call: " + request, e);
        }
    }

    private Request toOkHttp3Request(HttpRequest request) {
        var okHttp3RequestBuilder = new Request.Builder()
                .url(request.getUrl());
        if("get".equalsIgnoreCase(request.getMethod())){
            okHttp3RequestBuilder = okHttp3RequestBuilder.get();
        } else if ("head".equalsIgnoreCase(request.getMethod())) {
            okHttp3RequestBuilder = okHttp3RequestBuilder.head();
        }else{
            RequestBody okHttp3Body = null;
            if (request.getBody() != null) {
                var contentType = request.getHeader("Content-Type");
                if (contentType == null) {
                    okHttp3Body = RequestBody.create(null, request.getBody().getContent());
                } else {
                    okHttp3Body = RequestBody.create(MediaType.parse(contentType), request.getBody().getContent());
                }
            }else if(HttpMethod.requiresRequestBody(request.getMethod())){
                //okhttp forces a body even for empty requests, the check above assumes the method is in uppercase
                okHttp3Body = RequestBody.create(null, new byte[0]);
            }
            okHttp3RequestBuilder = okHttp3RequestBuilder.method(request.getMethod(), okHttp3Body);
        }
        request.getHeaders().forEach(okHttp3RequestBuilder::header);
        return okHttp3RequestBuilder.build();
    }

    private HttpResponse toHttpResponse(okhttp3.Response response) {
        var body = response.body();
        var headers = response.headers().toMultimap().entrySet().stream()
                .filter(i -> !i.getValue().isEmpty())
                .collect(HashMap<String, String>::new, (m, i) -> m.put(i.getKey(), i.getValue().get(0)), Map::putAll);

        var contentLength = body != null ? body.contentLength() : null;
        var contentTypeHeaderName = response.headers().names().stream().filter(s -> s != null && s.equalsIgnoreCase("content-type")).findFirst().orElse(null);
        var contentTypeHeader = contentTypeHeaderName != null ? response.header(contentTypeHeaderName) : null;
        MediaType mediaType = (body != null && body.contentType() != null) ? body.contentType() :
                (contentTypeHeader != null) ? MediaType.parse(contentTypeHeader) : null;
        var contentType = mediaType != null ? mediaType.toString() : null;
        var contentStream = body != null ? body.byteStream() : null;

        return new HttpResponse(
                response.code(),
                headers,
                contentLength,
                contentType,
                contentStream);
    }

    @Override
    public String getDashboard(Map<String, String> queryParams) {
        return execute(() -> client.analyticsProvider.getDashboard(queryParams));
    }

    protected static String readEntire(ResponseBody responseBody) {
        try (Reader reader = responseBody.charStream()) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read HTTP Response Body", e);
        }
    }

    public <T> T execute(Supplier<Call<T>> supplier) {

        //don't want to use locks, just throw calling threads if currently replacing the client as a result of url changed.
        //caller should always catch and handle AnalyticsProviderException
        if (replacingClient.get()) {
            throw new ReplacingClientException("can't serve requests, currently replacing clients");
        }

        Response<T> response;
        try {
            Call<T> call = supplier.get();
            response = call.execute();
        } catch (Exception e) {
            throw new AnalyticsProviderException(e);
        }

        if (response.isSuccessful()) {
            return response.body();
        } else {
            try (ResponseBody errorBody = response.errorBody()) {
                throw createUnsuccessfulResponseException(response.code(), errorBody);
            } catch (IOException e) {
                throw new AnalyticsProviderException(e.getMessage(), e);
            }
        }
    }

    private AnalyticsProviderException createUnsuccessfulResponseException(int code, ResponseBody errorBody) throws IOException {
        var errorMessage = errorBody == null ? null : errorBody.string();
        if (code == HTTPConstants.UNAUTHORIZED) {
            var message = errorMessage != null && !errorMessage.isEmpty() ? errorMessage : "Unauthorized " + code;
            return new AuthenticationException(code, message);
        }

        String message = String.format("Error %d. %s", code, errorMessage);
        return new AnalyticsProviderException(code, message);
    }


    private Client createClient(List<AuthenticationProvider> authenticationProviders, Consumer<String> logger, BaseUrlProvider baseUrlProvider) {
        return new Client(authenticationProviders, logger, baseUrlProvider);
    }


    @Override
    public void close() throws IOException {
        client.close();
        baseUrlProvider.removeUrlChangedListener(this);
    }


    //A closable client
    private static class Client implements Closeable {


        private final AnalyticsProviderRetrofit analyticsProvider;
        private final OkHttpClient okHttpClient;

        @SuppressWarnings("MoveFieldAssignmentToInitializer")
        public Client(List<AuthenticationProvider> authenticationProviders, Consumer<String> logger, BaseUrlProvider baseUrlProvider) {

            //configure okHttp here if necessary
            OkHttpClient.Builder builder = new OkHttpClient.Builder();


            //if (baseUrlProvider.baseUrl().startsWith("https:")) {
            // SSL
            //we can always applyInsecureSsl even if the schema is http
            applyInsecureSsl(builder);
            //}


            authenticationProviders.forEach(authenticationProvider -> builder.addInterceptor(chain -> {
                var headerName = authenticationProvider.getHeaderName();
                var headerValue = authenticationProvider.getHeaderValue();
                if (headerName != null && headerValue != null) {
                    Request request = chain.request().newBuilder().addHeader(headerName, headerValue).build();
                    return chain.proceed(request);
                } else {
                    return chain.proceed(chain.request());
                }
            }));


            addPerformanceInterceptor(builder);

            //always add the logging interceptor last, so it will log info from all other interceptors
            addLoggingInterceptor(builder, logger);

            builder.callTimeout(20, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS);

            okHttpClient = builder.build();

            var jacksonFactory = JacksonConverterFactory.create(createObjectMapper());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(ensureEndsWithSlash(baseUrlProvider.baseUrl()))
                    .client(okHttpClient)
                    //ScalarsConverterFactory must be the first, it supports serializing to plain String, see getAssets
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(jacksonFactory)
                    .validateEagerly(true)
                    .build();

            analyticsProvider = retrofit.create(AnalyticsProviderRetrofit.class);
        }


        private String ensureEndsWithSlash(String baseUrl) {
            //if url contains path it must end with slash.
            //retrofit will check that in retrofit2.Retrofit.Builder.baseUrl(okhttp3.HttpUrl)
            var url = HttpUrl.get(baseUrl);
            List<String> pathSegments = url.pathSegments();
            if (!pathSegments.isEmpty() && !"".equals(pathSegments.get(pathSegments.size() - 1))) {
                return baseUrl + "/";
            }
            return baseUrl;
        }


        private void addPerformanceInterceptor(OkHttpClient.Builder builder) {
            builder.addInterceptor(chain -> {
                PERFORMANCE.remove();
                var stopWatch = StopWatch.createStarted();
                try {
                    return chain.proceed(chain.request());
                } finally {
                    stopWatch.stop();
                    var time = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    PERFORMANCE.set(time);
                }
            });
        }


        private void addLoggingInterceptor(OkHttpClient.Builder builder, Consumer<String> logger) {

            var logSensitive = Boolean.getBoolean("api.digma.org.log.sensitive");

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                if (!logSensitive && message != null && message.contains("accessToken")) {
                    return;
                }
                logger.accept(message);
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            if (!logSensitive) {
                logging.redactHeader("Authorization");
                logging.redactHeader("Digma-Access-Token");
            }
            builder.addInterceptor(logging);
        }


        private void applyInsecureSsl(OkHttpClient.Builder builder) {
            SSLContext sslContext;
            X509TrustManager insecureTrustManager = new InsecureTrustManager();
            try {
                sslContext = SSLContext.getInstance("SSL");
            } catch (NoSuchAlgorithmException e) {
                throw new AnalyticsProviderException(e);
            }
            try {
                sslContext.init(null, new TrustManager[]{insecureTrustManager}, new SecureRandom());
            } catch (KeyManagementException e) {
                throw new AnalyticsProviderException(e);
            }
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(socketFactory, insecureTrustManager);
            builder.hostnameVerifier(new InsecureHostnameVerifier());
        }

        private ObjectMapper createObjectMapper() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
            objectMapper.registerModule(new JavaTimeModule());
            //objectMapper can be configured here if necessary
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
            objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
            return objectMapper;
        }


        @Override
        public void close() {
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
            //cache will be closed by try with resources
            try (Cache cache = okHttpClient.cache()) {
                if (cache != null) {
                    cache.evictAll();
                }
            } catch (Exception e) {
                //ignore here, is closing
            }
        }
    }


    static class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
            // accept all
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
            // accept all
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    static class InsecureHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            // allow all hostnames
            return true;
        }
    }

    //a bit of ugly design. need a retrofit interface that returns Call objects.
    //it's internal to this implementation.
    //plus we have an AnalyticsProvider interface for the plugin code just because an interface is nice to have.
    //both have the same methods with different return type.
    private interface AnalyticsProviderRetrofit {

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/instrumentation/event")
        Call<Void> sendDebuggerEvent(@Body DebuggerEventRequest debuggerEventRequest);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("environments")
        Call<List<Env>> getEnvironments();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("insights/typesForJaeger")
        Call<List<InsightTypesForJaegerResponse>> getInsightsForJaeger(@Body InsightTypesForJaegerRequest request);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/codeObjects/insight")
        Call<String> getInsightBySpan(@Query("environment") String environment, @Query("spanCodeObjectId") String spanCodeObjectId, @Query("insightType") String insightType);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/events/latest")
        Call<LatestCodeObjectEventsResponse> getLatestEvents(@Body LatestCodeObjectEventsRequest latestCodeObjectEventsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/codeObjects/errors")
        Call<String> getErrors(@Query("environment") String environment, @Query("codeObjectId") List<String> codeObjectIds);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors")
        Call<String> getGlobalErrors(@Body String payload);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("errors/{errorId}/timeseries")
        Call<String> getErrorTimeseries(@Path("errorId") String errorSourceId, @QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors/{errorId}/pin")
        Call<Void> pinError(@Path("errorId") String errorSourceId, @Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors/{errorId}/unpin")
        Call<Void> unpinError(@Path("errorId") String errorSourceId, @Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors/{errorId}/dismiss")
        Call<Void> dismissError(@Path("errorId") String errorSourceId, @Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors/{errorId}/undismiss")
        Call<Void> undismissError(@Path("errorId") String errorSourceId, @Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("errors/filters")
        Call<String> getGlobalErrorsFilters(@Body String payload);

        @Headers({
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/codeObjects/errors/{errorSourceId}")
        Call<String> getErrorDetails(@Path("errorSourceId") String errorSourceId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Graphs/graphForSpanPercentiles")
        // @Streaming means ResponseBody as is, without conversion
        @Streaming
        Call<ResponseBody> getHtmlGraphForSpanPercentiles(@Body SpanHistogramQuery request);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Graphs/graphForSpanScaling")
        // @Streaming means ResponseBody as is, without conversion
        @Streaming
        Call<ResponseBody> getHtmlGraphForSpanScaling(@Body SpanHistogramQuery request);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @PUT("CodeAnalytics/insights/start-time")
        Call<Void> setInsightCustomStartTime(
                @Body CustomStartTimeInsightRequest customStartTimeInsightRequest
        );

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/codeObjects/recent_activity")
        Call<RecentActivityResult> getRecentActivity(@Body RecentActivityRequest recentActivityRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/user/usage_stats")
        Call<UserUsageStatsResponse> getUserUsageStats(@Body UserUsageStatsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("LiveData/liveData")
        Call<DurationLiveData> getDurationLiveData(@Body DurationLiveDataRequest durationLiveDataRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/codeObjects/span_navigation")
        Call<CodeObjectNavigation> getCodeObjectNavigation(@Body CodeObjectNavigationRequest codeObjectNavigationRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("assets/get_categories")
        Call<String> getAssetCategories(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/codeObjects/insight_exists")
        Call<String> insightExists(@Query("environment") String environment);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("assets/get_assets")
        Call<String> getAssets(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("assets/get_filter")
        Call<String> getAssetFilters(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("reports/issues/statistics")
        Call<String> getIssuesReportStats(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("reports/services/issues")
        Call<String> getServiceReport(@Body String filters);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("services/{service}/environments")
        Call<String> getEnvironmentsByService(@Path("service") String service);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("reports/assets/statistics")
        Call<String> getAssetsReportStats(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("services/{service}/endpoints")
        Call<String> getEndpoints(@Path("service") String service, @QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("reports/endpoints/issues")
        Call<String> getEndpointIssues(@Body String filters);

        @GET("services/getServices")
        Call<String> getServices(@Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Notifications/get-all")
        Call<String> getNotifications(@Body NotificationsRequest notificationsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Notifications/read")
        Call<Void> setReadNotificationsTime(@Body SetReadNotificationsRequest setReadNotificationsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Notifications/counts")
        Call<UnreadNotificationsCountResponse> getUnreadNotificationsCount(@Body GetUnreadNotificationsCountRequest getUnreadNotificationsCountRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Testing/get_latest_tests_of_span")
        Call<String> getLatestTestsOfSpan(@Body LatestTestsOfSpanRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Version/verifyVersion")
        Call<VersionResponse> getVersions(@Body VersionRequest versionRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("about")
        Call<AboutResult> getAbout();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("performanceMetrics")
        Call<PerformanceMetricsResponse> getPerformanceMetrics();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("performanceMetrics/load-status")
        Call<LoadStatusResponse> getLoadStatus();


        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/delete_environment")
        Call<DeleteEnvironmentResponse> deleteEnvironment(@Body DeleteEnvironmentRequest deleteEnvironmentRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("dashboard/getDashboard")
        Call<String> getDashboard(@QueryMap Map<String, String> fields);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @PUT("insightsActions/link-ticket")
        Call<LinkUnlinkTicketResponse> linkTicket(@Body LinkTicketRequest linkRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @PUT("insightsActions/unlink-ticket")
        Call<LinkUnlinkTicketResponse> unlinkTicket(@Body UnlinkTicketRequest linkRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @POST("CodeAnalytics/codeObjects/lens")
        Call<CodeLensOfMethodsResponse> getCodeLensByMethods(@Body CodeLensOfMethodsRequest codeLensOfMethodsRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/code/assets")
        Call<CodeContextSpans> getSpansForCodeLocation(@Query("environment") String env, @Query("codeObjects") List<String> codeObjectIds);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("assets/display_info")
        Call<AssetDisplayInfo> getAssetDisplayInfo(@Query("environment") String environment, @Query("codeObjectId") String codeObjectId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("insights/get_insights_view")
        Call<String> getInsights(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("insights/issues")
        Call<String> getIssues(@Body GetIssuesRequestPayload fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("insights/issues/filters")
        Call<String> getIssuesFilters(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("assets/navigation")
        Call<AssetNavigationResponse> getAssetNavigation(@Query("environment") String environment, @Query("spanCodeObjectId") String spanCodeObjectId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Insights/markRead")
        Call<Void> markInsightsAsRead(@Body MarkInsightsAsReadRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("Insights/markAllRead")
        Call<Void> markAllInsightsAsRead(@Body MarkAllInsightsAsReadRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @PUT("InsightsActions/dismiss")
        Call<Void> dismissInsight(@Body DismissRequest insightId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @PUT("InsightsActions/unDismiss")
        Call<Void> undismissInsight(@Body UnDismissRequest insightId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("insights/statistics")
        Call<InsightsStatsResult> getInsightsStats(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("highlights/performance")
        Call<String> getHighlightsPerformance(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("highlights/topinsights")
        Call<String> getHighlightsTopInsights(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("highlights/performance")
        Call<String> getHighlightsPerformanceV2(@Body HighlightsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("highlights/topinsights")
        Call<String> getHighlightsTopInsightsV2(@Body HighlightsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("highlights/impact")
        Call<String> getHighlightsImpact(@Body HighlightsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("authentication/login")
        Call<LoginResponse> login(@Body LoginRequest loginRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("authentication/refresh-token")
        Call<LoginResponse> refreshToken(@Body RefreshRequest loginRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("environments")
        Call<String> createEnvironment(@Body Map<String, Object> request);

        @DELETE("environments/{envId}")
        Call<Void> deleteEnvironment(@Path("envId") String envId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("authentication/register")
        Call<String> register(@Body Map<String, Object> request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })

        @POST("highlights/scaling")
        Call<String> getHighlightsScaling(@Body HighlightsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })

        @GET("spans/info")
        Call<String> getSpanInfo(@Query("SpanCodeObjectId") String spanCodeObjectId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("PerformanceMetrics/reset-throttling")
        Call<Void> resetThrottlingStatus();

        @GET("spans/environments")
        Call<List<SpanEnvironment>> getSpanEnvironmentsStats(@Query("SpanCodeObjectId") String spanCodeObjectId);

        @Headers({
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/discovered-data")
        Call<DiscoveredDataResponse> getDiscoveredData();

        @Headers({
                "Content-Type:application/json"
        })
        @GET("spans/spanCodeObjectId/{uid}")
        Call<SpanInfoByUid> resolveSpanByUid(@Path("uid") String uid);

        @Headers({
                "Content-Type:application/json"
        })
        @GET("CodeAnalytics/codeObjects/error_environment")
        Call<EnvironmentInfoByErrorId> resolveEnvironmentByErrorId(@Query("errorId") String errorId);

    }
}
