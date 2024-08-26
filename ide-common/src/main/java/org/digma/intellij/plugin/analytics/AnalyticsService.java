package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.auth.AuthManager;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.AboutResult;
import org.digma.intellij.plugin.model.rest.activation.DiscoveredDataResponse;
import org.digma.intellij.plugin.model.rest.assets.AssetDisplayInfo;
import org.digma.intellij.plugin.model.rest.codelens.*;
import org.digma.intellij.plugin.model.rest.codespans.CodeContextSpans;
import org.digma.intellij.plugin.model.rest.common.SpanHistogramQuery;
import org.digma.intellij.plugin.model.rest.debugger.DebuggerEventRequest;
import org.digma.intellij.plugin.model.rest.env.*;
import org.digma.intellij.plugin.model.rest.environment.Env;
import org.digma.intellij.plugin.model.rest.event.*;
import org.digma.intellij.plugin.model.rest.highlights.HighlightsRequest;
import org.digma.intellij.plugin.model.rest.insights.*;
import org.digma.intellij.plugin.model.rest.insights.issues.GetIssuesRequestPayload;
import org.digma.intellij.plugin.model.rest.livedata.*;
import org.digma.intellij.plugin.model.rest.lowlevel.*;
import org.digma.intellij.plugin.model.rest.navigation.*;
import org.digma.intellij.plugin.model.rest.recentactivity.*;
import org.digma.intellij.plugin.model.rest.tests.*;
import org.digma.intellij.plugin.model.rest.user.*;
import org.digma.intellij.plugin.model.rest.version.*;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.ui.*;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static org.digma.intellij.plugin.analytics.EnvUtilsKt.getAllEnvironmentsIds;
import static org.digma.intellij.plugin.analytics.EnvironmentRefreshSchedulerKt.scheduleEnvironmentRefresh;
import static org.digma.intellij.plugin.common.JsonUtilsKt.objectToJsonNoException;
import static org.digma.intellij.plugin.common.StringUtilsKt.argsToString;
import static org.digma.intellij.plugin.log.Log.API_LOGGER_NAME;


public class AnalyticsService implements Disposable {

    public static final String ENVIRONMENT_QUERY_PARAM_NAME = "environment";

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private Environment environment;

    private final Project project;

    private AnalyticsProvider analyticsProviderProxy;

    public AnalyticsService(@NotNull Project project) {
        //make sure its created early, sometimes we get InterruptedException if two threads call getService at the same time.
        ApiPerformanceMonitor.getInstance(project);
        //initialize BackendConnectionMonitor when starting, so it is aware early on connection statuses
        BackendConnectionMonitor.getInstance(project);
        //initialize MainToolWindowCardsController and RecentActivityToolWindowCardsController when starting,
        // so they are aware early on connection statuses.
        RecentActivityToolWindowCardsController.getInstance(project);
        MainToolWindowCardsController.getInstance(project);
        environment = new Environment(project, this);
        this.project = project;
        createClient();
        scheduleEnvironmentRefresh(this, environment);
    }


    @NotNull
    public static AnalyticsService getInstance(@NotNull Project project) {
        return project.getService(AnalyticsService.class);
    }

    public Environment getEnvironment() {
        return environment;
    }


    //todo: not sure synchronized is necessary anymore, it is called from the constructor and intellij component manager makes sure to create singletons
    synchronized private void createClient() {

        var baseUrlProvider = AnalyticsUrlProvider.getInstance();

        Log.log(LOGGER::info, "creating AnalyticsProvider for url {}", baseUrlProvider.baseUrl());

        var logger = new Consumer<String>() {
            @Override
            public void accept(String message) {
                var apiLogger = Logger.getInstance(API_LOGGER_NAME);
                Log.log(apiLogger::debug, "API: {}", message);
            }
        };

        var restAnalyticsProvider = new RestAnalyticsProvider(AuthManager.getInstance().getAuthenticationProviders(), logger, baseUrlProvider, 2);

        Log.log(LOGGER::debug, "calling AuthManager.withAuth for url {}", baseUrlProvider.baseUrl());
        AnalyticsProvider analyticsProvider = AuthManager.getInstance().withAuth(project, restAnalyticsProvider);
        Log.log(LOGGER::debug, "AuthManager.withAuth successfully wrapped AnalyticsProvider for url {}", baseUrlProvider.baseUrl());
        analyticsProviderProxy = newAnalyticsProviderProxy(analyticsProvider);

        tryRegisterServerVersionEarly();
        environment.refreshNowOnBackground();

    }

