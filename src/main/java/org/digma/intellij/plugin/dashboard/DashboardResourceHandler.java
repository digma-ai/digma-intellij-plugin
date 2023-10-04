package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.cef.callback.CefCallback;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.digma.intellij.plugin.log.Log;

import java.io.IOException;
import java.io.InputStream;

public class DashboardResourceHandler implements CefResourceHandler {

    private static final Logger LOGGER = Logger.getInstance(DashboardResourceHandler.class);

    private final Project project;
    private final String path;
    private final DashboardVirtualFile dashboardVirtualFile;
    private InputStream inputStream;

    private CefRequest.ResourceType resourceType;

    public DashboardResourceHandler(Project project, String path, DashboardVirtualFile file) {
        this.project = project;
        this.path = path;
        this.dashboardVirtualFile = file;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {

        if (DashboardService.getInstance(project).isIndexHtml(path)) {
            inputStream = DashboardService.getInstance(project).buildIndexFromTemplate(path, dashboardVirtualFile);
        } else {
            inputStream = getClass().getResourceAsStream(path);
        }

        if (inputStream == null) {
            Log.log(LOGGER::warn, "inputStream is null , canceling request " + request.getURL());
            callback.cancel();
            return false;
        }

        resourceType = request.getResourceType();
        callback.Continue();
        return true;
    }


    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {

        if (inputStream == null) {
            response.setError(CefLoadHandler.ErrorCode.ERR_FILE_NOT_FOUND);
            response.setStatusText("file not found " + path);
            response.setStatus(404);
            return;
        }

        response.setStatus(200);
        response.setMimeType(getMimeType());
        try {
            responseLength.set(inputStream.available());
        } catch (IOException e) {
            response.setError(CefLoadHandler.ErrorCode.ERR_ABORTED);
            response.setStatusText("internal error for " + path);
            response.setStatus(500);
        }
    }

    private String getMimeType() {
        switch (resourceType) {
            case RT_MAIN_FRAME -> {
                return "text/html";
            }
            case RT_SCRIPT -> {
                return "text/javascript";
            }
            case RT_STYLESHEET -> {
                return "text/css";
            }
            case RT_IMAGE -> {
                if (path.endsWith("svg")) {
                    return "image/svg+xml";
                } else {
                    return "image/png";
                }
            }
            default -> {
                return "text/plain";
            }
        }
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        try {
            int available = inputStream.available();
            if (available == 0) {
                bytesRead.set(0);
                inputStream.close();
                return false;
            }
            var toRead = Math.min(available, bytesToRead);
            var read = inputStream.read(dataOut, 0, toRead);
            bytesRead.set(read);
            return true;
        } catch (Exception e) {
            throw new DashboardException(e);
        }
    }

    @Override
    public void cancel() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            //ignore
        }
    }
}
