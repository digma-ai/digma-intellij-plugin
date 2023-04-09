package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import kotlin.Pair;
import kotlin.Triple;
import org.apache.commons.lang3.StringUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.analytics.JaegerUrlChanged;
import org.digma.intellij.plugin.common.AsynchronousBackgroundTask;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.common.CommonUtils;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanForTracePayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityEntrySpanPayload;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToSpanRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityGoToTraceRequest;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResponseEntry;
import org.digma.intellij.plugin.model.rest.recentactivity.RecentActivityResult;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService;
import org.digma.intellij.plugin.service.EditorService;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedPayload;
import org.digma.intellij.plugin.toolwindow.common.JaegerUrlChangedRequest;
import org.digma.intellij.plugin.toolwindow.common.ThemeChangeListener;
import org.digma.intellij.plugin.ui.model.environment.EnvironmentsSupplier;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_SET_IS_JAEGER_ENABLED;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GET_DATA;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_SPAN;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_GO_TO_TRACE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_SET_DATA;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.REQUEST_MESSAGE_TYPE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.parseJsonToObject;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.LOCAL_ENV;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.SUFFIX_OF_LOCAL;
import static org.digma.intellij.plugin.ui.common.EnvironmentUtilKt.getSortedEnvironments;
import static org.digma.intellij.plugin.ui.list.insights.JaegerUtilKt.openJaegerFromRecentActivity;


/**
 * Digma tool window inside bottom panel
 */
public class DigmaBottomToolWindowFactory implements ToolWindowFactory {
    private static final Logger LOGGER = Logger.getInstance(DigmaBottomToolWindowFactory.class);
    private static final String DIGMA_SIDE_PANE_TOOL_WINDOW_NAME = "Digma";
    private EditorService editorService;
    private AnalyticsService analyticsService;
    private String localHostname;

    private final ReentrantLock recentActivityGetDataLock = new ReentrantLock();

