package org.digma.intellij.plugin.analytics;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;
import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class RestAnalyticsProvider implements AnalyticsProvider, Closeable {

    private final String basePath;
    private final ResteasyClient client;

    public RestAnalyticsProvider(String basePath) {
        this.basePath = basePath;
        this.client = createClient();
    }

    public List<String> getEnvironments() {
        return proxy().getEnvironments();
    }


    public List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest) {
        return proxy().getSummaries(summaryRequest);
    }


    private ResteasyClient createClient() {
        ResteasyClient resteasyClient = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .connectionPoolSize(20)
                .build();

        //todo: this is necessary because resteasy can't read service loader when running in a plugin.
        //see: using service loader in plugins: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html
        resteasyClient.register(org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider.class);
        resteasyClient.register(org.jboss.resteasy.plugins.providers.jackson.UnrecognizedPropertyExceptionHandler.class);
        resteasyClient.register(org.jboss.resteasy.plugins.providers.jackson.PatchMethodFilter.class);
        return resteasyClient;
    }


    private AnalyticsProvider proxy() {
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(basePath));
        return target.proxy(AnalyticsProvider.class);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
