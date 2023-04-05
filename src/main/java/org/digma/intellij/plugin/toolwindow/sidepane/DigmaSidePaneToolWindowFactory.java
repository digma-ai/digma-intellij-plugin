package org.digma.intellij.plugin.toolwindow.sidepane;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.BackendConnectionUtil;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.installationwizard.OpenInBrowserRequest;
import org.digma.intellij.plugin.model.rest.installationwizard.SendTrackingEventRequest;
import org.digma.intellij.plugin.model.rest.installationwizard.SetObservabilityRequest;
import org.digma.intellij.plugin.persistence.PersistenceService;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.service.ErrorsActionsService;
import org.digma.intellij.plugin.toolwindow.recentactivity.ConnectionCheckResult;
import org.digma.intellij.plugin.toolwindow.recentactivity.JBCefBrowserUtil;
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefConnectionCheckMessagePayload;
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefConnectionCheckMessageRequest;
import org.digma.intellij.plugin.toolwindow.recentactivity.JcefMessageRequest;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.digma.intellij.plugin.ui.common.ObservabilityUtil;
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.INSTALLATION_WIZARD_CHECK_CONNECTION;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.INSTALLATION_WIZARD_FINISH;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.INSTALLATION_WIZARD_SEND_TRACKING_EVENT;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.INSTALLATION_WIZARD_SET_CHECK_CONNECTION;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.INSTALLATION_WIZARD_SET_OBSERVABILITY;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.REQUEST_MESSAGE_TYPE;
import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.parseJsonToObject;
import static org.digma.intellij.plugin.ui.common.MainSidePaneWindowPanelKt.createMainSidePaneWindowPanel;


/**
 * The main Digma tool window on left panel
 */
