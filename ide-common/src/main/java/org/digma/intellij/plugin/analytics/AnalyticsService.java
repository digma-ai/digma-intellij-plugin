package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.Alarm;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.common.DatesUtils;
import org.digma.intellij.plugin.common.EDT;
import org.digma.intellij.plugin.common.ExceptionUtils;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.InsightType;
import org.digma.intellij.plugin.model.discovery.EndpointInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetsRequest;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.DeleteEnvironmentRequest;
import org.digma.intellij.plugin.model.rest.env.DeleteEnvironmentResponse;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsRequest;
import org.digma.intellij.plugin.model.rest.event.LatestCodeObjectEventsResponse;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsightsStatusResponse;
import org.digma.intellij.plugin.model.rest.insights.CustomStartTimeInsightRequest;
import org.digma.intellij.plugin.model.rest.insights.GlobalInsight;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfMethodsResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanRequest;
import org.digma.intellij.plugin.model.rest.insights.InsightsOfSingleSpanResponse;
import org.digma.intellij.plugin.model.rest.insights.InsightsRequest;
import org.digma.intellij.plugin.model.rest.insights.MethodWithCodeObjects;
import org.digma.intellij.plugin.model.rest.insights.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveDataRequest;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigation;
import org.digma.intellij.plugin.model.rest.navigation.CodeObjectNavigationRequest;
import org.digma.intellij.plugin.model.rest.notifications.GetUnreadNotificationsCountRequest;
import org.digma.intellij.plugin.model.rest.notifications.NotificationsRequest;
import org.digma.intellij.plugin.model.rest.notifications.SetReadNotificationsRequest;
import org.digma.intellij.plugin.model.rest.notifications.UnreadNotificationsCountResponse;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusRequest;
import org.digma.intellij.plugin.model.rest.usage.UsageStatusResult;
import org.digma.intellij.plugin.model.rest.version.PerformanceMetricsResponse;
import org.digma.intellij.plugin.model.rest.version.VersionRequest;
import org.digma.intellij.plugin.model.rest.version.VersionResponse;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.analytics.EnvironmentRefreshSchedulerKt.scheduleEnvironmentRefresh;
import static org.digma.intellij.plugin.common.EnvironmentUtilKt.isEnvironmentLocal;
import static org.digma.intellij.plugin.common.EnvironmentUtilKt.isEnvironmentLocalTests;
import static org.digma.intellij.plugin.common.EnvironmentUtilKt.isLocalEnvironmentMine;
import static org.digma.intellij.plugin.common.ExceptionUtils.getConnectExceptionMessage;
import static org.digma.intellij.plugin.common.ExceptionUtils.getSslExceptionMessage;
import static org.digma.intellij.plugin.common.ExceptionUtils.isConnectionException;
import static org.digma.intellij.plugin.common.ExceptionUtils.isEOFException;
import static org.digma.intellij.plugin.common.ExceptionUtils.isSslConnectionException;
import static org.digma.intellij.plugin.model.Models.Empties.EmptyUsageStatusResult;


