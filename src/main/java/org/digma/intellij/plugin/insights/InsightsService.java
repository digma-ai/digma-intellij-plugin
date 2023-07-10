package org.digma.intellij.plugin.insights;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.htmleditor.DigmaHTMLEditorProvider;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.ModelChangeListener;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.navigation.HomeSwitcherService;
import org.digma.intellij.plugin.navigation.InsightsAndErrorsTabsHelper;
import org.digma.intellij.plugin.posthog.ActivityMonitor;
import org.digma.intellij.plugin.recentactivity.RecentActivityService;
import org.digma.intellij.plugin.ui.common.Laf;
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope;
import org.digma.intellij.plugin.ui.model.MethodScope;
import org.digma.intellij.plugin.ui.service.InsightsViewService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class InsightsService implements Disposable {

    private final Logger logger = Logger.getInstance(InsightsService.class);

    private final Project project;
    private InsightsMessageRouterHandler messageRouter;


    public InsightsService(Project project) {
        this.project = project;



        project.getMessageBus().connect(this).subscribe(ModelChangeListener.MODEL_CHANGED_TOPIC, (ModelChangeListener) newModel -> pushInsights());

    }

    public static InsightsService getInstance(Project project) {
        return project.getService(InsightsService.class);
    }


    boolean isIndexHtml(String path) {
        return path.endsWith("index.html");
    }


    public InputStream buildIndexFromTemplate(String path) {

        if (!isIndexHtml(path)) {
            //should not happen
            return null;
        }

        return new InsightsIndexTemplateBuilder().build();
    }


    @Override
    public void dispose() {
        //nothing to do,its used as parent disposable for the AssetsPanel
    }

    public void setMessageRouter(InsightsMessageRouterHandler messageHandler) {
        this.messageRouter = messageHandler;
    }


    public List<CodeObjectInsight> getInsights() {
        try {
            Log.log(logger::debug, project, "got getInsights request");

            var insights = getCurrentInsights();
            Log.log(logger::debug, project, "got insights [{}]", insights);
            return insights;
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger,project,e,"Error loading insights {}",e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CodeObjectInsight> getCurrentInsights() throws AnalyticsServiceException {

        var scope = InsightsViewService.getInstance(project).getModel().getScope();
        if (scope instanceof MethodScope){
            return DocumentInfoService.getInstance(project).getCachedMethodInsights(((MethodScope) scope).getMethodInfo());
        }else if (scope instanceof CodeLessSpanScope){
            CodeLessSpanInsightsProvider insightsProvider = new CodeLessSpanInsightsProvider(((CodeLessSpanScope) scope).getSpan(),project);
            return insightsProvider.getRawInsights();
        }
        return new ArrayList<>();
    }


    public void pushInsights() {
        if (messageRouter != null){
            messageRouter.pushInsightsFromEvent();
        }
    }


    public void showInsight(String spanId) {
        Log.log(logger::debug, project, "showInsight called {}",spanId);
        project.getService(HomeSwitcherService.class).switchToInsights();
        project.getService(InsightsViewOrchestrator.class).showInsightsForCodelessSpan(spanId);
        project.getService(InsightsAndErrorsTabsHelper.class).switchToInsightsTab();
    }

    public void openHistogram(String spanId, String insightType) {

        ActivityMonitor.getInstance(project).registerInsightButtonClicked("histogram", insightType);

        var spanIdWithoutPrefix = CodeObjectsUtil.stripSpanPrefix(spanId);

        var instrumentationLibrary = spanIdWithoutPrefix.substring(0, spanIdWithoutPrefix.indexOf("$_$"));
        var spanName = spanIdWithoutPrefix.substring(spanIdWithoutPrefix.indexOf("$_$") + 3);

        var color = Laf.Colors.getPLUGIN_BACKGROUND();
        var s = GuiUtils.colorToHex(color);
        var colorName = s.startsWith("#") ? s : "#$s";
        try {
            String htmlContent = AnalyticsService.getInstance(project).getHtmlGraphForSpanPercentiles(instrumentationLibrary, spanName, colorName);
            DigmaHTMLEditorProvider.openEditor(project, "Percentiles Graph of Span ${span.name}", htmlContent);
        } catch (AnalyticsServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void openLiveView(String prefixedCodeObjectId) {
        Log.log(logger::debug, project, "openLiveView called {}",prefixedCodeObjectId);

        try {
            DurationLiveData durationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(prefixedCodeObjectId);
            RecentActivityService.getInstance(project).sendLiveData(durationLiveData, prefixedCodeObjectId);
        } catch (AnalyticsServiceException e) {
            Log.warnWithException(logger,project,e,"Error loading live view {}",e.getMessage());
        }

    }
}
