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
     * could be implemented on RestAnalyticsProvider, but I prefer to keep RestAnalyticsProvider pure,throwing exceptions and testable.
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
                //fixme!! maybe replace the rest framework with one that works nice in intellij
                // see:https://github.com/resteasy/resteasy/discussions/3096
                //resteasy can't load ProxyBuilderImpl and fails on CNFE. intellij has its own PluginClassLoader ,
                // but resteasy's code replaces intellij's PluginClassLoader with currentThread.getContextClassLoader()
                // before loading ProxyBuilderImpl and the currentThread.getContextClassLoader() doesn't know how to load
                // classes from a plugin zip.
                ClassLoader current = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(AnalyticsProvider.class.getClassLoader());

                try {
                    return method.invoke(analyticsProvider, args);
                }finally {
                    Thread.currentThread().setContextClassLoader(current);
                }

            } catch (Exception e) {
                Log.log(LOGGER::error, "error invoking {}, exception {}", method.getName(), e);
                //todo: maybe return empty object
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