public class AnalyticsService implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private final Environment environment;
    private String myApiUrl;
    @Nullable
    private String myApiToken;
    private final Project project;

    /**
     * myConnectionLostFlag must be a member of AnalyticsService and not of the AnalyticsInvocationHandler proxy.
     * AnalyticsInvocationHandler may be replaced , AnalyticsService is a singleton.
     * the error that may happen if myConnectionLostFlag is a member of AnalyticsInvocationHandler is:
     * we got a connection error,myConnectionLostFlag is marked to true. user changes the url in settings,
     * AnalyticsInvocationHandler is replaced with a new instance and myConnectionLostFlag is false, the next successful
     * call will not reset the status, and we're locked in connection lost.
     * when myConnectionLostFlag is a member of AnalyticsService as is here, it will not happen, it is a singleton
     * for the project and new instances of AnalyticsInvocationHandler will see its real state.
     */
    private final AtomicBoolean myConnectionLostFlag = new AtomicBoolean(false);


    private AnalyticsProvider analyticsProviderProxy;

    public AnalyticsService(@NotNull Project project) {
        //initialize BackendConnectionMonitor when starting, so it is aware early on connection statuses
        BackendConnectionMonitor.getInstance(project);
        //initialize MainToolWindowCardsController when starting, so it is aware early on connection statuses
        MainToolWindowCardsController.getInstance(project);
        SettingsState settingsState = SettingsState.getInstance();
        environment = new Environment(project, this);
        this.project = project;
        myApiUrl = settingsState.apiUrl;
        myApiToken = settingsState.apiToken;
        replaceClient(myApiUrl, myApiToken);
        scheduleEnvironmentRefresh(this, environment);

        settingsState.addChangeListener(state -> {

            boolean shouldReplaceClient = false;

            if (!Objects.equals(state.apiUrl, myApiUrl)) {
                myApiUrl = state.apiUrl;
                myApiToken = state.apiToken;
                shouldReplaceClient = true;
            }
            if (!Objects.equals(state.apiToken, myApiToken)) {
                myApiToken = state.apiToken;
                shouldReplaceClient = true;
            }

            if (shouldReplaceClient) {
                replaceClient(myApiUrl, myApiToken);
            }

        }, this);
    }


    public static AnalyticsService getInstance(@NotNull Project project) {
        return project.getService(AnalyticsService.class);
    }

    public Environment getEnvironment() {
        return environment;
    }


    //just replace the client and do not fire any events
    //this method should be synchronized, and it shouldn't be a problem, it doesn't happen too often.
    private synchronized void replaceClient(String url, String token) {
        if (analyticsProviderProxy != null) {
            try {
                analyticsProviderProxy.close();
            } catch (IOException e) {
                Log.log(LOGGER::warn, e.getMessage());
            }
        }
        RestAnalyticsProvider analyticsProvider = new RestAnalyticsProvider(url, token);
        analyticsProviderProxy = newAnalyticsProviderProxy(analyticsProvider);

        environment.refreshNowOnBackground();

    }


    @NotNull
    public ConnectionTestResult testRemoteConnection(@NotNull String serverUrl, @Nullable String token) {
        try (RestAnalyticsProvider analyticsProvider = new RestAnalyticsProvider(serverUrl, token)) {
            //todo: use health check to test connection
            var envs = analyticsProvider.getEnvironments();
            if (envs != null) {
                return ConnectionTestResult.success();
            }
            return ConnectionTestResult.failure("unknown");
        } catch (Exception e) {
            ErrorReporter.getInstance().reportError(project, "AnalyticsService.testRemoteConnection", e);
            return ConnectionTestResult.failure(ExceptionUtils.getNonEmptyMessage(e));
        }

    }


    @Nullable
    public List<String> getEnvironments() {
        try {
            var environments = analyticsProviderProxy.getEnvironments();
            var hostName = CommonUtils.getLocalHostname();
            //filter out other LOCAL environments, keep only mine LOCAL
            return environments.stream()
                    .filter(env -> (!isEnvironmentLocal(env) && !isEnvironmentLocalTests(env)) || isLocalEnvironmentMine(env, hostName))
                    .toList();
        } catch (Exception e) {
            //getEnvironments should never throw exception.
            // it is called only from this class or from the Environment object and both can handle null.
            return null;
        }
    }

    private String getCurrentEnvironment() throws AnalyticsServiceException {
        String currentEnv = environment.getCurrent();
        if (currentEnv == null || currentEnv.isEmpty()) {
            throw new NoSelectedEnvironmentException("No selected environment");
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
        Log.log(LOGGER::trace, "Requesting Global Insights for next environment {}", env);
        var insights = executeCatching(() -> analyticsProviderProxy.getGlobalInsights(new InsightsRequest(env, Collections.emptyList())));
        if (insights == null) {
            insights = Collections.emptyList();
        }
        onInsightReceived(insights);
        return insights;
    }


    public LatestCodeObjectEventsResponse getLatestEvents(@NotNull String lastReceivedTime) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getLatestEvents(new LatestCodeObjectEventsRequest(environment.getEnvironments(), lastReceivedTime)));
    }


    /**
     * removed deprecation because its necessary for JaegerUIService#getImportance(java.util.List)
     */
    //@deprecated This method is deprecated and will be removed in a future release.
    //Use {@link #getInsightsOfMethods(List<MethodInfo>)} instead.
    public List<CodeObjectInsight> getInsights(List<String> objectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::trace, "Requesting insights for next objectIds {} and next environment {}", objectIds, env);
        var insights = executeCatching(() -> analyticsProviderProxy.getInsights(new InsightsRequest(env, objectIds)));
        if (insights == null) {
            insights = Collections.emptyList();
        } else {
            onInsightReceived(insights);
        }
        return insights;
    }

    private <TInsight> void onInsightReceived(List<TInsight> insightsOrMethodsWithInsights) {
        if (insightsOrMethodsWithInsights != null &&
                !insightsOrMethodsWithInsights.isEmpty() &&
                !PersistenceService.getInstance().getState().getFirstTimeInsightReceived()) {
            ActivityMonitor.getInstance(project).registerFirstInsightReceived();
            PersistenceService.getInstance().getState().setFirstTimeInsightReceived(true);
        }
    }


    public InsightsOfSingleSpanResponse getInsightsForSingleSpan(String spanId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::debug, "Requesting insights for span {}", spanId);
        return executeCatching(() -> analyticsProviderProxy.getInsightsForSingleSpan(new InsightsOfSingleSpanRequest(env, spanId)));
    }


    public InsightsOfMethodsResponse getInsightsOfMethods(List<MethodInfo> methodInfos) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::trace, "Requesting insights for next methodInfos {} and next environment {}", methodInfos, env);
        var methodWithCodeObjects = methodInfos.stream()
                .map(AnalyticsService::toMethodWithCodeObjects)
                .toList();
        InsightsOfMethodsResponse insightsOfMethodsResponse = executeCatching(() -> analyticsProviderProxy.getInsightsOfMethods(new InsightsOfMethodsRequest(env, methodWithCodeObjects)));
        if (insightsOfMethodsResponse != null && !insightsOfMethodsResponse.getMethodsWithInsights().isEmpty()) {
            onInsightReceived(insightsOfMethodsResponse.getMethodsWithInsights());
        }
        return insightsOfMethodsResponse;
    }

    public List<CodeObjectError> getErrorsOfCodeObject(List<String> codeObjectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        Log.log(LOGGER::trace, "Requesting insights for next codeObjectId {} and next environment {}", codeObjectIds, env);
        var errors = executeCatching(() -> analyticsProviderProxy.getErrorsOfCodeObject(env, codeObjectIds));
        if (errors == null) {
            errors = Collections.emptyList();
        }
        return errors;
    }

    public CodeObjectInsightsStatusResponse getCodeObjectInsightStatus(List<MethodInfo> methodInfos) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        var methodWithCodeObjects = methodInfos.stream()
                .map(AnalyticsService::toMethodWithCodeObjects)
                .toList();
        return executeCatching(() -> analyticsProviderProxy.getCodeObjectInsightStatus(new InsightsOfMethodsRequest(env, methodWithCodeObjects)));
    }

    @VisibleForTesting
    public static MethodWithCodeObjects toMethodWithCodeObjects(MethodInfo methodInfo) {
        return new MethodWithCodeObjects(methodInfo.idWithType(),
                methodInfo.getSpans().stream().map(SpanInfo::idWithType).toList(),
                methodInfo.getEndpoints().stream().map(EndpointInfo::idWithType).toList()
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
        return executeCatching(() -> {
            UsageStatusResult usageStatus = analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds));
            return usageStatus == null ? EmptyUsageStatusResult : usageStatus;
        });
    }

    public VersionResponse getVersions(VersionRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getVersions(request));
    }

    public RecentActivityResult getRecentActivity(List<String> environments) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getRecentActivity(new RecentActivityRequest(environments)));
    }

    public DurationLiveData getDurationLiveData(String codeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() ->
                analyticsProviderProxy.getDurationLiveData(new DurationLiveDataRequest(env, codeObjectId)));
    }

    public CodeObjectNavigation getCodeObjectNavigation(String spanCodeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() ->
                analyticsProviderProxy.getCodeObjectNavigation(new CodeObjectNavigationRequest(env, spanCodeObjectId)));
    }


    public UsageStatusResult getUsageStatusOfErrors(List<String> objectIds) throws AnalyticsServiceException {
        return executeCatching(() -> {
            UsageStatusResult usageStatus = analyticsProviderProxy.getUsageStatus(new UsageStatusRequest(objectIds, List.of("Error")));
            return usageStatus == null ? EmptyUsageStatusResult : usageStatus;
        });
    }

    public String getHtmlGraphForSpanPercentiles(String instrumentationLibrary, String spanName, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironment(), spanName, instrumentationLibrary, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanPercentiles(spanHistogramQuery));
    }

    public String getHtmlGraphForSpanScaling(String instrumentationLibrary, String spanName, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironment(), spanName, instrumentationLibrary, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanScaling(spanHistogramQuery));
    }


    public String getAssets() throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        var assets = executeCatching(() ->
                analyticsProviderProxy.getAssets(new AssetsRequest(env)));

        try {
            if (!PersistenceService.getInstance().getState().getFirstTimeAssetsReceived()) {
                var objectMapper = new ObjectMapper();
                var payload = objectMapper.readTree(assets);
                if (!payload.isMissingNode() &&
                        payload.get("serviceAssetsEntries") != null &&
                        payload.get("serviceAssetsEntries") instanceof ArrayNode &&
                        !((ArrayNode) payload.get("serviceAssetsEntries")).isEmpty()) {
                    ActivityMonitor.getInstance(project).registerFirstAssetsReceived();
                    PersistenceService.getInstance().getState().setFirstTimeAssetsReceived(true);
                }
            }
        } catch (Exception e) {
            Log.warnWithException(LOGGER, project, e, "error reporting FirstTimeAssetsReceived {}", e);
        }

        return assets;
    }


    public String getNotifications(String notificationsStartDate, String userId, int pageNumber, int pageSize, boolean isRead) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() ->
                analyticsProviderProxy.getNotifications(new NotificationsRequest(env, userId, notificationsStartDate, pageNumber, pageSize, isRead)));
    }

    public void setReadNotificationsTime(String upToDateTime, String userId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        executeCatching(() -> {
            analyticsProviderProxy.setReadNotificationsTime(new SetReadNotificationsRequest(env, userId, upToDateTime));
            return null;
        });
    }

    public UnreadNotificationsCountResponse getUnreadNotificationsCount(String notificationsStartDate, String userId) throws AnalyticsServiceException {
        var env = getCurrentEnvironment();
        return executeCatching(() -> analyticsProviderProxy.getUnreadNotificationsCount(new GetUnreadNotificationsCountRequest(env, userId, notificationsStartDate)));
    }


    public PerformanceMetricsResponse getPerformanceMetrics() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getPerformanceMetrics());
    }


    public AboutResult getAbout() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getAbout());
    }

    public DeleteEnvironmentResponse deleteEnvironment(@NotNull String environmentName) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.deleteEnvironment(new DeleteEnvironmentRequest(environmentName)));
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
        } catch (RuntimeExceptionWithAttachments e) {
            //this is a platform exception as a result of asserting non UI thread when calling backend API
            throw e;
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

        //this errorReportingHelper is used to keep track of errors for helping with reporting messages only when necessary
        // and keep the log clean
        private final ErrorReportingHelper errorReportingHelper = new ErrorReportingHelper();

        //ObjectMapper here is only used for printing the result to log as json
        private final ObjectMapper objectMapper = new ObjectMapper();

        private final ReentrantLock myConnectionLostLock = new ReentrantLock();


        private final Set<String> methodsToIgnoreExceptions = Set.of(new String[]{"getPerformanceMetrics", "getAbout"});

        //sometimes the connection lost is momentary or regaining is momentary, use the alarm to wait
        // before notifying listeners of connectionLost/ConnectionGained
        private final Alarm myConnectionStatusNotifyAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, AnalyticsService.this);


        public AnalyticsInvocationHandler(AnalyticsProvider analyticsProvider) {
            this.analyticsProvider = analyticsProvider;
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            //these methods do not need cross-cutting concerns that this proxy offers and may be called on ui thread.
            // toString for example will only be called by idea debugger.
            if (method.getName().equals("toString") ||
                    method.getName().equals("hashCode") ||
                    method.getName().equals("equals") ||
                    method.getName().equals("close")) {
                return method.invoke(analyticsProvider, args);
            }

            var stopWatch = StopWatch.createStarted();

            try {

                //assert not UI thread, should never happen.
                //noinspection UnstableApiUsage
                ApplicationManager.getApplication().assertIsNonDispatchThread();


                if (LOGGER.isTraceEnabled()) {
                    Log.log(LOGGER::trace, "Sending request to {}: args '{}'", method.getName(), argsToString(args));
                }

                Object result;
                try {
                    result = method.invoke(analyticsProvider, args);
                } catch (Exception e) {
                    //this is a poor retry, we don't have a retry mechanism, but sometimes there is a momentary
                    // connection issue and the next call will succeed, instead of going through the exception handling
                    // and events , just try again once. the performance penalty is minor, we are in error state anyway.
                    result = method.invoke(analyticsProvider, args);
                }

                if (LOGGER.isTraceEnabled()) {
                    Log.log(LOGGER::trace, "Got response from {}: args '{}', -----------------" +
                            "Result '{}'", method.getName(), argsToString(args), resultToString(result));
                }


                //todo: not thread safe so the block may be invoked more then once
                if (!PersistenceService.getInstance().getState().getFirstTimeConnectionEstablished()) {
                    ActivityMonitor.getInstance(project).registerFirstConnectionEstablished();
                    PersistenceService.getInstance().getState().setFirstTimeConnectionEstablished(true);
                    PersistenceService.getInstance().getState().setFirstTimeConnectionEstablishedTimestamp(DatesUtils.Instants.instantToString(Instant.now()));
                }

                //if we are here then the call to the underlying analytics api succeeded, we can reset the status
                // and notify connectionGained if necessary.
                //it is not perfect, there is still a race condition, we may report connection lost while actually the
                // connection is ok, or we may consider the connection ok while there is an error.
                //if two threads A and B enter this method, A gets an error and enters the exception handling,
                // right afet that B succeeds because maybe the error was momentary, and A still didn't mark
                // myConnectionLostFlag ,A will mark myConnectionLostFlag to true and notify connectionLost while
                // actually the connection is ok. Or the other way around.
                //but, the next call will fix it and the status will be ok. So this incorrect state is only until the
                // next call, which is probably ok.
                //to be more accurate we need to lock the whole critical section that actually calls the backend, but we
                // don't want to do that because then the performance penalty is significant for every call.
                //resetConnectionLostAndNotifyIfNecessary is significant penalty only when it needs to recover from connectionLost,
                // otherwise its very fast and insignificant in such an application.
                resetConnectionLostAndNotifyIfNecessary();

                return result;

            } catch (InvocationTargetException e) {

                //some methods may fail due to missing endpoint or some other technical issue that
                // is known. these methods should not impact the connection status or mark connectionLost.
                //so just throw an exception, code that calls these methods should be ready for AnalyticsServiceException.
                if (methodsToIgnoreExceptions.contains(method.getName())) {
                    Log.warnWithException(LOGGER, e, "failed executing method {}", method);
                    throw new AnalyticsServiceException(e);
                }

                //Note: when logging LOGGER.error idea will pop up a red message which we don't want, so only report warn messages.

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log connection exceptions only the first time and show an error notification.
                // while status is in error, following connection exceptions will not be logged, other exceptions
                // will be logged only once.

                //handleInvocationTargetException may rethrow an exception, if it didn't then always
                // an AnalyticsServiceException will be throws
                handleInvocationTargetException(e, method, args);
                throw new AnalyticsServiceException(e);

            } catch (Exception e) {
                errorReportingHelper.addIfNewError(e);
                Log.log(LOGGER::warn, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e.getMessage());
                Log.warnWithException(LOGGER, project, e, "error in analytics service method {},{}", method.getName(), e);
                ErrorReporter.getInstance().reportAnalyticsServiceError(project, "AnalyticsInvocationHandler.invoke", method.getName(), e, false);
                throw e;
            } finally {
                stopWatch.stop();
                Log.log(LOGGER::trace, "Api call {} took {} milliseconds", method.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
            }
        }


        private void handleInvocationTargetException(InvocationTargetException invocationTargetException, Method method, Object[] args) throws Throwable {
            boolean isConnectionException = isConnectionException(invocationTargetException) || isSslConnectionException(invocationTargetException);
            String message;
            if (isConnectionException(invocationTargetException)) {
                message = getConnectExceptionMessage(invocationTargetException);
            } else if (isSslConnectionException(invocationTargetException)) {
                message = getSslExceptionMessage(invocationTargetException);
            } else {
                message = invocationTargetException.getCause() != null ? invocationTargetException.getCause().getMessage() : invocationTargetException.getMessage();
            }
            if (isConnectionOK()) {
                //if more than one thread enter this section the worst that will happen is that we
                // report the error more than once but connectionLost will be fired once because
                // markConnectionLostAndNotify locks, marks and notifies only if connection ok.
                if (isConnectionException) {
                    markConnectionLostAndNotify();
                    errorReportingHelper.addIfNewError(invocationTargetException);
                    Log.log(LOGGER::warn, "Connection exception: error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                    NotificationUtil.notifyError(project, "<html>Connection error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                } else {
                    Log.log(LOGGER::warn, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), invocationTargetException.getCause().getMessage());
                    if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                        NotificationUtil.notifyError(project, "<html>Error with Digma backend api for method " + method.getName() + ".<br> "
                                + message + ".<br> See logs for details.");
                        if (isEOFException(invocationTargetException)) {
                            NotificationUtil.showBalloonWarning(project, "Digma API EOF error: " + message);
                        }
                    }
                }
            }
            // status was not ok but it's a new error
            else if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                Log.log(LOGGER::warn, "New Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                LOGGER.warn(invocationTargetException);
            }

            ErrorReporter.getInstance().reportAnalyticsServiceError(project, "AnalyticsInvocationHandler.invoke." + method.getName(), method.getName(), invocationTargetException, isConnectionException);

            if (invocationTargetException.getCause() instanceof AnalyticsProviderException) {
                throw invocationTargetException.getCause();
            }

        }


        private boolean isConnectionOK() {
            return !myConnectionLostFlag.get();
        }


        private void resetConnectionLostAndNotifyIfNecessary() {


            Log.log(LOGGER::trace, "resetConnectionLostAndNotifyIfNecessary called");

            //this is the critical section of the race condition, there is a performance penalty
            // for the locking , and if we recover from exception then also for the notification,
            // but only when recovering from connection lost, otherwise its very fast, and we are not a critical
            // multithreading application, so it's probably ok to lock in every API call
            // the reason for locking here and in markConnectionLostAndNotify is to avoid a situation were myConnectionLostFlag
            // if marked but never reset and to make sure that if we notified connectionLost we will also notify when its gained back.
            try {
                //if connection is ok do nothing.
                if (isConnectionOK()) {
                    Log.log(LOGGER::trace, "resetConnectionLostAndNotifyIfNecessary called, connection ok nothing to do.");
                    return;
                }
                Log.log(LOGGER::info, "acquiring lock to reset connection status after connection lost");
                myConnectionLostLock.lock();
                if (myConnectionLostFlag.get()) {
                    Log.log(LOGGER::warn, "resetting connection status after connection lost");
                    myConnectionLostFlag.set(false);
                    errorReportingHelper.reset();
                    myConnectionStatusNotifyAlarm.cancelAllRequests();

                    BackendConnectionMonitor.getInstance(project).connectionGained();
                    ActivityMonitor.getInstance(project).registerConnectionGained();

                    myConnectionStatusNotifyAlarm.addRequest(() -> {
                        Log.log(LOGGER::warn, "notifying connectionGained");
                        project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionGained();
                    }, 500);


                    EDT.ensureEDT(() -> NotificationUtil.showNotification(project, "Digma: Connection reestablished !"));
                }
            } finally {
                if (myConnectionLostLock.isHeldByCurrentThread()) {
                    myConnectionLostLock.unlock();
                }
            }
        }


        private void markConnectionLostAndNotify() {

            Log.log(LOGGER::warn, "markConnectionLostAndNotify called");

            //this is the second critical section of the race condition,
            // we are in error state so the performance penalty of locking is insignificant.
            try {
                Log.log(LOGGER::warn, "acquiring lock to mark connection lost");
                myConnectionLostLock.lock();
                //only mark and fire the event if connection is ok, avoid firing the event more than once.
                // this code block should be as fast as possible.
                if (isConnectionOK()) {
                    Log.log(LOGGER::warn, "marking connection lost");
                    myConnectionLostFlag.set(true);

                    //must notify BackendConnectionMonitor immediately and not on background thread, the main reason is
                    // that on startup it must be notified immediately before starting to create UI components
                    // it will also catch the connection lost event
                    BackendConnectionMonitor.getInstance(project).connectionLost();
                    ActivityMonitor.getInstance(project).registerConnectionLost();

                    //wait half a second because maybe the connection lost is momentary, and it will be back
                    // very soon
                    myConnectionStatusNotifyAlarm.cancelAllRequests();
                    myConnectionStatusNotifyAlarm
                            .addRequest(() -> {
                                Log.log(LOGGER::warn, "notifying connectionLost");
                                project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionLost();
                            }, 500);
                }
            } finally {
                if (myConnectionLostLock.isHeldByCurrentThread()) {
                    myConnectionLostLock.unlock();
                }
            }
        }


        //Exceptions that may indicate that connection can't be established


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


    private static class ErrorReportingHelper {

        private final Set<String> errors = new HashSet<>();

        public void reset() {
            errors.clear();
        }


        public boolean addIfNewError(Exception e) {
            var cause = findRealError(e);
            var errorName = cause.getClass().getName();
            return errors.add(errorName);
        }

        @NotNull
        private Throwable findRealError(Exception e) {

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