public class DigmaSidePaneToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaSidePaneToolWindowFactory.class);
    private static final String DIGMA_NAME = "DIGMA";

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

        toolWindow.setTitle(DIGMA_NAME);
        //some language service should complete their startup on EDT,especially C# language service
        // needs to initialize its models on EDT.
        // startup may happen here if the tool window is opened on startup, or in EditorEventsHandler.selectionChanged
        // when the first document is opened.
        LanguageService.ensureStartupOnEDTForAll(project);

        var contentFactory = ContentFactory.getInstance();

        //initialize AnalyticsService early so the UI can detect the connection status when created
        project.getService(AnalyticsService.class);

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        BackendConnectionUtil backendConnectionUtil = project.getService(BackendConnectionUtil.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);

        if (PersistenceService.getInstance().getState().getAlreadyPassedTheInstallationWizard()) {
            displayMainSidePaneWindowPanel(project, toolWindow, contentFactory);
        } else {
            displayInstallationWizard(project, toolWindow, contentFactory, backendConnectionUtil);
        }

        //todo: runWhenSmart is ok for java,python , but in Rider runWhenSmart does not guarantee that the solution
        // is fully loaded. consider replacing that with LanguageService.runWhenSmartForAll so that C# language service
        // can run this task when the solution is fully loaded.
        DumbService.getInstance(project).runWhenSmart(() -> initializeWhenSmart(project));
    }

    private void displayMainSidePaneWindowPanel(@NotNull Project project, ToolWindow toolWindow, ContentFactory contentFactory) {
        DigmaResettablePanel mainSidePaneWindowPanel = createMainSidePaneWindowPanel(project);
        Content contentToDisplay = contentFactory.createContent(mainSidePaneWindowPanel, null, false);
        ToolWindowShower.getInstance(project).setToolWindow(toolWindow);
        ToolWindowShower.getInstance(project).setInsightsTab(contentToDisplay);

        toolWindow.getContentManager().addContent(contentToDisplay);
    }

    private void displayInstallationWizard(
            @NotNull Project project,
            ToolWindow toolWindow,
            ContentFactory contentFactory,
            BackendConnectionUtil backendConnectionUtil
    ) {
        Content codeAnalyticsTab = createInstallationWizardTab(project, toolWindow, contentFactory, backendConnectionUtil);
        if (codeAnalyticsTab != null) {
            toolWindow.getContentManager().addContent(codeAnalyticsTab);
        }
    }

    private Content createInstallationWizardTab(
            Project project,
            ToolWindow toolWindow,
            ContentFactory contentFactory,
            BackendConnectionUtil backendConnectionUtil
    ) {
        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            return null;
        }
        var customViewerWindow = project.getService(InstallationWizardCustomViewerWindowService.class).getCustomViewerWindow();
        JBCefBrowser jbCefBrowser = customViewerWindow.getWebView();

        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        CefMessageRouter msgRouter = CefMessageRouter.create();

        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                Log.log(LOGGER::debug, "request: {}", request);
                JcefMessageRequest reactMessageRequest = parseJsonToObject(request, JcefMessageRequest.class);
                if (INSTALLATION_WIZARD_SEND_TRACKING_EVENT.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    SendTrackingEventRequest eventRequest = parseJsonToObject(request, SendTrackingEventRequest.class);
                    if (eventRequest.getPayload() != null) {
                        ActivityMonitor.getInstance(project).registerCustomEvent(eventRequest.getPayload().getEventName());
                    }
                }
                if (INSTALLATION_WIZARD_SET_OBSERVABILITY.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    SetObservabilityRequest observabilityRequest = parseJsonToObject(request, SetObservabilityRequest.class);
                    if (observabilityRequest.getPayload() != null) {
                        ObservabilityUtil.updateObservabilityValue(project, observabilityRequest.getPayload().isObservabilityEnabled());
                    }
                }
                if (INSTALLATION_WIZARD_FINISH.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            changeToolWindowContent(project, toolWindow, contentFactory));
                }
                if (GLOBAL_OPEN_URL_IN_DEFAULT_BROWSER.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    OpenInBrowserRequest openInBrowserRequest = parseJsonToObject(request, OpenInBrowserRequest.class);
                    if (openInBrowserRequest.getPayload() != null) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                BrowserUtil.browse(openInBrowserRequest.getPayload().getUrl()));
                    }
                }
                if (INSTALLATION_WIZARD_CHECK_CONNECTION.equalsIgnoreCase(reactMessageRequest.getAction())) {
                    JcefConnectionCheckMessagePayload jcefConnectionCheckMessagePayload;
                    if (backendConnectionUtil.testConnectionToBackend()) {
                        jcefConnectionCheckMessagePayload = new JcefConnectionCheckMessagePayload(ConnectionCheckResult.SUCCESS.getValue());
                    } else {
                        jcefConnectionCheckMessagePayload = new JcefConnectionCheckMessagePayload(ConnectionCheckResult.FAILURE.getValue());
                    }

                    String requestMessage = JBCefBrowserUtil.resultToString(new JcefConnectionCheckMessageRequest(
                            REQUEST_MESSAGE_TYPE,
                            INSTALLATION_WIZARD_SET_CHECK_CONNECTION,
                            jcefConnectionCheckMessagePayload
                    ));

                    JBCefBrowserUtil.postJSMessage(requestMessage, jbCefBrowser);
                }
                callback.success("");
                return true;
            }
        }, true);

        jbCefClient.getCefClient().addMessageRouter(msgRouter);

        JPanel browserPanel = new JPanel();
        browserPanel.setLayout(new BorderLayout());
        browserPanel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);


        JPanel jcefDigmaPanel = new JPanel();
        jcefDigmaPanel.setLayout(new BorderLayout());
        jcefDigmaPanel.add(browserPanel, BorderLayout.CENTER);

        return contentFactory.createContent(jcefDigmaPanel, null, false);
    }

    private void changeToolWindowContent(@NotNull Project project, ToolWindow toolWindow, ContentFactory contentFactory) {
        // Get the content of the tool window
        Content content = toolWindow.getContentManager().getContent(0);

        // Create a new content with the updated content
        Content newContent = contentFactory.createContent(createMainSidePaneWindowPanel(project), "", false);

        // Replace the old content with the new one
        toolWindow.getContentManager().removeContent(content, true);
        toolWindow.getContentManager().addContent(newContent);

        if (!PersistenceService.getInstance().getState().getAlreadyPassedTheInstallationWizard()) {
            // set global flag that this user has already passed the installation wizard
            PersistenceService.getInstance().getState().setAlreadyPassedTheInstallationWizard(true);
        }
    }


    private void initializeWhenSmart(@NotNull Project project) {

        Log.log(LOGGER::debug, "in initializeWhenSmart, dumb mode is {}", DumbService.isDumb(project));

        //sometimes the views models are updated before the tool window is initialized.
        //it happens when files are re-opened early before the tool window, and CaretContextService.contextChanged
        //is invoked and updates the models.
        //SummaryViewService is also initialized before the tool window is opened, it will get the event when
        // the environment is loaded and will update its model but will not update the ui because the panel is
        // not initialized yet.
        //only at this stage the panels are constructed already. just calling updateUi() for all view services
        // will actually update the UI.
        //todo: probably not necessary, EditorEventsHandler.selectionChanged loads DocumentInfo and
        // calls contextChanged only in smart mode. and in smart mode the panels should be constructed already.
        // needs some testing.
        // on the other hand if the tool window is opened after EditorEventsHandler.selectionChanged then the
        // models will be populated with data but updateUi was not invoked
        project.getService(InsightsViewService.class).updateUi();
        project.getService(ErrorsViewService.class).updateUi();
        project.getService(SummaryViewService.class).updateUi();


        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it.
        //todo: probably not necessary anymore because EditorEventsHandler.selectionChanged loads DocumentInfo and
        // calls contextChanged only in smart mode. so even when documents are opened in dumb mode the loading of
        // DocumentInfo, installing caret listener and change listener will occur in smart mode. so the situation
        // mentioned above should not happen.
        // on the other hand: in Rider, smart mode doesn't guarantee that the solution is fully loaded. so even if
        // EditorEventsHandler.selectionChanged loads DocumentInfo in smart mode it does not guarantee that C# language
        // service will have access to PSI references because the solution may still be loading. so calling that only
        // after the solution is fully loaded will guarantee full PSi access. see above, calling initializeWhenSmart
        // with LanguageService.runWhenSmartForAll will solve it.
//        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
//        if (backendConnectionMonitor.isConnectionOk()) {
//            Log.log(LOGGER::debug,"calling environmentChanged in background");
//            Backgroundable.ensureBackground(project, "change environment", () -> {
//                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
//                Log.log(LOGGER::debug,"calling environmentChanged with current environment to cause refresh of views in smart mode");
//                publisher.environmentChanged(project.getService(AnalyticsService.class).getEnvironment().getCurrent());
//            });
//        }
    }
}
