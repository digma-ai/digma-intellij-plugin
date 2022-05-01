package org.digma.intellij.plugin.analytics;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;
import org.digma.intellij.plugin.model.CodeObjectSummary;
import org.digma.intellij.plugin.model.CodeObjectSummaryRequest;
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
        return ((ResteasyClientBuilder) ClientBuilder.newBuilder())
                .connectionPoolSize(20)
                .build();
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
