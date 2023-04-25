package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

public class JaegerUIMessageRouterHandler extends CefMessageRouterHandlerAdapter {

    private final Project project;
    private final JBCefBrowser jbCefBrowser;

    public JaegerUIMessageRouterHandler(Project project, JBCefBrowser jbCefBrowser) {
        this.project = project;
        this.jbCefBrowser = jbCefBrowser;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        //todo: implement
        return super.onQuery(browser, frame, queryId, request, persistent, callback);
    }

    @Override
    public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
        //todo: implement
        super.onQueryCanceled(browser, frame, queryId);
    }
}
