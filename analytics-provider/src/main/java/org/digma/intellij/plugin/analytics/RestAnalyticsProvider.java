package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.CharStreams;
import okhttp3.*;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.notifications.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.tests.LatestTestsOfSpanRequest;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.*;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Headers;
import retrofit2.http.*;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RestAnalyticsProvider implements AnalyticsProvider, Closeable {

    private final Client client;

    public RestAnalyticsProvider(String baseUrl) {
        this(baseUrl, null);
    }

    public RestAnalyticsProvider(String baseUrl, String apiToken) {
        this.client = createClient(baseUrl, apiToken);
    }


    public List<String> getEnvironments() {
        var envs = execute(client.analyticsProvider::getEnvironments);
        //make sure environments list is always a mutable list because we change it
        if (envs != null) {
            envs = new ArrayList<>(envs);
        }
        return envs;
    }


    public void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest) {
        execute(() -> client.analyticsProvider.sendDebuggerEvent(debuggerEventRequest));
    }

    @Override
    public List<InsightInfo> getInsightsInfo(InsightsRequest insightsRequest) {
        return execute(() -> client.analyticsProvider.getInsightsInfo(insightsRequest));
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
    public List<CodeObjectError> getErrorsOfCodeObject(String environment, List<String> codeObjectIds) {
        return execute(() -> client.analyticsProvider.getErrorsOfCodeObject(environment, codeObjectIds));
    }

    @Override
    public void setInsightCustomStartTime(CustomStartTimeInsightRequest customStartTimeInsightRequest) {
        execute(() -> client.analyticsProvider.setInsightCustomStartTime(customStartTimeInsightRequest));
    }

    @Override
    public CodeObjectErrorDetails getCodeObjectErrorDetails(String errorSourceId) {
        return execute(() -> client.analyticsProvider.getCodeObjectErrorDetails(errorSourceId));
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
    public AssetNavigationResponse getAssetNavigation(String env, String spanCodeObjectId) {
        return execute(() -> client.analyticsProvider.getAssetNavigation(env, spanCodeObjectId));
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
            String message;
            try (ResponseBody errorBody = response.errorBody()) {
                message = String.format("Error %d. %s", response.code(), errorBody == null ? null : errorBody.string());
            } catch (IOException e) {
                throw new AnalyticsProviderException(e.getMessage(), e);
            }
            throw new AnalyticsProviderException(response.code(), message);
        }
    }


    private Client createClient(String baseUrl, String apiToken) {
        return new Client(baseUrl, apiToken);
    }


    @Override
    public void close() throws IOException {
        client.close();
    }


    //A closable client
    private static class Client implements Closeable {


        private final AnalyticsProviderRetrofit analyticsProvider;
        private final OkHttpClient okHttpClient;

        @SuppressWarnings("MoveFieldAssignmentToInitializer")
        public Client(String baseUrl, String apiToken) {

            //configure okHttp here if necessary
            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            if (baseUrl.startsWith("https:")) {
                // SSL
                applyInsecureSsl(builder);
            }


            if (apiToken != null && !apiToken.isBlank()) {
                builder.addInterceptor(chain -> {
                    Request request = chain.request().newBuilder().addHeader("Authorization", "Token " + apiToken).build();
                    return chain.proceed(request);
                });
            }

            builder.callTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS);

            okHttpClient = builder.build();

            var jacksonFactory = JacksonConverterFactory.create(createObjectMapper());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    //ScalarsConverterFactory must be the first, it supports serializing to plain String, see getAssets
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(jacksonFactory)
                    .validateEagerly(true)
                    .build();

            analyticsProvider = retrofit.create(AnalyticsProviderRetrofit.class);
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
            //objectMapper can be configured here is necessary
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
        @POST("/CodeAnalytics/instrumentation/event")
        Call<Void> sendDebuggerEvent(@Body DebuggerEventRequest debuggerEventRequest);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/environments")
        Call<List<String>> getEnvironments();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/insights")
        Call<List<InsightInfo>> getInsightsInfo(@Body InsightsRequest insightsRequest);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/codeObjects/insight")
        Call<String> getInsightBySpan(@Query("environment") String environment, @Query("spanCodeObjectId") String spanCodeObjectId, @Query("insightType") String insightType);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/events/latest")
        Call<LatestCodeObjectEventsResponse> getLatestEvents(@Body LatestCodeObjectEventsRequest latestCodeObjectEventsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/codeObjects/errors")
        Call<List<CodeObjectError>> getErrorsOfCodeObject(@Query("environment") String environment, @Query("codeObjectId") List<String> codeObjectIds);

        @Headers({
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/codeObjects/errors/{errorSourceId}")
        Call<CodeObjectErrorDetails> getCodeObjectErrorDetails(@Path("errorSourceId") String errorSourceId);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Graphs/graphForSpanPercentiles")
        // @Streaming means ResponseBody as is, without conversion
        @Streaming
        Call<ResponseBody> getHtmlGraphForSpanPercentiles(@Body SpanHistogramQuery request);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Graphs/graphForSpanScaling")
        // @Streaming means ResponseBody as is, without conversion
        @Streaming
        Call<ResponseBody> getHtmlGraphForSpanScaling(@Body SpanHistogramQuery request);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @PUT("/CodeAnalytics/insights/start-time")
        Call<ResponseBody> setInsightCustomStartTime(
                @Body CustomStartTimeInsightRequest customStartTimeInsightRequest
        );

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/recent_activity")
        Call<RecentActivityResult> getRecentActivity(@Body RecentActivityRequest recentActivityRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/user/usage_stats")
        Call<UserUsageStatsResponse> getUserUsageStats(@Body UserUsageStatsRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/LiveData/liveData")
        Call<DurationLiveData> getDurationLiveData(@Body DurationLiveDataRequest durationLiveDataRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/span_navigation")
        Call<CodeObjectNavigation> getCodeObjectNavigation(@Body CodeObjectNavigationRequest codeObjectNavigationRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/assets/get_categories")
        Call<String> getAssetCategories(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/codeObjects/insight_exists")
        Call<String> insightExists(@Query("environment") String environment);


        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/assets/get_assets")
        Call<String> getAssets(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/assets/get_filter")
        Call<String> getAssetFilters(@QueryMap Map<String, Object> fields);

        @GET("/services/getServices")
        Call<String> getServices(@Query("environment") String environment);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Notifications/get-all")
        Call<String> getNotifications(@Body NotificationsRequest notificationsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Notifications/read")
        Call<ResponseBody> setReadNotificationsTime(@Body SetReadNotificationsRequest setReadNotificationsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Notifications/counts")
        Call<UnreadNotificationsCountResponse> getUnreadNotificationsCount(@Body GetUnreadNotificationsCountRequest getUnreadNotificationsCountRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Testing/get_latest_tests_of_span")
        Call<String> getLatestTestsOfSpan(@Body LatestTestsOfSpanRequest request);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/Version/verifyVersion")
        Call<VersionResponse> getVersions(@Body VersionRequest versionRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/about")
        Call<AboutResult> getAbout();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/performanceMetrics")
        Call<PerformanceMetricsResponse> getPerformanceMetrics();

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/performanceMetrics/load-status")
        Call<LoadStatusResponse> getLoadStatus();


        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/delete_environment")
        Call<DeleteEnvironmentResponse> deleteEnvironment(@Body DeleteEnvironmentRequest deleteEnvironmentRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/dashboard/getDashboard")
        Call<String> getDashboard(@QueryMap Map<String, String> fields);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @PUT("/insightsActions/link-ticket")
        Call<LinkUnlinkTicketResponse> linkTicket(@Body LinkTicketRequest linkRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @PUT("/insightsActions/unlink-ticket")
        Call<LinkUnlinkTicketResponse> unlinkTicket(@Body UnlinkTicketRequest linkRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/lens")
        Call<CodeLensOfMethodsResponse> getCodeLensByMethods(@Body CodeLensOfMethodsRequest codeLensOfMethodsRequest);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/code/assets")
        Call<CodeContextSpans> getSpansForCodeLocation(@Query("environment") String env, @Query("codeObjects") List<String> codeObjectIds);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("/assets/display_info")
        Call<AssetDisplayInfo> getAssetDisplayInfo(@Query("environment") String environment, @Query("codeObjectId") String codeObjectId);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/insights/get_insights_view")
        Call<String> getInsights(@QueryMap Map<String, Object> fields);

        @Headers({
                "Accept: text/plain",
                "Content-Type:application/json"
        })
        @GET("/assets/navigation")
        Call<AssetNavigationResponse> getAssetNavigation(@Query("environment") String environment, @Query("spanCodeObjectId") String spanCodeObjectId);
    }

}
