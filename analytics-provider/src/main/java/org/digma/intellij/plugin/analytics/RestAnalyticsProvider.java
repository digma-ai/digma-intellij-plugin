package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import javax.net.ssl.*;
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


    public void sendDebuggerEvent(DebuggerEventRequest debuggerEventRequest){
        execute(() -> client.analyticsProvider.sendDebuggerEvent(debuggerEventRequest));
    }

    public List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest) {
        return execute(() -> client.analyticsProvider.getSummaries(summaryRequest));
    }

    @Override
    public List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest) {
        return execute(() -> client.analyticsProvider.getInsights(insightsRequest));
    }

    @Override
    public List<GlobalInsight> getGlobalInsights(InsightsRequest insightsRequest) {
        return execute(() -> client.analyticsProvider.getGlobalInsights(insightsRequest));
    }

    @Override
    public List<CodeObjectError> getErrorsOfCodeObject(String environment, String codeObjectId) {
        return execute(() -> client.analyticsProvider.getErrorsOfCodeObject(environment, codeObjectId));
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

            builder.callTimeout(5, TimeUnit.SECONDS)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS);

            okHttpClient = builder.build();

            var jacksonFactory = JacksonConverterFactory.create(createObjectMapper());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
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
        @POST("/CodeAnalytics/summary")
        Call<List<CodeObjectSummary>> getSummaries(@Body CodeObjectSummaryRequest summaryRequest);


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
        @POST("/CodeAnalytics/insights")
        Call<List<GlobalInsight>> getGlobalInsights(@Body InsightsRequest insightsRequest);

        @Headers({
                "Accept: application/+json",
                "Content-Type:application/json"
        })
        @GET("/CodeAnalytics/codeObjects/errors")
        Call<List<CodeObjectError>> getErrorsOfCodeObject(@Query("environment") String environment, @Query("codeObjectId") String codeObjectId);

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

    }

}
