package org.digma.intellij.plugin.analytics;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.digma.intellij.plugin.model.CodeObjectSummary;
import org.digma.intellij.plugin.model.CodeObjectSummaryRequest;

import java.io.Closeable;
import java.util.List;

public interface AnalyticsProvider extends Closeable {

    @GET
    @Path("/CodeAnalytics/environments")
    @Produces({MediaType.APPLICATION_JSON})
    List<String> getEnvironments();


    @POST
    @Path("/CodeAnalytics/summary")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest);


}
