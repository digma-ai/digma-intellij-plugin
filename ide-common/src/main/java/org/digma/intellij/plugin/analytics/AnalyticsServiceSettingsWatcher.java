package org.digma.intellij.plugin.analytics;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.*;
import org.digma.intellij.plugin.auth.AuthManager;
import org.digma.intellij.plugin.common.ProjectUtilsKt;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.settings.SettingsState;

import java.util.Objects;


/**
 * catch settingsChanged events for apiUrl and instructs all AnalyticsService for all
 * open projects to replace the api client.
 * it is an application service , the reason is that it needs to instruct AuthManager to logout
 * and to replace its client, AuthManager is also an application service, if this listener was a
 * project service the AuthManager would replace its client for every open project which is unnecessary.
 */
@Service(Service.Level.APP)
public final class AnalyticsServiceSettingsWatcher implements Disposable {

    private static final Logger LOGGER = Logger.getInstance(AnalyticsService.class);

    private String myApiUrl;

    public AnalyticsServiceSettingsWatcher() {
        myApiUrl = SettingsState.getInstance().apiUrl;

        SettingsState.getInstance().addChangeListener(state -> {

            Log.log(LOGGER::debug, "settings changed event");

            boolean shouldReplaceClient = false;

            if (!Objects.equals(state.apiUrl, myApiUrl)) {
                myApiUrl = state.apiUrl;
                shouldReplaceClient = true;
            }

            //replace the client only when apiUrl is changed.
            //there is no need to replace the client when api token is changed because there is an
            // AuthenticationProvider that always takes it from the settings
            if (shouldReplaceClient) {
                Log.log(LOGGER::debug, "api url changed to {}, calling replace client", myApiUrl);
                AuthManager.getInstance().logout();
                AuthManager.getInstance().pauseBeforeClientChange();
                AuthManager.getInstance().replaceClient(myApiUrl);

                for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
                    if (ProjectUtilsKt.isProjectValid(openProject)) {
                        var analyticsService = AnalyticsService.getInstance(openProject);
                        analyticsService.replaceClient(myApiUrl);
                    }
                }
            }

        }, this);


    }

    public static AnalyticsServiceSettingsWatcher getInstance() {
        return ApplicationManager.getApplication().getService(AnalyticsServiceSettingsWatcher.class);
    }

    @Override
    public void dispose() {
        //nothing to do , used as parent disposable
    }
}