    /**
     * this is the starting point of the plugin. this method is called when the tool window is opened.
     * before the window is opened there may be no reason to do anything, listen to events for example will be
     * a waste if the user didn't open the window. at least as much as possible, some extensions will be registered
     * but will do nothing if the plugin is not active.
     * after the plugin is active all listeners and extensions are installed and kicking until the IDE is closed.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);
        this.editorService = project.getService(EditorService.class);
        //initialize AnalyticsService early so the UI already can detect the connection status when created
        this.analyticsService = project.getService(AnalyticsService.class);

        this.localHostname = CommonUtils.getLocalHostname();

        var contentFactory = ContentFactory.getInstance();

        Content codeAnalyticsTab = createCodeAnalyticsTab(project, toolWindow, contentFactory);

        if (codeAnalyticsTab != null) {
            toolWindow.getContentManager().setSelectedContent(codeAnalyticsTab, true);
        }

        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(analyticsService.getEnvironment().getCurrent());
            });
        }

    }

    private Content createCodeAnalyticsTab(Project project, ToolWindow toolWindow, ContentFactory contentFactory) {
        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            return null;
        }
        var customViewerWindow = project.getService(RecentActivityCustomViewerWindowService.class).getCustomViewerWindow();
        JBCefBrowser jbCefBrowser = customViewerWindow.getWebView();

        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        CefMessageRouter msgRouter = CefMessageRouter.create();

        ThemeChangeListener listener = new ThemeChangeListener(jbCefBrowser);
        UIManager.addPropertyChangeListener(listener);

        project.getMessageBus().connect().subscribe(
                JaegerUrlChanged.JAEGER_URL_CHANGED_TOPIC,
                (JaegerUrlChanged) newJaegerUrl -> Backgroundable.ensureBackground(project, "Jaeger Url was changed", () ->
                        sendRequestToChangeTraceButtonDisplaying(jbCefBrowser, newJaegerUrl))
        );

        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                Log.log(LOGGER::debug, "request: {}", request);
                JcefMessageRequest reactMessageRequest = parseJsonToObject(request, JcefMessageRequest.class);
                if (RECENT_ACTIVITY_GET_DATA.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    new AsynchronousBackgroundTask(recentActivityGetDataLock,
                            () -> processRecentActivityGetDataRequest(analyticsService, jbCefBrowser)).queue();
                }
                if (RECENT_ACTIVITY_GO_TO_SPAN.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToSpanRequest recentActivityGoToSpanRequest = parseJsonToObject(request, RecentActivityGoToSpanRequest.class);
                    processRecentActivityGoToSpanRequest(recentActivityGoToSpanRequest.getPayload(), project);
                }
                if (RECENT_ACTIVITY_GO_TO_TRACE.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    RecentActivityGoToTraceRequest recentActivityGoToTraceRequest = parseJsonToObject(request, RecentActivityGoToTraceRequest.class);
                    processRecentActivityGoToTraceRequest(recentActivityGoToTraceRequest, project);
                }

                callback.success("");
                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                super.onQueryCanceled(browser, frame, queryId);
            }
        }, true);

        jbCefClient.getCefClient().addMessageRouter(msgRouter);

        JPanel browserPanel = new JPanel();
        browserPanel.setLayout(new BorderLayout());
        browserPanel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);


        JPanel jcefDigmaPanel = new JPanel();
        jcefDigmaPanel.setLayout(new BorderLayout());
        jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER);

        var jcefContent = contentFactory.createContent(jcefDigmaPanel, null, false);

        toolWindow.getContentManager().addContent(jcefContent);
        return jcefContent;
    }

    private void processRecentActivityGoToSpanRequest(RecentActivityEntrySpanPayload payload, Project project) {
        if (payload != null) {
            String methodCodeObjectId = payload.getSpan().getMethodCodeObjectId();

            ApplicationManager.getApplication().invokeLater(() -> {
                LanguageService languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);
                Map<String, Pair<String, Integer>> workspaceUrisForMethodCodeObjectIds = languageService.findWorkspaceUrisForMethodCodeObjectIds(Collections.singletonList(methodCodeObjectId));
                final Pair<String, Integer> fileAndOffset = workspaceUrisForMethodCodeObjectIds.get(methodCodeObjectId);
                if (fileAndOffset != null) {
                    // modifying the selected environment
                    EnvironmentsSupplier environmentsSupplier = analyticsService.getEnvironment();
                    String actualEnvName = adjustBackEnvNameIfNeeded(payload.getEnvironment());
                    environmentsSupplier.setCurrent(actualEnvName);

                    Triple<VirtualFile, Editor, Boolean> openedFileAndEditor = editorService.openWorkspaceFileInEditor(fileAndOffset.getFirst(), fileAndOffset.getSecond());

                    if (openedFileAndEditor != null) {
                        boolean fileWasAlreadyOpen = openedFileAndEditor.component3();
                        if (fileWasAlreadyOpen) {
                            // if file already opened then refresh for faster insight getting (issue 474)
                            RefreshService refreshService = RefreshService.getInstance(project);
                            refreshService.refreshAllInBackground();
                        }
                    }

                    ToolWindow digmaSidePaneToolWindow = ToolWindowManager.getInstance(project).getToolWindow(DIGMA_SIDE_PANE_TOOL_WINDOW_NAME);
                    if (digmaSidePaneToolWindow != null && !digmaSidePaneToolWindow.isVisible()) {
                        digmaSidePaneToolWindow.show();
                    } else {
                        Log.log(LOGGER::debug, "digmaSidePaneToolWindow is empty OR is visible already");
                    }
                }
            });
        }
    }

    private void processRecentActivityGoToTraceRequest(RecentActivityGoToTraceRequest recentActivityGoToTraceRequest, Project project) {
        RecentActivityEntrySpanForTracePayload payload = recentActivityGoToTraceRequest.getPayload();
        if (payload != null) {
            openJaegerFromRecentActivity(project, payload.getTraceId(), payload.getSpan().getScopeId());
        } else {
            Log.log(LOGGER::debug, "processRecentActivityGoToTraceRequest payload is empty");
        }
    }

    private void processRecentActivityGetDataRequest(AnalyticsService analyticsService, JBCefBrowser jbCefBrowser) {
        List<String> allEnvironments = analyticsService.getEnvironments();
        if (allEnvironments == null) {
            Log.log(LOGGER::warn, "error while getting environments from server");
            return;
        }
        RecentActivityResult recentActivityData = null;
        try {
            recentActivityData = analyticsService.getRecentActivity(allEnvironments);
        } catch (AnalyticsServiceException e) {
            Log.log(LOGGER::warn, "AnalyticsServiceException for getRecentActivity: {}", e.getMessage());
        }

        if (recentActivityData != null) {
            List<String> sortedEnvironments = getSortedEnvironments(allEnvironments, localHostname);
            String requestMessage = JBCefBrowserUtil.resultToString(new JcefMessageRequest(
                    REQUEST_MESSAGE_TYPE,
                    RECENT_ACTIVITY_SET_DATA,
                    new JcefMessagePayload(
                            sortedEnvironments,
                            getEntriesWithAdjustedLocalEnvs(recentActivityData)
                    )
            ));

            JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
        }
    }

    private List<RecentActivityResponseEntry> getEntriesWithAdjustedLocalEnvs(RecentActivityResult recentActivityData) {
        return recentActivityData != null ? recentActivityData.getEntries().stream()
                .map(f -> new RecentActivityResponseEntry(
                        getAdjustedEnvName(f.getEnvironment()),
                        f.getTraceFlowDisplayName(),
                        f.getFirstEntrySpan(),
                        f.getLastEntrySpan(),
                        f.getLatestTraceId(),
                        f.getLatestTraceTimestamp(),
                        f.getLatestTraceDuration(),
                        f.getSlimAggregatedInsights())
                )
                .toList() : new ArrayList<>();
    }

    private String getAdjustedEnvName(String environment) {
        return environment.toUpperCase().endsWith(SUFFIX_OF_LOCAL) ? LOCAL_ENV : environment;
    }

    private String adjustBackEnvNameIfNeeded(String environment) {
        if (environment.equalsIgnoreCase(LOCAL_ENV)) {
            return (localHostname + SUFFIX_OF_LOCAL).toUpperCase();
        }
        return environment;
    }

    private void sendRequestToChangeTraceButtonDisplaying(JBCefBrowser jbCefBrowser, String newJaegerUrl) {
        String requestMessage = JBCefBrowserUtil.resultToString(
                new JaegerUrlChangedRequest(
                        REQUEST_MESSAGE_TYPE,
                        GLOBAL_SET_IS_JAEGER_ENABLED,
                        new JaegerUrlChangedPayload(StringUtils.isNotEmpty(newJaegerUrl) && StringUtils.isNotBlank(newJaegerUrl))
                ));
        JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
    }

}
