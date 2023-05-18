package org.digma.intellij.plugin.toolwindow.recentactivity;

import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.digma.intellij.plugin.model.rest.livedata.DurationLiveData;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataMessage;
import org.digma.intellij.plugin.toolwindow.recentactivity.outgoing.LiveDataPayload;

public class RecentActivityService {

    private final Project project;
    private JBCefBrowser jbCefBrowser;

    private Runnable initTask = null;

    public RecentActivityService(Project project) {
        this.project = project;
    }


    public static RecentActivityService getInstance(Project project){
        return project.getService(RecentActivityService.class);
    }

    public void setJcefBrowser(JBCefBrowser jbCefBrowser) {
        this.jbCefBrowser = jbCefBrowser;
    }

    public void sendLiveData(DurationLiveData durationLiveData) {
        if (jbCefBrowser == null){
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            //ugly hack for initialization
            initTask = () -> sendLiveDataImpl(durationLiveData);
        }else{
            RecentActivityToolWindowShower.getInstance(project).showToolWindow();
            sendLiveDataImpl(durationLiveData);
        }
    }


    private void sendLiveDataImpl(DurationLiveData durationLiveData){
        LiveDataMessage liveDataMessageMessage =
                new LiveDataMessage("digma", "RECENT_ACTIVITY/SET_LIVE_DATA",
                        new LiveDataPayload(durationLiveData.getLiveDataRecords(),durationLiveData.getDurationInsight()));
        var strMessage = JBCefBrowserUtil.resultToString(liveDataMessageMessage);
        JBCefBrowserUtil.postJSMessage(strMessage, jbCefBrowser);
    }

    public void runInitTask() {
        if (initTask != null){
            initTask.run();
            initTask = null;
        }
    }
}
