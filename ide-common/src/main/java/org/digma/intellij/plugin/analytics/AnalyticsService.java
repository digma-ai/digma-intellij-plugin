package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.CodeObjectSummary;
import org.digma.intellij.plugin.model.rest.CodeObjectSummaryRequest;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

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
                Log.log(LOGGER::error, "error invoking AnalyticsProvide.{}, exception {}", method.getName(), e);
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
