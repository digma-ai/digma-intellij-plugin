package org.digma.intellij.plugin.errors;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.rest.errordetails.CodeObjectErrorDetails;
import org.digma.intellij.plugin.model.rest.errordetails.DetailedErrorInfo;
import org.digma.intellij.plugin.model.rest.errordetails.Frame;
import org.digma.intellij.plugin.model.rest.errordetails.FrameStack;
import org.digma.intellij.plugin.model.rest.errors.CodeObjectError;
import org.digma.intellij.plugin.ui.model.errors.*;
import org.digma.intellij.plugin.ui.model.listview.ListViewItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ErrorsProvider {

    private static final Logger LOGGER = Logger.getInstance(ErrorsProvider.class);

    private final AnalyticsService analyticsService;
    private final DocumentInfoService documentInfoService;


    public ErrorsProvider(@NotNull Project project) {
        analyticsService = project.getService(AnalyticsService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public ErrorsListContainer getErrors(@NotNull MethodInfo methodInfo) {
        try {
            final List<CodeObjectError> codeObjectErrors = analyticsService.getErrorsOfCodeObject(methodInfo.getId());
            Log.log(LOGGER::debug, "CodeObjectErrors for {}: {}", methodInfo, codeObjectErrors);

            final List<ListViewItem<CodeObjectError>> lviList = codeObjectErrors
                    .stream()
                    .map(x -> new ListViewItem<>(x, 1))
                    .collect(Collectors.toList());

            return new ErrorsListContainer(lviList);
        }catch (AnalyticsServiceException e){
            //if analyticsService.getErrorsOfCodeObject throws exception it means errors could not be loaded, usually when
            //the backend is not available. return an empty ErrorsListContainer to keep everything running and don't
            //crash the plugin. don't log the exception, it was logged in AnalyticsService, keep the log quite because
            //it may happen many times.
            return new ErrorsListContainer(new ArrayList<>());
        }
    }



    @NotNull
    public ErrorDetailsModel getErrorDetails(@NotNull String uid) {
        try {
            var errorDetails = analyticsService.getErrorDetails(uid);
            var methodInfo = documentInfoService.findMethodInfo(errorDetails.getSourceCodeObjectId());
            var model = new ErrorDetailsModel();
            model.setDelegate(errorDetails);
            model.setMethodInfo(methodInfo);
            model.setFlowStacks(buildFlowStacks(errorDetails));
            return model;
        } catch (AnalyticsServiceException e) {
            return new ErrorDetailsModel();
        }
    }

    private FlowStacks buildFlowStacks(CodeObjectErrorDetails errorDetails) {

        FlowStacks flowStacks = new FlowStacks();
        errorDetails.getErrors().forEach(detailedErrorInfo ->
                flowStacks.getStacks().add(buildFlowStack(detailedErrorInfo)));

        flowStacks.setCurrent(0);
        return flowStacks;
    }

    private List<ListViewItem<FrameListViewItem>> buildFlowStack(DetailedErrorInfo detailedErrorInfo) {

        var viewItems = new ArrayList<ListViewItem<FrameListViewItem>>();
        var index = 0;
        String currentSpan = null;

        for (FrameStack frameStack : detailedErrorInfo.getFrameStacks()) {

            var frameStackTitle = new FrameStackTitle(frameStack);
            var frameStackViewItem = new ListViewItem<FrameListViewItem>(frameStackTitle, index++);
            viewItems.add(frameStackViewItem);

            boolean first = true;
            for (Frame frame : frameStack.getFrames()) {

                if (currentSpan == null || !currentSpan.equals(frame.getSpanName())){
                    currentSpan = frame.getSpanName();
                    var spanTitle = new SpanTitle(currentSpan);
                    var spanTitleViewItem = new ListViewItem<FrameListViewItem>(spanTitle,index++);
                    viewItems.add(spanTitleViewItem);
                }

                var frameItem = new FrameItem(frameStack,frame,first);
                first = false;
                //todo: its only the file name but probably we don't need workspaceUri in order to open the file
                // because the frame already contains all the info for opening the file.
                // but use workspaceUri to decide if its in workspace or not.
                // another way in rider would be to query our reshrper cache with frame.codeObjectId
                frameItem.setWorkspaceUrl(frame.getModulePhysicalPath());
                var frameViewItem = new ListViewItem<FrameListViewItem>(frameItem,index++);
                viewItems.add(frameViewItem);

            }
        }

        return viewItems;
    }
}