    //usually BackendInfoHolder registers the server version,but it depends on AnalyticsService.
    // it may happen that getEnvironments will fail and register error but there will be no server version yet
    // in ActivityMonitor.
    // this is an attempt to register server version as early as possible before any errors occurs.
    // BackendInfoHolder will continue monitoring the server info for changes.
    private void tryRegisterServerVersionEarly() {
        Backgroundable.executeOnPooledThread(() -> {
            try {
                var about = getAbout();
                if (about != null) {
                    ActivityMonitor.getInstance(project).registerServerInfo(about);
                }
            } catch (Throwable e) {
                Log.debugWithException(LOGGER, project, e, "getAbout failed");
            }
        });
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
                ErrorReporter.getInstance().reportError(project, "AnalyticsService.getEnvironments", e);
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
        List<String> environments = getAllEnvironmentsIds(project);
        return executeCatching(() -> analyticsProviderProxy.getLatestEvents(new LatestCodeObjectEventsRequest(environments, lastReceivedTime)));
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


    public LinkUnlinkTicketResponse linkTicket(String insightId, String ticketLink) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        var linkRequest = new LinkTicketRequest(env, insightId, ticketLink);
        return executeCatching(() -> analyticsProviderProxy.linkTicket(linkRequest));
    }

    public LinkUnlinkTicketResponse unlinkTicket(String insightId) throws AnalyticsServiceException {
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


    public String getErrors(List<String> codeObjectIds) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        Log.log(LOGGER::trace, "Requesting insights for next codeObjectId {} and next environment {}", codeObjectIds, env);
        var errors = executeCatching(() -> analyticsProviderProxy.getErrors(env, codeObjectIds));
        if (errors == null) {
            errors = "[]";
        }
        return errors;
    }

