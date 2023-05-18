package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.AnalyticsServiceException;
import org.digma.intellij.plugin.document.CodeObjectsUtil;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.toolwindow.recentactivity.incoming.CloseLiveViewMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataPayload;
import org.jetbrains.annotations.NotNull;

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

    public void sendLiveData(@NotNull DurationLiveData durationLiveData) {

        Log.log(logger::debug,project,"Got sendLiveData request for {}",durationLiveData.getDurationInsight().getCodeObjectId());

        stopLiveDataTimerTask();

        if (jbCefBrowser == null){
            Log.log(logger::debug,project,"jbCefBrowser is not initialized, calling showToolWindow");
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            //ugly hack for initialization when RECENT_ACTIVITY_INITIALIZE message is sent.
            // if the recent activity window was not yet initialized then we need to send the live data only after
            // RECENT_ACTIVITY_INITIALIZE message is sent.
            initTask = new MyInitTask(durationLiveData) {
                @Override
                public void run() {
                    sendLiveDataImpl(durationLiveData);
                }
            };
        }else{
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            sendLiveDataImpl(durationLiveData);
            startNewLiveDataTimerTask(durationLiveData);
        }
    }


    private void sendLiveDataImpl(DurationLiveData durationLiveData){
        Log.log(logger::debug,project,"sending live data for {}",durationLiveData.getDurationInsight().getCodeObjectId());
        LiveDataMessage liveDataMessageMessage =
                new LiveDataMessage("digma", RECENT_ACTIVITY_SET_LIVE_DATA,
                        new LiveDataPayload(durationLiveData.getLiveDataRecords(),durationLiveData.getDurationInsight()));
        var strMessage = JBCefBrowserUtil.resultToString(liveDataMessageMessage);
        JBCefBrowserUtil.postJSMessage(strMessage, jbCefBrowser);
    }

    private void stopLiveDataTimerTask() {
        Log.log(logger::debug,project,"Stopping timer");
        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
            myLiveDataTimer = null;
        }
    }

    private void startNewLiveDataTimerTask(DurationLiveData originalDurationLiveData) {

        Log.log(logger::debug,project,"Starting new timer for {}",originalDurationLiveData.getDurationInsight().getCodeObjectId());

        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
        }
        myLiveDataTimer = new Timer();
        myLiveDataTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DurationLiveData newDurationLiveData =
                            AnalyticsService.getInstance(project).getDurationLiveData(originalDurationLiveData.getDurationInsight().getCodeObjectId());
                    sendLiveDataImpl(newDurationLiveData);
                } catch (AnalyticsServiceException e) {
                    Log.debugWithException(logger,e,"Exception from getDurationLiveData {}",e.getMessage());
                }

            }
        },5000,5000);
    }




    public void runInitTask() {
        if (initTask != null){
            initTask.run();
            startNewLiveDataTimerTask(initTask.durationLiveData);
            initTask = null;
        }
    }

    public void liveViewClosed(CloseLiveViewMessage closeLiveViewMessage) {
        Log.log(logger::debug,project,"Stopping timer for {}",closeLiveViewMessage.payload().codeObjectId());
        var codeObjectId = CodeObjectsUtil.stripMethodPrefix(closeLiveViewMessage.payload().codeObjectId());
        //currently not considering the codeObjectId because there is only one timer task
        stopLiveDataTimerTask();
    }

    @Override
    public void dispose() {
        if (myLiveDataTimer != null){
            myLiveDataTimer.cancel();
        }
    }


    private abstract static class MyInitTask implements Runnable{

        private final DurationLiveData durationLiveData;

        public MyInitTask(@NotNull DurationLiveData durationLiveData) {
            this.durationLiveData = durationLiveData;
        }
    }
}
