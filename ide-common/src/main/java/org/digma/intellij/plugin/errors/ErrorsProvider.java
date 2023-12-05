package org.digma.intellij.plugin.errors;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errordetails.ErrorFlowInfo;
import org.digma.intellij.plugin.model.rest.errordetails.Frame;
import org.digma.intellij.plugin.model.rest.errordetails.FrameStack;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.ui.model.errors.ErrorDetailsModel;
import org.digma.intellij.plugin.ui.model.errors.FlowStacks;
import org.digma.intellij.plugin.ui.model.errors.FrameItem;
import org.digma.intellij.plugin.ui.model.errors.FrameListViewItem;
import org.digma.intellij.plugin.ui.model.errors.FrameStackTitle;
import org.digma.intellij.plugin.ui.model.errors.SpanTitle;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ErrorsProvider {

    private static final Logger LOGGER = Logger.getInstance(ErrorsProvider.class);

    private final AnalyticsService analyticsService;
    private final DocumentInfoService documentInfoService;
    private final Project project;


    public ErrorsProvider(@NotNull Project project) {
        this.project = project;
        analyticsService = project.getService(AnalyticsService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public ErrorsListContainer getErrors(@NotNull MethodInfo methodInfo) {
        var stopWatch = StopWatch.createStarted();
        try {
            final List<CodeObjectError> codeObjectErrors = analyticsService.getErrorsOfCodeObject(methodInfo.allIdsWithType());
            Log.log(LOGGER::debug, "CodeObjectErrors for {}: {}", methodInfo.getId(), codeObjectErrors);

            final List<ListViewItem<CodeObjectError>> errorsListViewItems = codeObjectErrors
                    .stream()
                    .map(x -> new ListViewItem<>(x, 1))
                    .collect(Collectors.toList());

            Log.log(LOGGER::debug, "ListViewItems for {}: {}", methodInfo.getId(), errorsListViewItems);

            return new ErrorsListContainer(errorsListViewItems);
        } catch (AnalyticsServiceException e) {
            //if analyticsService.getErrorsOfCodeObject throws exception it means errors could not be loaded, usually when
            //the backend is not available. return an empty ErrorsListContainer to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            Log.log(LOGGER::debug, "AnalyticsServiceException for getErrors for {}: {}", methodInfo.getId(), e.getMessage());
            return new ErrorsListContainer();
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "getErrors time took {} milliseconds", stopWatch.getTime(java.util.concurrent.TimeUnit.MILLISECONDS));
        }
    }


    @NotNull
    public ErrorDetailsModel getErrorDetails(@NotNull String uid) {
        try {
            var errorDetails = analyticsService.getErrorDetails(uid);
            var methodCodeObjectId = MethodInfo.Companion.removeType(errorDetails.getSourceCodeObjectId());

            //we need to find a languageService for this error.
            //getErrorDetails may be called from few places.
            //when clicked in errors insight then the document is probably opened and DocumentInfoService has the MethodInfo
            //and the language.
            var languageService = LanguageService.findLanguageServiceByMethodCodeObjectId(project, methodCodeObjectId);

            var methodInfo = documentInfoService.findMethodInfo(methodCodeObjectId);

            var model = new ErrorDetailsModel();
            model.setDelegate(errorDetails);
            model.setMethodInfo(methodInfo);
            model.setFlowStacks(buildFlowStacks(errorDetails, languageService));
            return model;
        } catch (AnalyticsServiceException e) {
            return new ErrorDetailsModel();
        }
    }

    private FlowStacks buildFlowStacks(CodeObjectErrorDetails errorDetails, LanguageService languageService) {

        FlowStacks flowStacks = new FlowStacks();
        errorDetails.getErrors().forEach(errorFlowInfo ->
                flowStacks.getStacks().add(buildFlowStack(errorFlowInfo, languageService)));

        flowStacks.setCurrent(0);
        return flowStacks;
    }

    private List<ListViewItem<FrameListViewItem>> buildFlowStack(ErrorFlowInfo errorFlowInfo, LanguageService languageService) {

        Map<String, String> workspaceUris = findWorkspaceUriForFrames(errorFlowInfo, languageService);

        var viewItems = new ArrayList<ListViewItem<FrameListViewItem>>();
        var index = 0;
        for (FrameStack frameStack : errorFlowInfo.getFrameStacks()) {

            var frameStackTitle = new FrameStackTitle(frameStack, errorFlowInfo.getLatestTraceId());
            var frameStackViewItem = new ListViewItem<FrameListViewItem>(frameStackTitle, index++);
            viewItems.add(frameStackViewItem);

            boolean first = true;
            String currentSpan = null;
            for (Frame frame : frameStack.getFrames()) {

                if (currentSpan == null || !currentSpan.equals(frame.getSpanName())) {
                    currentSpan = frame.getSpanName();
                    var spanTitle = new SpanTitle(currentSpan);
                    var spanTitleViewItem = new ListViewItem<FrameListViewItem>(spanTitle, index++);
                    viewItems.add(spanTitleViewItem);
                }

                var workspaceUri = workspaceUris.getOrDefault(frame.getCodeObjectId(), null);
                var frameItem = new FrameItem(frameStack, frame, first, workspaceUri, errorFlowInfo.getLastInstanceCommitId());
                first = false;

                var frameViewItem = new ListViewItem<FrameListViewItem>(frameItem, index++);
                viewItems.add(frameViewItem);

            }
        }

        return viewItems;
    }


    private Map<String, String> findWorkspaceUriForFrames(ErrorFlowInfo errorFlowInfo, LanguageService languageService) {

        List<String> codeObjectIds = errorFlowInfo.getFrameStacks().stream().
                flatMap((Function<FrameStack, Stream<String>>) frameStack -> frameStack.getFrames().stream().
                        map(Frame::getCodeObjectId)).collect(Collectors.toList());


        return languageService.findWorkspaceUrisForCodeObjectIdsForErrorStackTrace(codeObjectIds);

    }
}
