package org.digma.intellij.plugin.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.Alarm;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.auth.AuthManager;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans;
import org.digma.intellij.plugin.model.rest.common.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.highlights.HighlightsRequest;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.lowlevel.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.notifications.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.tests.*;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;
import org.digma.intellij.plugin.notifications.NotificationUtil;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.settings.SettingsState;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.digma.intellij.plugin.analytics.EnvUtilsKt.getAllEnvironmentsNames;
import static org.digma.intellij.plugin.analytics.EnvironmentRefreshSchedulerKt.scheduleEnvironmentRefresh;
import static org.digma.intellij.plugin.common.ExceptionUtils.*;
import static org.digma.intellij.plugin.log.Log.API_LOGGER_NAME;


public class AnalyticsService implements Disposable {

    public static final String ENVIRONMENT_QUERY_PARAM_NAME = "environment";

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private final Environment environment;

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
        environment = new Environment(project, this);
        this.project = project;
        replaceClient(SettingsState.getInstance().apiUrl);
        scheduleEnvironmentRefresh(this, environment);
    }


    public static AnalyticsService getInstance(@NotNull Project project) {
        return project.getService(AnalyticsService.class);
    }

    public Environment getEnvironment() {
        return environment;
    }


    //just replace the client and do not fire any events
    //this method should be synchronized, and it shouldn't be a problem, it doesn't happen too often.
    //package private, should not be called other than from AnalyticsServiceSettingsWatcher
    synchronized void replaceClient(String url) {

        Log.log(LOGGER::debug, "replacing AnalyticsProvider for url {}", url);

        if (analyticsProviderProxy != null) {
            try {
                analyticsProviderProxy.close();
            } catch (IOException e) {
                Log.log(LOGGER::warn, e.getMessage());
            }
        }

        Log.log(LOGGER::debug, "calling AuthManager.withAuth for url {}", url);
        AnalyticsProvider analyticsProvider =
                AuthManager.getInstance().withAuth(new RestAnalyticsProvider(url, AuthManager.getInstance().getAuthenticationProviders(),
                        message -> {
                            var apiLogger = Logger.getInstance(API_LOGGER_NAME);
                            Log.log(apiLogger::debug, "API: {}", message);
                        }));
        Log.log(LOGGER::debug, "AuthManager.withAuth successfully wrapped AnalyticsProvider for url {}", url);
        analyticsProviderProxy = newAnalyticsProviderProxy(analyticsProvider);

        environment.refreshNowOnBackground();

        project.getMessageBus().syncPublisher(ApiClientChangedEvent.getAPI_CLIENT_CHANGED_TOPIC()).apiClientChanged(url);

    }


    @NotNull
    public ConnectionTestResult testRemoteConnection(@NotNull String serverUrl, @Nullable String token) {
        try (RestAnalyticsProvider analyticsProvider = new RestAnalyticsProvider(serverUrl, Collections.singletonList(new AuthenticationProvider() {
            @Nullable
            @Override
            public String getHeaderName() {
                return "Authorization";
            }

            @Nullable
            @Override
            public String getHeaderValue() {
                return token;
            }
        }), message -> Log.log(LOGGER::debug, message))) {
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


    public List<Env> getEnvironments() {
        try {
            var envs = executeCatching(() -> analyticsProviderProxy.getEnvironments());
            //warn about duplicates
            if (new HashSet<>(envs).size() < envs.size()) {
                var details = new HashMap<String, String>();
                details.put("error", "duplicate environments in getEnvironments");
                details.put("environments", String.join(",", envs.stream().map(env -> env.getId() + ":" + env.getName()).toList()));
                ErrorReporter.getInstance().reportError(project, "AnalyticsService.getEnvironments", "get environments", details);
            }
            return envs;
        } catch (AnalyticsServiceException e) {
            if (!ExceptionUtils.isAnyConnectionException(e)) {
                ErrorReporter.getInstance().reportError("AnalyticsService.getEnvironments", e);
            }
            return Collections.emptyList();
        }
    }


    private String getCurrentEnvironmentId() throws AnalyticsServiceException {
        var envId = EnvUtilsKt.getCurrentEnvironmentId(project);
        if (envId == null) {
            throw new NoSelectedEnvironmentException("No selected environment");
        }
        return envId;
    }


    public void sendDebuggerEvent(int eventType, String timestamp) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.sendDebuggerEvent(new DebuggerEventRequest(String.valueOf(eventType), CommonUtils.getLocalHostname(), timestamp));
            return null;
        });
    }


    public LatestCodeObjectEventsResponse getLatestEvents(@NotNull String lastReceivedTime) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getLatestEvents(new LatestCodeObjectEventsRequest(getAllEnvironmentsNames(project), lastReceivedTime)));
    }


    public List<InsightTypesForJaegerResponse> getInsightsForJaeger(List<String> spanCodeObjectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() -> analyticsProviderProxy.getInsightsForJaeger(new InsightTypesForJaegerRequest(env, spanCodeObjectIds)));
    }


    @NotNull
    public CodeContextSpans getSpansForCodeLocation(@NotNull List<String> idsWithType) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        Log.log(LOGGER::debug, "Requesting spans for code objects {}", idsWithType);
        return executeCatching(() -> analyticsProviderProxy.getSpansForCodeLocation(env, idsWithType));
    }


    public String getInsightBySpan(String spanCodeObjectId, String insightType) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        Log.log(LOGGER::debug, "Requesting insight for span {}", spanCodeObjectId);
        return executeCatching(() -> analyticsProviderProxy.getInsightBySpan(env, spanCodeObjectId, insightType));

    }


    public LinkUnlinkTicketResponse linkTicket(String insightId, String ticketLink) throws AnalyticsServiceException{
        var env = getCurrentEnvironmentId();
        var linkRequest = new LinkTicketRequest(env, insightId, ticketLink);
        return executeCatching(() -> analyticsProviderProxy.linkTicket(linkRequest));
    }

    public LinkUnlinkTicketResponse unlinkTicket(String insightId) throws AnalyticsServiceException{
        var env = getCurrentEnvironmentId();
        var unlinkRequest = new UnlinkTicketRequest(env, insightId);
        return executeCatching(() -> analyticsProviderProxy.unlinkTicket(unlinkRequest));
    }

    public CodeLensOfMethodsResponse getCodeLensByMethods(List<MethodWithCodeObjects> methods) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        var request = new CodeLensOfMethodsRequest(env, methods);
        return executeCatching(() -> analyticsProviderProxy.getCodeLensByMethods(request));
    }


    public AssetDisplayInfo getAssetDisplayInfo(String codeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() -> analyticsProviderProxy.getAssetDisplayInfo(env, codeObjectId));
    }


    public List<CodeObjectError> getErrorsOfCodeObject(List<String> codeObjectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        Log.log(LOGGER::trace, "Requesting insights for next codeObjectId {} and next environment {}", codeObjectIds, env);
        var errors = executeCatching(() -> analyticsProviderProxy.getErrorsOfCodeObject(env, codeObjectIds));
        if (errors == null) {
            errors = Collections.emptyList();
        }
        return errors;
    }


    public void setInsightCustomStartTime(String insightId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        String formattedActualDate = Instant.now().toString();//FYI: by UTC time zone
        executeCatching(() -> {
            analyticsProviderProxy.setInsightCustomStartTime(
                    new CustomStartTimeInsightRequest(
                            env,
                            insightId,
                            formattedActualDate
                    ));
            return null;
        });
    }

    public CodeObjectErrorDetails getErrorDetails(String errorUid) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getCodeObjectErrorDetails(errorUid));
    }


    public VersionResponse getVersions(VersionRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getVersions(request));
    }

    public RecentActivityResult getRecentActivity(List<String> environmentsIds) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getRecentActivity(new RecentActivityRequest(environmentsIds)));
    }

    public UserUsageStatsResponse getUserUsageStats() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getUserUsageStats(new UserUsageStatsRequest()));
    }

    public DurationLiveData getDurationLiveData(String codeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() ->
                analyticsProviderProxy.getDurationLiveData(new DurationLiveDataRequest(env, codeObjectId)));
    }

    public CodeObjectNavigation getCodeObjectNavigation(String spanCodeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() ->
                analyticsProviderProxy.getCodeObjectNavigation(new CodeObjectNavigationRequest(env, spanCodeObjectId)));
    }


    public String getHtmlGraphForSpanPercentiles(String spanCodeObjectId, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironmentId(), spanCodeObjectId, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanPercentiles(spanHistogramQuery));
    }

    public String getHtmlGraphForSpanScaling(String spanCodeObjectId, String backgroundColor) throws AnalyticsServiceException {
        final SpanHistogramQuery spanHistogramQuery = new SpanHistogramQuery(getCurrentEnvironmentId(), spanCodeObjectId, JBColor.isBright() ? "light" : "dark", backgroundColor);
        return executeCatching(() -> analyticsProviderProxy.getHtmlGraphForSpanScaling(spanHistogramQuery));
    }

    public String getAssetCategories(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        queryParams.put(ENVIRONMENT_QUERY_PARAM_NAME, env);
        return executeCatching(() ->
                analyticsProviderProxy.getAssetCategories(queryParams));
    }


    public String getInsightsExist() throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() -> analyticsProviderProxy.insightExists(env));
    }


    public String getAssetFilters(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        queryParams.put(ENVIRONMENT_QUERY_PARAM_NAME, env);
        return executeCatching(() ->
                analyticsProviderProxy.getAssetFilters(queryParams));
    }

    public String getAssets(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        queryParams.put(ENVIRONMENT_QUERY_PARAM_NAME, env);
        return executeCatching(() ->
                analyticsProviderProxy.getAssets(queryParams));
    }

    public String getServices() throws AnalyticsServiceException {

        var env = getCurrentEnvironmentId();
        return executeCatching(() ->
                analyticsProviderProxy.getServices(env));
    }


    public String getNotifications(String notificationsStartDate, String userId, int pageNumber, int pageSize, boolean isRead) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() ->
                analyticsProviderProxy.getNotifications(new NotificationsRequest(env, userId, notificationsStartDate, pageNumber, pageSize, isRead)));
    }

    public void setReadNotificationsTime(String upToDateTime, String userId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        executeCatching(() -> {
            analyticsProviderProxy.setReadNotificationsTime(new SetReadNotificationsRequest(env, userId, upToDateTime));
            return null;
        });
    }

    public void resetThrottlingStatus() throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.resetThrottlingStatus();
            return null;
        });
    }

    public UnreadNotificationsCountResponse getUnreadNotificationsCount(String notificationsStartDate, String userId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() -> analyticsProviderProxy.getUnreadNotificationsCount(new GetUnreadNotificationsCountRequest(env, userId, notificationsStartDate)));
    }


    // return JSON as string (type LatestTestsOfSpanResponse)
    public String getLatestTestsOfSpan(TestsScopeRequest req, FilterForLatestTests filter, int pageSize) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getLatestTestsOfSpan(
                new LatestTestsOfSpanRequest(req.getSpanCodeObjectIds(), req.getMethodCodeObjectId(), req.getEndpointCodeObjectId(),
                        filter.getEnvironments(), filter.getPageNumber(), pageSize)));
    }


    public PerformanceMetricsResponse getPerformanceMetrics() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getPerformanceMetrics());
    }

    public String getHighlightsPerformance(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsPerformance(queryParams));
    }

    public String getHighlightsTopInsights(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsTopInsights(queryParams));
    }

    public String getHighlightsPerformanceV2(HighlightsRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsPerformanceV2(request));
    }

    public String getHighlightsTopInsightsV2(HighlightsRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsTopInsightsV2(request));
    }

    public String getHighlightsScaling(HighlightsRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsScaling(request));
    }

    public String getSpanInfo(String spanCodeObjectId) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getSpanInfo(spanCodeObjectId));
    }

    public String getHighlightsImpact(HighlightsRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getHighlightsImpact(request));
    }

    public Optional<LoadStatusResponse> getLoadStatus() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getLoadStatus());
    }

    public AboutResult getAbout() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getAbout());
    }

    public DeleteEnvironmentResponse deleteEnvironment(@NotNull String environmentName) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.deleteEnvironment(new DeleteEnvironmentRequest(environmentName)));
    }

    public void deleteEnvironmentV2(@NotNull String id) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.deleteEnvironmentV2(id);
            return null;
        });
    }

    public String getDashboard(@NotNull Map<String, String> queryParams) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getDashboard(queryParams));
    }

    public String getInsights(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        queryParams.put(ENVIRONMENT_QUERY_PARAM_NAME, env);
        return executeCatching(() -> analyticsProviderProxy.getInsights(queryParams));
    }

    public void markInsightsAsRead(@NotNull List<String> insightIds) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.markInsightsAsRead(insightIds);
            return null;
        });
    }

    public void markAllInsightsAsRead(MarkInsightsAsReadScope scope) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        executeCatching(() -> {
            analyticsProviderProxy.markAllInsightsAsRead(env, scope);
            return null;
        });
    }

    public void dismissInsight(String insightId) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.dismissInsight(insightId);
            return null;
        });
    }

    public void undismissInsight(String insightId) throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.undismissInsight(insightId);
            return null;
        });
    }

    @NotNull
    public AssetNavigationResponse getAssetNavigation(@NotNull String spanCodeObjectId) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        return executeCatching(() -> analyticsProviderProxy.getAssetNavigation(env, spanCodeObjectId));
    }

    public String createEnvironment(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.createEnvironments(queryParams));
    }

    public String register(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.register(queryParams));
    }

    @NotNull
    public InsightsStatsResult getInsightsStats(String spanCodeObjectId) throws AnalyticsServiceException {

        try {
            var env = getCurrentEnvironmentId();
            var envId = getCurrentEnvironmentId();
            var params = new HashMap<String, Object>();
            params.put("Environment", envId);

            if (spanCodeObjectId != null) {
                params.put("ScopedSpanCodeObjectId", spanCodeObjectId);
            }

            return executeCatching(() -> analyticsProviderProxy.getInsightsStats(params));
        } catch (NoSelectedEnvironmentException e) {
            Log.debugWithException(LOGGER, project, e, "error calling  insights stats", e.getMessage());
        } catch (Exception e) {
            Log.debugWithException(LOGGER, project, e, "error calling  insights stats", e.getMessage());
        }
        return new InsightsStatsResult(0, 0, 0, 0, 0, 0);
    }

    public HttpResponse lowLevelCall(HttpRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.lowLevelCall(request));
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
        } catch (Exception e) {
            Log.warnWithException(LOGGER, project, e, "exception while closing AnalyticsProvider {}", e.getMessage());
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
            throw new AnalyticsServiceException("UndeclaredThrowableException caught " + e.getMessage(), e.getUndeclaredThrowable());
        } catch (RuntimeExceptionWithAttachments e) {
            //this is a platform exception as a result of asserting non UI thread when calling backend API
            throw e;
        } catch (Throwable e) {
            throw new AnalyticsServiceException("Unknown exception " + e.getMessage(), e);
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


        private final Set<String> methodsToIgnoreExceptions = Set.of(new String[]{"getPerformanceMetrics", "getAbout", "getVersions"});

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

                //should never call backend on EDT or in read access because it will cause a freeze.
                // these must be caught during development
                EDT.assertNonDispatchThread();
                ReadActions.assertNotInReadAccess();


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
                if (!PersistenceService.getInstance().isFirstTimeConnectionEstablished()) {
                    ActivityMonitor.getInstance(project).registerFirstConnectionEstablished();
                    PersistenceService.getInstance().setFirstTimeConnectionEstablished();
                }

                PersistenceService.getInstance().updateLastConnectionTimestamp();

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
                    Log.debugWithException(LOGGER, e, "failed executing method {}", method);
                    throw new AnalyticsServiceException(e);
                }

                //Note: when logging LOGGER.error idea will pop up a red message which we don't want, so only report warn messages.

                //handle only InvocationTargetException, other exceptions are probably a bug.
                //log connection exceptions only the first time and show an error notification.
                // while status is in error, following connection exceptions will not be logged, other exceptions
                // will be logged only once.

                //handleInvocationTargetException may rethrow an exception, if it didn't then always throw
                // an AnalyticsServiceException.
                handleInvocationTargetException(e, method, args);
                if (e.getCause() != null) {
                    //this is caught in executeCatching
                    var analyticsProviderException = ExceptionUtils.findCause(AnalyticsProviderException.class, e);
                    throw Objects.requireNonNullElse(analyticsProviderException, e.getCause());
                } else {
                    throw new AnalyticsServiceException(e);
                }

            } catch (Exception e) {
                errorReportingHelper.addIfNewError(e);
                Log.log(LOGGER::warn, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e);
                Log.warnWithException(LOGGER, project, e, "error in analytics service method {},{}", method.getName(), e);
                ErrorReporter.getInstance().reportAnalyticsServiceError(project, "AnalyticsInvocationHandler.invoke", method.getName(), e, false);
                throw e;
            } finally {
                stopWatch.stop();
                Log.log(LOGGER::trace, "Api call {} took {} milliseconds", method.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
            }
        }


        private void handleInvocationTargetException(InvocationTargetException invocationTargetException, Method method, Object[] args) throws Throwable {
            boolean isConnectionException = isAnyConnectionException(invocationTargetException);
            String message;
            if (isConnectionException(invocationTargetException)) {
                message = getConnectExceptionMessage(invocationTargetException);
            } else if (isSslConnectionException(invocationTargetException)) {
                message = getSslExceptionMessage(invocationTargetException);
            } else {
                message = invocationTargetException.getCause() != null ? invocationTargetException.getCause().getMessage() : invocationTargetException.getMessage();
            }

            if (message == null) {
                message = invocationTargetException.toString();
            }


            if (isConnectionOK()) {
                //if more than one thread enter this section the worst that will happen is that we
                // report the error more than once but connectionLost will be fired once because
                // markConnectionLostAndNotify locks, marks and notifies only if connection ok.
                if (isConnectionException) {
                    markConnectionLostAndNotify();
                    errorReportingHelper.addIfNewError(invocationTargetException);
                    Log.warnWithException(LOGGER, project, invocationTargetException, "Connection exception: error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
                    NotificationUtil.notifyWarning(project, "<html>Connection error with Digma backend api for method " + method.getName() + ".<br> "
                            + message + ".<br> See logs for details.");
                } else {
                    Log.warnWithException(LOGGER, project, invocationTargetException, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), invocationTargetException.getCause().getMessage());
                    if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                        NotificationUtil.notifyWarning(project, "<html>Error with Digma backend api for method " + method.getName() + ".<br> "
                                + message + ".<br> See logs for details.");
                        if (isEOFException(invocationTargetException)) {
                            NotificationUtil.showBalloonWarning(project, "Digma API EOF error: " + message);
                        }
                    }
                }
            } else if (errorReportingHelper.addIfNewError(invocationTargetException)) {
                Log.warnWithException(LOGGER, project, invocationTargetException, "New Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), message);
            }

            ErrorReporter.getInstance().reportAnalyticsServiceError(project, "AnalyticsInvocationHandler.invoke." + message, method.getName(), invocationTargetException, isConnectionException);

        }


        private boolean isConnectionOK() {
            return !myConnectionLostFlag.get();
        }


        private void resetConnectionLostAndNotifyIfNecessary() {

            Log.log(LOGGER::trace, "resetConnectionLostAndNotifyIfNecessary called");

            //this is the critical section of the race condition, there is a performance penalty
            // for the locking, but only when recovering from connection lost, otherwise It's very fast.
            // the reason for locking here and in markConnectionLostAndNotify is to avoid a situation were myConnectionLostFlag
            // if marked but never reset and to make sure that if we notified connectionLost we will also notify when its gained back.
            try {
                //if connection is ok do nothing.
                if (isConnectionOK()) {
                    Log.log(LOGGER::trace, "resetConnectionLostAndNotifyIfNecessary called, connection ok, nothing to do.");
                    return;
                }
                Log.log(LOGGER::info, "acquiring lock to reset connection status after connection lost");
                myConnectionLostLock.lock();
                if (!isConnectionOK()) {
                    Log.log(LOGGER::warn, "resetting connection status after connection lost");
                    myConnectionLostFlag.set(false);
                    errorReportingHelper.reset();
                    myConnectionStatusNotifyAlarm.cancelAllRequests();

                    BackendConnectionMonitor.getInstance(project).connectionGained();
                    ActivityMonitor.getInstance(project).registerConnectionGained();

                    myConnectionStatusNotifyAlarm.addRequest(() -> {
                        Log.log(LOGGER::warn, "notifying connectionGained");
                        project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionGained();
                    }, 1000);


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
                    // it will also catch the connection lost event later
                    BackendConnectionMonitor.getInstance(project).connectionLost();
                    ActivityMonitor.getInstance(project).registerConnectionLost();

                    //wait a second because maybe the connection lost is momentary, and it will be back
                    // very soon
                    myConnectionStatusNotifyAlarm.cancelAllRequests();
                    myConnectionStatusNotifyAlarm
                            .addRequest(() -> {
                                Log.log(LOGGER::warn, "notifying connectionLost");
                                project.getMessageBus().syncPublisher(AnalyticsServiceConnectionEvent.ANALYTICS_SERVICE_CONNECTION_EVENT_TOPIC).connectionLost();
                            }, 2000);
                }
            } finally {
                if (myConnectionLostLock.isHeldByCurrentThread()) {
                    myConnectionLostLock.unlock();
                }
            }
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