    public String getErrorDetails(String errorUid) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getErrorDetails(errorUid));
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

    public String getServices(String environment) throws AnalyticsServiceException {
        return executeCatching(() ->
                analyticsProviderProxy.getServices(environment));
    }

    public String getAssetsReportStats(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() ->
                analyticsProviderProxy.getAssetsReportStats(queryParams));
    }

    public String getIssuesReportStats(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        return executeCatching(() ->
                analyticsProviderProxy.getIssuesReportStats(queryParams));
    }

    public void resetThrottlingStatus() throws AnalyticsServiceException {
        executeCatching(() -> {
            analyticsProviderProxy.resetThrottlingStatus();
            return null;
        });
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

    public String getIssues(GetIssuesRequestPayload requestPayload) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        requestPayload.setEnvironment(env);
        return executeCatching(() -> analyticsProviderProxy.getIssues(requestPayload));
    }

    public String getIssuesFilters(@NotNull Map<String, Object> queryParams) throws AnalyticsServiceException {
        var env = getCurrentEnvironmentId();
        queryParams.put(ENVIRONMENT_QUERY_PARAM_NAME, env);
        return executeCatching(() -> analyticsProviderProxy.getIssuesFilters(queryParams));
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
    public InsightsStatsResult getInsightsStats(String spanCodeObjectId, String insightTypes, String services) {
        try {
            var envId = getCurrentEnvironmentId();
            var params = new HashMap<String, Object>();
            params.put("Environment", envId);

            if (spanCodeObjectId != null) {
                params.put("ScopedSpanCodeObjectId", spanCodeObjectId);
            }

            if (insightTypes != null && !insightTypes.isEmpty()) {
                params.put("insights", insightTypes);
            }

            if (services != null && !services.isEmpty()) {
                params.put("services", services);
            }

            return executeCatching(() -> analyticsProviderProxy.getInsightsStats(params));
        } catch (Exception e) {
            Log.debugWithException(LOGGER, project, e, "error calling  insights stats", e.getMessage());
        }
        return new InsightsStatsResult(0, 0, 0, 0, 0, 0);
    }

    @NotNull
    public List<SpanEnvironment> getSpanEnvironmentsStats(String spanCodeObjectId) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getSpanEnvironmentsStats(spanCodeObjectId));
    }

    @NotNull
    public DiscoveredDataResponse getDiscoveredData() throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.getDiscoveredData());
    }

    public HttpResponse lowLevelCall(HttpRequest request) throws AnalyticsServiceException {
        return executeCatching(() -> analyticsProviderProxy.lowLevelCall(request));
    }

    @Override
    public void dispose() {
        try {
            analyticsProviderProxy.close();
            analyticsProviderProxy = null;
            environment = null;
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
    /*
    Note: this method should throw only AnalyticsServiceException.
     the only case it will throw something else is on EDTAccessException or ReadAccessException which are severe errors
     that should turn a red error to user, and we must catch those during development or testing before release.
    */
    private <T> T executeCatching(Supplier<T> tSupplier) throws AnalyticsServiceException {
        try {
            return tSupplier.get();

            //Exception handling:
            //The AnalyticsProvider interface does not declare any checked exceptions in any method, it always throws a AnalyticsProviderException
            // which is an unchecked exception.
            //our proxy catches InvocationTargetException and throws it, this exception will wrap the real AnalyticsProviderException, actually
            // it will wrap UndeclaredThrowableException from the AuthProxy.
            //thr proxy will always throw an UndeclaredThrowableException wrapping the InvocationTargetException.
            //we should try to find the real AnalyticsProviderException,wrap it with AnalyticsServiceException and throw.
            //the proxy
        } catch (UndeclaredThrowableException undeclaredThrowableException) {
            //we have two proxies, the AnalyticsService proxy and AuthManager proxy, so this could be an UndeclaredThrowableException
            // wrapping InvocationTargetException wrapping UndeclaredThrowableException wrapping InvocationTargetException.
            //there must be an AnalyticsProviderException as cause because all methods should go through the proxies.
            var analyticsProviderException = ExceptionUtils.findAssignableCause(AnalyticsProviderException.class, undeclaredThrowableException);
            if (analyticsProviderException != null) {
                throw new AnalyticsServiceException(analyticsProviderException);
            } else {
                Throwable cause = ExceptionUtils.findFirstNonWrapperException(undeclaredThrowableException);
                throw new AnalyticsServiceException(Objects.requireNonNullElse(cause, undeclaredThrowableException));
            }
        } catch (EDT.EDTAccessException | ReadActions.ReadAccessException e) {
            //these are exceptions we throw intentionally if calling the API on EDT or in ReadAccess.
            //these may break the flow of the application because no one catches them.
            //before they are thrown an error is logged which should pop up a red error icon to the user.
            //we must catch them during development.
            throw e;
        } catch (Throwable e) {
            throw new AnalyticsServiceException(e);
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

        private final Set<String> methodsThatShouldNotChangeConnectionStatus = Set.of(new String[]{"getPerformanceMetrics", "getAbout", "getVersions"});

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
            Throwable exception = null;

            try {

                //should never call backend on EDT or in read access because it will cause a freeze.
                // these must be caught during development
                EDT.assertNonDispatchThread();
                ReadActions.assertNotInReadAccess();


                if (LOGGER.isTraceEnabled()) {
                    Log.log(LOGGER::trace, "Sending request to {}: args '{}'", method.getName(), argsToString(args));
                }

                Object result = method.invoke(analyticsProvider, args);

                if (LOGGER.isTraceEnabled()) {
                    Log.log(LOGGER::trace, "Got response from {}: args '{}', -----------------" +
                            "Result '{}'", method.getName(), argsToString(args), objectToJsonNoException(result));
                }


                if (!PersistenceService.getInstance().isFirstTimeConnectionEstablished()) {
                    //if this block is not synchronized the event may be sent more than once.
                    //synchronization only happens on first time because of double check
                    synchronized (this) {
                        if (!PersistenceService.getInstance().isFirstTimeConnectionEstablished()) {
                            ActivityMonitor.getInstance(project).registerFirstConnectionEstablished();
                            PersistenceService.getInstance().setFirstTimeConnectionEstablished();
                        }
                    }
                }

                PersistenceService.getInstance().updateLastConnectionTimestamp();

                ApiErrorHandler.getInstance().resetConnectionLostAndNotifyIfNecessary(project);

                return result;

            } catch (InvocationTargetException e) {

                //this message should help us understand the logs when debugging issues. it will help
                // understand that there are more and more exceptions.
                //code below and in ApiErrorHandler.handleInvocationTargetException will log the exceptions but our Log class
                // will not explode the logs, so we don't see all the exceptions in the log as they happen.
                //this message will explode the idea.log if user has digma trace logging on and no backend running,
                // which shouldn't happen, users should not have digma trace logging on all the time.
                var rootCause = ExceptionUtils.findRootCausePreferConnectionException(e);
                exception = rootCause;
                Log.log(LOGGER::trace, "got exception in AnalyticsService {}", rootCause);


                //for these methods we rethrow the exception without effecting the connection status.
                //prefer connection exception as root cause because if the code checks isConnectionException it should be true,
                // if we put the real root-cause it may be an exception that is not considered connection exception, for example
                // ssl exception may wrap EOFException and EOFException alone is not considered connection exception.
                if (methodsThatShouldNotChangeConnectionStatus.contains(method.getName())) {
                    Log.warnWithException(LOGGER, e, "error in method {}", method.getName());
                    throw new AnalyticsServiceException(rootCause);
                }


                //handle only InvocationTargetException, other exceptions are probably a bug.
                ApiErrorHandler.getInstance().handleInvocationTargetException(project, e, method, args);
                throw e;

            } catch (Exception e) {
                exception = e;
                Log.warnWithException(LOGGER, project, e, "Error invoking AnalyticsProvider.{}({}), exception {}", method.getName(), argsToString(args), e);
                ErrorReporter.getInstance().reportAnalyticsServiceError(project, "AnalyticsInvocationHandler.invoke", method.getName(), e, false);
                throw e;
            } finally {
                stopWatch.stop();
                if (BackendConnectionMonitor.getInstance(project).isConnectionOk()) {
                    //httpNetoTime may be null, usually it will not but the ThreadLocal is nullable by default so need to check
                    var httpNetoTime = RestAnalyticsProvider.PERFORMANCE.get();
                    if (httpNetoTime != null) {
                        ApiPerformanceMonitor.getInstance(project).addPerformance(method.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS), httpNetoTime, exception);
                    }
                }
                if (LOGGER.isTraceEnabled()) {
                    Log.log(LOGGER::trace, "Api call {} took {} milliseconds", method.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
                }
            }
        }
    }

}
