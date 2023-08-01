package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetsRequest;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.CustomStartTimeInsightRequest;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveDataRequest;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigationRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse;
import org.digma.intellij.plugin.model.rest.version.VersionRequest;
import org.digma.intellij.plugin.model.rest.version.VersionResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
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
        return execute(client.analyticsProvider::getEnvironments);
    }


    public void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest) {
        execute(() -> client.analyticsProvider.sendDebuggerEvent(debuggerEventRequest));
    }

    @Override
    public List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest) {
        return execute(() -> client.analyticsProvider.getInsights(insightsRequest));
    }

    @Override
    public InsightsOfMethodsResponse getInsightsOfMethods(InsightsOfMethodsRequest insightsOfMethodsRequest) {
        return execute(() -> client.analyticsProvider.getInsightsOfMethods(insightsOfMethodsRequest));
    }

    public InsightsOfSingleSpanResponse getInsightsForSingleSpan(InsightsOfSingleSpanRequest insightsOfSingleSpanRequest){
        return execute(() -> client.analyticsProvider.getInsightsForSingleSpan(insightsOfSingleSpanRequest));
    }

    @Override
    public List<GlobalInsight> getGlobalInsights(InsightsRequest insightsRequest) {
        return execute(() -> client.analyticsProvider.getGlobalInsights(insightsRequest));
    }

    @Override
    public List<CodeObjectError> getErrorsOfCodeObject(String environment, List<String> codeObjectIds) {
        return execute(() -> client.analyticsProvider.getErrorsOfCodeObject(environment, codeObjectIds));
    }

    @Override
    public CodeObjectInsightsStatusResponse getCodeObjectInsightStatus(InsightsOfMethodsRequest request) {
        return execute(() -> client.analyticsProvider.getCodeObjectInsightStatus(request));
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
    public UsageStatusResult getUsageStatus(UsageStatusRequest usageStatusRequest) {
        return execute(() -> client.analyticsProvider.getUsageStatus(usageStatusRequest));
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
    public DurationLiveData getDurationLiveData(DurationLiveDataRequest durationLiveDataRequest) {
        return execute(() -> client.analyticsProvider.getDurationLiveData(durationLiveDataRequest));
    }

    @Override
    public CodeObjectNavigation getCodeObjectNavigation(CodeObjectNavigationRequest codeObjectNavigationRequest) {
        return execute(() -> client.analyticsProvider.getCodeObjectNavigation(codeObjectNavigationRequest));
    }

    @Override
    public String getAssets(AssetsRequest assetsRequest) {
        return execute(() -> client.analyticsProvider.getAssets(assetsRequest));
    }

    @Override
    public VersionResponse getVersions(VersionRequest request) {
        return execute(() -> client.analyticsProvider.getVersions(request));
    }

    @Override
    public AboutResult getAbout() {
        return execute(() -> client.analyticsProvider.getAbout());
    }

    @Override
    public PerformanceMetricsResponse getPerformanceMetrics()  {
        return execute(client.analyticsProvider::getPerformanceMetrics);
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
            try {
                message = String.format("Error %d. %s", response.code(), response.errorBody() == null ? null : response.errorBody().string());
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
                throw new RuntimeException(e);
            }
            try {
                sslContext.init(null, new TrustManager[]{insecureTrustManager}, new SecureRandom());
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            }
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(socketFactory, insecureTrustManager);
            builder.hostnameVerifier(new InsecureHostnameVerifier());
        }

        private ObjectMapper createObjectMapper() {
            ObjectMapper objectMapper = new ObjectMapper();
            //objectMapper can be configured here is necessary
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
            return objectMapper;
        }


        @Override
        public void close() throws IOException {
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
            if (null != okHttpClient.cache()) {
                Objects.requireNonNull(okHttpClient.cache()).close();
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
        Call<List<CodeObjectInsight>> getInsights(@Body InsightsRequest insightsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/insights_of_methods")
        Call<InsightsOfMethodsResponse> getInsightsOfMethods(@Body InsightsOfMethodsRequest insightsOfMethodsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/insights_of_single")
        Call<InsightsOfSingleSpanResponse> getInsightsForSingleSpan(@Body InsightsOfSingleSpanRequest insightsOfSingleSpanRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/insights")
        Call<List<GlobalInsight>> getGlobalInsights(@Body InsightsRequest insightsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @POST("/CodeAnalytics/codeObjects/insight_status")
        Call<CodeObjectInsightsStatusResponse> getCodeObjectInsightStatus(@Body InsightsOfMethodsRequest insightsRequest);

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
        @POST("/CodeAnalytics/codeobjects/status")
        Call<UsageStatusResult> getUsageStatus(@Body UsageStatusRequest usageStatusRequest);


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
        @POST("/CodeAnalytics/codeObjects/assets")
        Call<String> getAssets(@Body AssetsRequest assetsRequest);

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
    }

}
