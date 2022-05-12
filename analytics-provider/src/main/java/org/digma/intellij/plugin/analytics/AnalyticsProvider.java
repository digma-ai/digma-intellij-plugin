package org.digma.intellij.plugin.analytics;

import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;

import java.io.Closeable;
import java.util.List;

public interface AnalyticsProvider extends Closeable {

    List<String> getEnvironments();


    List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest);


}
