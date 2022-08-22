package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.log.Log;
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
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.settings.SettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import javax.swing.*;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class AnalyticsService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private final Environment environment;
    private String myApiUrl;
    @Nullable
    private String myApiToken;
    private final Project project;

    private AnalyticsProvider analyticsProviderProxy;


    public AnalyticsService(@NotNull Project project) {
        //initialize BackendConnectionMonitor when starting, so it is aware early on connection statuses
        project.getService(BackendConnectionMonitor.class);
        SettingsState settingsState = project.getService(SettingsState.class);
        PersistenceService persistenceService = project.getService(PersistenceService.class);
        environment = new Environment(project, this, persistenceService.getState(), settingsState);
        this.project = project;
        myApiUrl = settingsState.apiUrl;
        myApiToken = settingsState.apiToken;
        replaceClientAndFireChange(myApiUrl, myApiToken);
        settingsState.addChangeListener(state -> {
            if (!Objects.equals(state.apiUrl, myApiUrl)) {
                myApiUrl = state.apiUrl;
                myApiToken = state.apiToken;
                replaceClientAndFireChange(myApiUrl, myApiToken);
            }
            if (!Objects.equals(state.apiToken, myApiToken)) {
                myApiToken = state.apiToken;
                replaceClient(myApiUrl, myApiToken);
            }
        });
    }

    public static AnalyticsService getInstance(@NotNull Project project) {
        return project.getService(AnalyticsService.class);
    }

    public Environment getEnvironment() {
        return environment;
    }

    //just replace the client and do not fire any events
    //this method should be synchronized, and it shouldn't be a problem that really doesn't happen too often.
    private synchronized void replaceClient(String url, String token) {
        if (analyticsProviderProxy != null) {
            try {
                analyticsProviderProxy.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        analyticsProviderProxy = newAnalyticsProviderProxy(new RestAnalyticsProvider(url, token), project);
    }


    private void replaceClientAndFireChange(String url, String token) {

        replaceClient(url, token);

        var r = new Runnable() {
            @Override
            public void run() {
                List<String> envs = getEnvironments();
                if (envs == null) {
                    envs = new ArrayList<>();
                }

                environment.replaceEnvironmentsList(envs);
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            new Task.Backgroundable(project, "Digma: Environments list changed...") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    r.run();
                }
            }.queue();
        } else {
            r.run();
        }

    }


    List<String> getEnvironments() {
        try {
            return analyticsProviderProxy.getEnvironments();
        } catch (Exception e) {
            //getEnvironments should never throw exception. it is called only from the constructor or be the
            //Environment object that can handle null.
            return null;
        }
    }

    private String getCurrentEnvironment() throws AnalyticsServiceException {
        String currentEnv = environment.getCurrent();
        if (currentEnv == null || currentEnv.isEmpty()){
            throw new AnalyticsServiceException("No selected environment");
        }
        return currentEnv;
    }


    public List<CodeObjectSummary> getSummaries(List<String> objectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() -> analyticsProviderProxy.getSummaries(new CodeObjectSummaryRequest(env, objectIds)));
    }

    public List<GlobalInsight> getGlobalInsights() throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() -> analyticsProviderProxy.getGlobalInsights(new InsightsRequest(env, Collections.emptyList())));
    }

    public List<CodeObjectInsight> getInsights(List<String> objectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() -> analyticsProviderProxy.getInsights(new InsightsRequest(env, objectIds)));
    }

    public List<CodeObjectError> getErrorsOfCodeObject(String codeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() -> analyticsProviderProxy.getErrorsOfCodeObject(env, codeObjectId));
    }

    public CodeObjectErrorDetails getErrorDetails(String errorUid) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getCodeObjectErrorDetails(errorUid));
    }

    public UsageStatusResult getUsageStatus(List<String> objectIds) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds)));
    }

    public UsageStatusResult getUsageStatusOfErrors(List<String> objectIds) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds, List.of("Error"))));
    }

    public String getHtmlGraphForSpanPercentiles(String instrumentationLibrary, String spanName) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironment(), spanName, instrumentationLibrary, "");
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanPercentiles(spanHistogramQuery));
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
        } catch (Exception e) {
            Log.log(LOGGER::error, "exception while closing AnalyticsProvider {}", e.getMessage());
        }
    }


    /**
     * The AnalyticsService class is an IDE component and better throw declared known exceptions.
     * this is a catch-all method that will translate AnalyticsProvider exceptions to AnalyticsServiceException.
     * It's not a replacement to the AnalyticsProvider proxy, a proxy is more suitable for different things. The proxy will
     * throw an AnalyticsProvider exception if that was the source of the exception, otherwise it may throw an
     * UndeclaredThrowableException if something else goes wrong. in all cases we want to rethrow an AnalyticsServiceException
     * because that's what the plugin code expects.
     * All calls to the AnalyticsProvider proxy must be wrapped by a call to this method.
     * this is purely catch exceptions and rethrow AnalyticsServiceException.
     */
    private <T> T executeCatching(Supplier<T> tSupplier) throws AnalyticsServiceException {
        try {
            return tSupplier.get();
        } catch (AnalyticsProviderException e) {
            throw new AnalyticsServiceException("An AnalyticsProviderException was caught", e);
        } catch (UndeclaredThrowableException e) {
            throw new AnalyticsServiceException("UndeclaredThrowableException caught", e.getUndeclaredThrowable());
        } catch (Exception e) {
            throw new AnalyticsServiceException("Unknown exception", e);
        }
    }


    /**
     * A proxy for cross-cutting concerns across all api methods.
     * In a proxy it's easier to log events, we have the method name, parameters etc.
     * easier to investigate exceptions, if its an InvocationTargetException or IllegalAccessException etc.
     */
    private static class AnalyticsInvocationHandler implements InvocationHandler {

        private final AnalyticsProvider analyticsProvider;
        private final Project project;

        //ObjectMapper here is only used for printing the result to log as json
        private final ObjectMapper objectMapper = new ObjectMapper();

        //this status is only used for helping with reporting messages only when necessary and keep the log clean
        private final Status status = new Status();

        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider,Project project) {
            this.analyticsProvider = analyticsProvider;
            this.project = project;
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            //these methods are called by idea debugger and flags are changed while debugging when they should not.
            //and anyway there methods do not need cross-cutting concerns that this proxy offers.
            if (method.getName().equals("toString") ||
                    method.getName().equals("hashCode") ||
                    method.getName().equals("equals") ||
                    method.getName().equals("close")) {
                return method.invoke(analyticsProvider, args);
            }


            var stopWatch = StopWatch.createStarted();

            try {

                if (LOGGER.isDebugEnabled()) {
                    Log.log(LOGGER::debug, "Sending request to {}: args '{}'", method.getName(), argsToString(args));
                }

                Object result = method.invoke(analyticsProvider, args);

                if (LOGGER.isDebugEnabled()) {
                    Log.log(LOGGER::debug, "Got response from {}: args '{}', -----------------" +
                            "Result '{}'",method.getName(), argsToString(args),resultToString(result));
                }

                //reset status on success call
                if (status.isInConnectionError()) {
                    NotificationUtil.showNotification(project, "Digma: Connection reestablished !");
                    project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionGained();
                }
                status.ok();

                return result;

            } catch (InvocationTargetException e) {

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log to error only the first time of ConnectException and show an error notification.
                // following exceptions will just be logged to the log file
                boolean isConnectionException = isConnectionException(e) || isSslConnectionException(e);
                if (status.isOk() && isConnectionException) {
                    status.connectError();
                    project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionLost();
                    var message = isConnectionException(e) ? getConnectExceptionMessage(e) : getSslExceptionMessage(e);
                    Log.log(LOGGER::warn, "Connection exception: error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                    LOGGER.warn(e);
                    NotificationUtil.notifyError(project, "<html>Connection error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                } else if (status.isOk()) {
                    status.error();
                    Log.log(LOGGER::debug,"Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getCause().getMessage());
                    var message = getExceptionMessage(e);
                    NotificationUtil.notifyError(project, "<html>Error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                } else if (!status.hadError(e)) {
                    status.error();
                    var message = getExceptionMessage(e);
                    Log.log(LOGGER::debug, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                }


                if (e.getCause() instanceof AnalyticsProviderException) {
                    throw e.getCause();
                }

                throw new AnalyticsServiceException(e);

            } catch (Throwable e) {
                status.error();
                Log.log(LOGGER::debug, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getMessage());
                throw e;
            } finally {
                stopWatch.stop();
                Log.log(LOGGER::debug, "Api call {} took {} milliseconds", method.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
            }
        }

        private boolean isConnectionException(InvocationTargetException e) {

            var ex = e.getCause();
            while (ex != null && !(isConnectionUnavailableException(ex))) {
                ex = ex.getCause();
            }
            if (ex != null){
                return true;
            }

            return e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Error 404");
        }


        private String getConnectExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(isConnectionUnavailableException(ex))) {
                ex = ex.getCause();
            }
            if (ex != null) {
                return ex.getMessage();
            }

            return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        }


        //Exceptions that may indicate that connection can't be established
        private boolean isConnectionUnavailableException(Throwable exception) {

            //InterruptedIOException is thrown when the connection is dropped , for example by iptables

            return exception instanceof ConnectException ||
                    exception instanceof InterruptedIOException;

        }


        private boolean isSslConnectionException(InvocationTargetException e) {

            var ex = e.getCause();
            while (ex != null && !(ex instanceof SSLException)) {
                ex = ex.getCause();
            }
            return ex != null;
        }

        private String getSslExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof SSLException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return ex.getMessage();
            }

            return e.getCause() != null? e.getCause().getMessage():e.getMessage();
        }


        private String getExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof AnalyticsProviderException)){
                ex = ex.getCause();
            }
            if (ex != null){
                return ex.getMessage();
            }

            return e.getCause() != null? e.getCause().getMessage():e.getMessage();
        }




        private String resultToString(Object result) {
            try{
                //pretty print doesn't work in intellij logs, line end cause the text to disappear.
                return objectMapper.writeValueAsString(result);
            }catch (Exception e){
                return "Error parsing object "+e.getMessage();
            }
        }
        private String argsToString(Object[] args){
            try {
                return (args == null || args.length == 0) ? "" : Arrays.stream(args).map(Object::toString).collect(Collectors.joining(","));
            }catch (Exception e){
                return "Error parsing args "+e.getMessage();
            }
        }

    }



    private AnalyticsProvider newAnalyticsProviderProxy(AnalyticsProvider obj, Project project) {
        return (AnalyticsProvider) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                new Class[]{AnalyticsProvider.class, Closeable.class},
                new AnalyticsInvocationHandler(obj,project));
    }


    private static class Status {

        private final Map<String, Boolean> errorsHistory = new HashMap<>();

        private boolean hadConnectException = false;
        private boolean hadError = false;

        boolean isInError() {
            return hadConnectException || hadError;
        }

        boolean isInConnectionError() {
            return hadConnectException;
        }

        boolean isOk() {
            return !isInError();
        }

        public void ok() {
            hadConnectException = false;
            hadError = false;
            errorsHistory.clear();
        }

        public void connectError() {
            hadConnectException = true;
        }


        public void error() {
            hadError = true;
        }

        public boolean hadError(InvocationTargetException e) {
            var ex = e.getCause();
            if (ex != null){
                var errorName = ex.getClass().getName();
                if (errorsHistory.containsKey(errorName)){
                    return true;
                }
                errorsHistory.put(errorName,true);
            }

            return false;
        }
    }

}
