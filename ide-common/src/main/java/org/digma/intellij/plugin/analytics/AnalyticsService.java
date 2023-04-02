package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.CustomStartTimeInsightRequest;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightOfMethodsRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.settings.SettingsState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.net.ssl.SSLException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    //this status is used to keep track of connection errors, and for helping with reporting messages only when necessary
    // and keep the log clean
    private final Status status = new Status();

    public AnalyticsService(@NotNull Project project) {
        //initialize BackendConnectionMonitor when starting, so it is aware early on connection statuses
        project.getService(BackendConnectionMonitor.class);
        SettingsState settingsState = SettingsState.getInstance();
        environment = new Environment(project, this, PersistenceService.getInstance().getState(), settingsState);
        this.project = project;
        myApiUrl = settingsState.apiUrl;
        myApiToken = settingsState.apiToken;
        replaceClient(myApiUrl, myApiToken);
        initializeEnvironmentsList();
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
                Log.log(LOGGER::warn, e.getMessage());
            }
        }

        analyticsProviderProxy = newAnalyticsProviderProxy(new RestAnalyticsProvider(url, token));
    }


    private void initializeEnvironmentsList() {
        List<String> envs = getEnvironments();
        if (envs == null) {
            envs = new ArrayList<>();
        }

        environment.replaceEnvironmentsList(envs);
    }


    private void replaceClientAndFireChange(String url, String token) {

        Backgroundable.ensureBackground(project, "Digma: Environments list changed", () -> {
            replaceClient(url, token);
            List<String> envs = getEnvironments();
            if (envs == null) {
                envs = new ArrayList<>();
            }

            environment.replaceEnvironmentsListAndFireChange(envs);
        });

    }


    public List<String> getEnvironments() {
        try {
            return analyticsProviderProxy.getEnvironments();
        } catch (Exception e) {
            //getEnvironments should never throw exception.
            // it is called only from this class or from the Environment object and both can handle null.
            return null;
        }
    }

    private String getCurrentEnvironment() throws AnalyticsServiceException {
        String currentEnv = environment.getCurrent();
        //todo: we probably don't need to refresh environments here
//        if (currentEnv == null || currentEnv.isEmpty()){
//            environment.refreshEnvironments();
//        }
        if (currentEnv == null || currentEnv.isEmpty()) {
            throw new AnalyticsServiceException("No selected environment");
        }
        return currentEnv;
    }


    public void sendDebuggerEvent(int eventType, String timestamp) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.sendDebuggerEvent(new DebuggerEventRequest(String.valueOf(eventType), CommonUtils.getLocalHostname(), timestamp));
            return null;
        });
    }

    public List<GlobalInsight> getGlobalInsights() throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::debug, "Requesting Global Insights for next environment {}", env);
        var insights = executeCatching(() -> analyticsProviderProxy.getGlobalInsights(new InsightsRequest(env, Collections.emptyList())));
        if (insights == null) {
            insights = Collections.emptyList();
        }
        onInsightReceived(insights);
        return insights;
    }

    public List<CodeObjectInsight> getInsights(List<String> objectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::debug, "Requesting insights for next objectIds {} and next environment {}", objectIds, env);
        var insights = executeCatching(() -> analyticsProviderProxy.getInsights(new InsightsRequest(env, objectIds)));
        if (insights == null) {
            insights = Collections.emptyList();
        }
        onInsightReceived(insights);
        return insights;
    }

    private <TInsight> void onInsightReceived(List<TInsight> insights) {
        if (insights != null && !insights.isEmpty() && !SettingsState.getInstance().firstTimeInsightReceived) {
            ActivityMonitor.getInstance(project).registerFirstInsightReceived();
            SettingsState.getInstance().firstTimeInsightReceived = true;
            SettingsState.getInstance().fireChanged();
        }
    }

    public List<CodeObjectError> getErrorsOfCodeObject(List<String> codeObjectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::debug, "Requesting insights for next codeObjectId {} and next environment {}", codeObjectIds, env);
        var errors = executeCatching(() -> analyticsProviderProxy.getErrorsOfCodeObject(env, codeObjectIds));
        if (errors == null) {
            errors = Collections.emptyList();
        }
        return errors;
    }

    public CodeObjectInsightsStatusResponse getCodeObjectInsightStatus(List<MethodInfo> methodInfos) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        var methodWithCodeObjects = methodInfos.stream()
                .map(it -> toMethodWithCodeObjects(it))
                .toList();
        return executeCatching(() -> analyticsProviderProxy.getCodeObjectInsightStatus(new InsightOfMethodsRequest(env, methodWithCodeObjects)));
    }

    @VisibleForTesting
    public static MethodWithCodeObjects toMethodWithCodeObjects(MethodInfo methodInfo) {
        return new MethodWithCodeObjects(methodInfo.idWithType(),
                methodInfo.getSpans().stream().map(it -> it.idWithType()).toList(),
                methodInfo.getEndpoints().stream().map(it -> it.idWithType()).toList()
        );
    }

    public void setInsightCustomStartTime(String codeObjectId, InsightType insightType) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        String formattedActualDate = Instant.now().toString();//FYI: by UTC time zone
        executeCatching(() -> {
            analyticsProviderProxy.setInsightCustomStartTime(
                    new CustomStartTimeInsightRequest(
                            env,
                            codeObjectId,
                            insightType.name(),
                            formattedActualDate
                    ));
            return null;
        });
    }

    public CodeObjectErrorDetails getErrorDetails(String errorUid) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getCodeObjectErrorDetails(errorUid));
    }

    public UsageStatusResult getUsageStatus(List<String> objectIds) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds)));
    }

    public RecentActivityResult getRecentActivity(List<String> environments) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getRecentActivity(new RecentActivityRequest(environments)));
    }

    public UsageStatusResult getUsageStatusOfErrors(List<String> objectIds) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds, List.of("Error"))));
    }

    public String getHtmlGraphForSpanPercentiles(String instrumentationLibrary, String spanName, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironment(), spanName, instrumentationLibrary, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanPercentiles(spanHistogramQuery));
    }

    public String getHtmlGraphForSpanScaling(String instrumentationLibrary, String spanName, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironment(), spanName, instrumentationLibrary, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanScaling(spanHistogramQuery));
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
        } catch (Exception e) {
            Log.error(LOGGER, project, e, "exception while closing AnalyticsProvider {}", e.getMessage());
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


    private AnalyticsProvider newAnalyticsProviderProxy(AnalyticsProvider obj) {
        return (AnalyticsProvider) java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                new Class[]{AnalyticsProvider.class, Closeable.class},
                new AnalyticsInvocationHandler(obj));
    }


    /**
     * A proxy for cross-cutting concerns across all api methods.
     * In a proxy it's easier to log events, we have the method name, parameters etc.
     * easier to investigate exceptions, if it's an InvocationTargetException or IllegalAccessException etc.
     * It's an inner class intentionally, so it has access to the enclosing AnalyticsService members.
     */
    private class AnalyticsInvocationHandler implements InvocationHandler {

        private final AnalyticsProvider analyticsProvider;

        //ObjectMapper here is only used for printing the result to log as json
        private final ObjectMapper objectMapper = new ObjectMapper();


        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider) {
            this.analyticsProvider = analyticsProvider;
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
                            "Result '{}'", method.getName(), argsToString(args), resultToString(result));
                }

                if (!SettingsState.getInstance().firstTimeConnectionEstablished) {
                    ActivityMonitor.getInstance(project).registerFirstConnectionEstablished();
                    SettingsState.getInstance().firstTimeConnectionEstablished = true;
                    SettingsState.getInstance().fireChanged();
                }

                //reset status on success call
                if (status.isInConnectionError()) {
                    // change status here because connectionGained() will trigger call to this invoke() again
                    // and without changing the status Notification will be displayed several times in a loop
                    status.ok();
                    NotificationUtil.showNotification(project, "Digma: Connection reestablished !");
                    project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionGained();
                }
                status.ok();

                return result;

            } catch (InvocationTargetException e) {

                //Note: when logging LOGGER.error idea will popup a red message which we don't want, so only report warn messages.

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log connection exceptions only the first time and show an error notification.
                // while status is in error the following connection exceptions will not be logged, other exceptions
                // will be logged only once.

                if (e.getTargetException().getCause() instanceof SocketTimeoutException) {
                    Log.log(LOGGER::warn, "SocketTimeoutException for {} request => {}", method.getName(), e.getTargetException().getCause());
                    return null;
                }

                boolean isConnectionException = isConnectionException(e) || isSslConnectionException(e);
                if (status.isOk() && isConnectionException) {
                    status.connectError();
                    project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionLost();
                    var message = isConnectionException(e) ? getConnectExceptionMessage(e) : getSslExceptionMessage(e);
                    Log.log(LOGGER::warn, "Connection exception: error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                    LOGGER.warn(e);
                    ActivityMonitor.getInstance(project).registerConnectionError(method.getName(), message);
                    NotificationUtil.notifyError(project, "<html>Connection error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                } else if (status.isOk()) {
                    status.error();
                    Log.log(LOGGER::warn, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getCause().getMessage());
                    var message = getExceptionMessage(e);
                    NotificationUtil.notifyError(project, "<html>Error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                    LOGGER.warn(e);
                    ActivityMonitor.getInstance(project).registerError(e, message);
                } else if (!status.hadError(e)) {
                    status.error();
                    var message = getExceptionMessage(e);
                    Log.log(LOGGER::warn, "New Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                    LOGGER.warn(e);
                    ActivityMonitor.getInstance(project).registerError(e, message);
                }


                if (e.getCause() instanceof AnalyticsProviderException) {
                    throw e.getCause();
                }

                throw new AnalyticsServiceException(e);

            } catch (Exception e) {
                status.error();
                Log.log(LOGGER::debug, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getMessage());
                LOGGER.error(e);
                ActivityMonitor.getInstance(project).registerError(e, "Error invoking AnalyticsProvider");
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
            if (ex != null) {
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

            return exception instanceof SocketException ||
                    exception instanceof UnknownHostException ||
                    exception instanceof HttpTimeoutException ||
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
            while (ex != null && !(ex instanceof SSLException)) {
                ex = ex.getCause();
            }
            if (ex != null) {
                return ex.getMessage();
            }

            return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        }


        private String getExceptionMessage(InvocationTargetException e) {
            var ex = e.getCause();
            while (ex != null && !(ex instanceof AnalyticsProviderException)) {
                ex = ex.getCause();
            }
            if (ex != null) {
                return ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            }

            return e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        }


        private String resultToString(Object result) {
            try {
                //pretty print doesn't work in intellij logs, line end cause the text to disappear.
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                return "Error parsing object " + e.getMessage();
            }
        }

        private String argsToString(Object[] args) {
            try {
                return (args == null || args.length == 0) ? "" : Arrays.stream(args).map(Object::toString).collect(Collectors.joining(","));
            } catch (Exception e) {
                return "Error parsing args " + e.getMessage();
            }
        }

    }


    private static class Status {

        private final Map<String, Boolean> errorsHistory = new HashMap<>();

        private final AtomicBoolean hadConnectException = new AtomicBoolean(false);
        private boolean hadError = false;

        boolean isInError() {
            return hadConnectException.get() || hadError;
        }

        boolean isInConnectionError() {
            return hadConnectException.get();
        }

        boolean isOk() {
            return !isInError();
        }

        public void ok() {
            hadConnectException.set(false);
            hadError = false;
            errorsHistory.clear();
        }

        public void connectError() {
            hadConnectException.set(true);
        }


        public void error() {
            hadError = true;
        }

        public boolean hadError(InvocationTargetException e) {
            var cause = findRealError(e);
            var errorName = cause.getClass().getName();
            if (errorsHistory.containsKey(errorName)) {
                return true;
            }
            errorsHistory.put(errorName, true);

            return false;
        }

        @NotNull
        private Throwable findRealError(InvocationTargetException e) {

            Throwable cause = e.getCause();
            while (cause != null && !cause.getClass().equals(AnalyticsProviderException.class)) {
                cause = cause.getCause();
            }

            if (cause != null && cause.getCause() != null) {
                return cause.getCause();
            }

            return cause == null ? e : cause;
        }
    }

}
