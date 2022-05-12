package org.digma.intellij.plugin.analytics;

import okhttp3.OkHttpClient;
import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class RestAnalyticsProvider implements AnalyticsProvider, Closeable {

    private final Client client;

    public RestAnalyticsProvider(String baseUrl) {
        this.client = createClient(baseUrl);
    }


    public List<String> getEnvironments() {
        return execute(client.analyticsProvider::getEnvironments);
    }


    public List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest) {
        return execute(() -> client.analyticsProvider.getSummaries(summaryRequest));
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
                throw new AnalyticsProviderException(e);
            }
            throw new AnalyticsProviderException(response.code(), message);
        }
    }


    private Client createClient(String baseUrl) {
        return new Client(baseUrl);
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
        public Client(String baseUrl) {

            //configure okHttp here if necessary
            okHttpClient = new OkHttpClient.Builder()
//                    .eventListener()
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(JacksonConverterFactory.create())
                    .validateEagerly(true)
                    .build();

            analyticsProvider = retrofit.create(AnalyticsProviderRetrofit.class);
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


    //a bit of ugly design. need a retrofit interface that returns Call objects.
    //it's internal to this implementation.
    //plus we have an AnalyticsProvider interface for the plugin code just because an interface is nice to have.
    //both have the same methods with different return type but not necessarily.
    private interface AnalyticsProviderRetrofit {


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


    }

}
