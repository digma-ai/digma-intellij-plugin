package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.common.JsonUtils;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.toolwindow.recentactivity.incoming.CloseLiveViewMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import static org.digma.intellij.plugin.toolwindow.common.ToolWindowUtil.RECENT_ACTIVITY_SET_LIVE_DATA;

public class RecentActivityService implements Disposable {

    private final Logger logger = Logger.getInstance(RecentActivityService.class);

    private final Project project;
    private JBCefBrowser jbCefBrowser;

    //the recent activity code is not managed in one place that is accessible from the plugin code
    // like a project service, so currently need an init task.
    // it is used to send live data in case the live view button was clicked before the recent activity window was initialized.
    private MyInitTask initTask = null;

    private Timer myLiveDataTimer = null;

    public RecentActivityService(Project project) {
        this.project = project;
    }


    public static RecentActivityService getInstance(Project project){
        return project.getService(RecentActivityService.class);
    }

    public void setJcefBrowser(JBCefBrowser jbCefBrowser) {
        this.jbCefBrowser = jbCefBrowser;
    }

    public void sendLiveData(@NotNull DurationLiveData durationLiveData, @NotNull String codeObjectId) {

        Log.log(logger::debug,project,"Got sendLiveData request for {}",codeObjectId);

        stopLiveDataTimerTask();

        if (jbCefBrowser == null){
            Log.log(logger::debug,project,"jbCefBrowser is not initialized, calling showToolWindow");
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            //ugly hack for initialization when RECENT_ACTIVITY_INITIALIZE message is sent.
            // if the recent activity window was not yet initialized then we need to send the live data only after
            // RECENT_ACTIVITY_INITIALIZE message is sent.
            initTask = new MyInitTask(codeObjectId) {
                @Override
                public void run() {
                    sendLiveDataImpl(durationLiveData);
                }
            };
        }else{
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            sendLiveDataImpl(durationLiveData);
            startNewLiveDataTimerTask(codeObjectId);
        }
    }


    private void sendLiveDataImpl(DurationLiveData durationLiveData){
        Log.log(logger::debug,project,"sending live data for {}",durationLiveData.getDurationData().getCodeObjectId());
        LiveDataMessage liveDataMessageMessage =
                new LiveDataMessage("digma", RECENT_ACTIVITY_SET_LIVE_DATA,
                        new LiveDataPayload(durationLiveData.getLiveDataRecords(),durationLiveData.getDurationData()));
        try{
            var strMessage = JsonUtils.javaRecordToJsonString(liveDataMessageMessage);
            JBCefBrowserUtil.postJSMessage(strMessage, jbCefBrowser);
        }catch (Exception e){
            Log.debugWithException(logger,project,e,"Exception sending live data message");
        }
    }

    private void stopLiveDataTimerTask() {
        Log.log(logger::debug,project,"Stopping timer");
        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
            myLiveDataTimer = null;
        }
    }

    private void startNewLiveDataTimerTask(@NotNull String codeObjectId) {

        Log.log(logger::debug,project,"Starting new timer for {}",codeObjectId);

        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
        }
        myLiveDataTimer = new Timer();
        myLiveDataTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DurationLiveData newDurationLiveData = AnalyticsService.getInstance(project).getDurationLiveData(codeObjectId);
                    sendLiveDataImpl(newDurationLiveData);
                } catch (AnalyticsServiceException e) {
                    Log.debugWithException(logger,e,"Exception from getDurationLiveData {}",e.getMessage());
                }catch (Exception e){
                    //catch any other exception and rethrow because it's a bug we should fix
                    Log.debugWithException(logger,e,"Exception from myLiveDataTimer {}",e.getMessage());
                    throw e;
                }
            }
        },5000,5000);
    }




    public void runInitTask() {
        if (initTask != null){
            initTask.run();
            startNewLiveDataTimerTask(initTask.codeObjectId);
            initTask = null;
        }
    }

    public void liveViewClosed(@Nullable CloseLiveViewMessage closeLiveViewMessage) {

        //closeLiveViewMessage may be null if there is an error parsing the message.
        // this is a protection against errors so that the timer is always closed when user clicks the close button

        Log.log(logger::debug, project, "Stopping timer");
        if (closeLiveViewMessage != null) {
            Log.log(logger::debug, project, "Stopping timer for {}", closeLiveViewMessage.payload().codeObjectId());
            var codeObjectId = CodeObjectsUtil.stripMethodPrefix(closeLiveViewMessage.payload().codeObjectId());
            //currently not considering the codeObjectId because there is only one timer task.
            // but may be necessary in the future when there are few live views opened
        }
        stopLiveDataTimerTask();
    }

    @Override
    public void dispose() {
        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
        }
    }


    private abstract static class MyInitTask implements Runnable{

        private final String codeObjectId;

        public MyInitTask(@NotNull String codeObjectId) {
            this.codeObjectId = codeObjectId;
        }
    }
}
