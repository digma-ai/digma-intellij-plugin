package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.summary.CodeObjectSummaryRequest;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyticsService implements AnalyticsProvider, Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    //todo: where is the url configured ?
    private final String baseUrl = "http://localhost:5051";

    private final Project project;

    private final AnalyticsProvider analyticsProviderProxy;

    public AnalyticsService(Project project) {
        this.project = project;
        analyticsProviderProxy = newAnalyticsProviderProxy(new RestAnalyticsProvider(baseUrl));
    }


    @Override
    public List<String> getEnvironments() {
        return analyticsProviderProxy.getEnvironments();
    }

    @Override
    public List<CodeObjectSummary> getSummaries(CodeObjectSummaryRequest summaryRequest) {
        return analyticsProviderProxy.getSummaries(summaryRequest);
    }

    @Override
    public List<CodeObjectInsight> getInsights(InsightsRequest insightsRequest) {
        return analyticsProviderProxy.getInsights(insightsRequest);
    }

    @Override
    public List<CodeObjectError> getErrorsOfCodeObject(String environment, String codeObjectId) {
        return analyticsProviderProxy.getErrorsOfCodeObject(environment, codeObjectId);
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
        } catch (Exception e) {
            Log.log(LOGGER::error, "exception while closing AnalyticsProvider {}", e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        dispose();
    }


    /**
     * A proxy to swallow all AnalyticsProvider exceptions.
     * the AnalyticsService class is an IDE component and better not to throw exceptions.
     */
    private class AnalyticsInvocationHandler implements InvocationHandler {

        private AnalyticsProvider analyticsProvider;

        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider) {
            this.analyticsProvider = analyticsProvider;
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            try {
                return method.invoke(analyticsProvider, args);
            } catch (Exception e) {
                if (args != null && args.length > 0) {
                    String argsStr = Arrays.stream(args).map(Object::toString).collect(Collectors.joining(","));
                    Log.error(LOGGER, e, "error invoking AnalyticsProvide.{}({}), exception {}", method.getName(), argsStr, e);
                } else {
                    Log.error(LOGGER, e, "error invoking AnalyticsProvide.{}, exception {}", method.getName(), e);
                }
                //todo: we probably need to show some error message to user
                return null;
            }

        }
    }

    private AnalyticsProvider newAnalyticsProviderProxy(AnalyticsProvider obj) {
        return (AnalyticsProvider) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                new Class[]{AnalyticsProvider.class, Closeable.class},
                new AnalyticsInvocationHandler(obj));
    }

}
