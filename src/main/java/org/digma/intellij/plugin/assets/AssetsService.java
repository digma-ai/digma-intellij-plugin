package org.digma.intellij.plugin.assets;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.insights.InsightsViewOrchestrator;
import org.digma.intellij.plugin.jaegerui.JaegerUIService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.navigation.HomeSwitcherService;
import org.digma.intellij.plugin.ui.service.TabsHelper;

import java.io.InputStream;

@Service(Service.Level.PROJECT)
public final class AssetsService implements Disposable {

    private final Logger logger = Logger.getInstance(JaegerUIService.class);

    private final Project project;



    public AssetsService(Project project) {
        this.project = project;

    }

    public static AssetsService getInstance(Project project) {
        return project.getService(AssetsService.class);
    }


    boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }


    public InputStream buildIndexFromTemplate(String path) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new IndexTemplateBuilder(path).build();
    }


    @Override
    public void dispose() {
        //nothing to do,its used as parent disposable for the AssetsPanel
    }

    public String getAssets() {
        try {
            return AnalyticsService.getInstance(project).getAssets();
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger,project,e,"Error loading assets {}",e.getMessage());
            return "";
        }
    }

    public void showAsset(String spanId) {
        project.getService(HomeSwitcherService.class).switchToInsights();
        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(spanId);
        project.getService(TabsHelper.class).notifyTabChanged(0);
    }
}
