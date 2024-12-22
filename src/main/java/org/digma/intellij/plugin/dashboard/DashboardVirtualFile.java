package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

public class DashboardVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public static final Key<String> DASHBOARD_EDITOR_KEY = Key.create("Digma.DASHBOARD_EDITOR_KEY");
    private String dashboardName;
    private String initialRoute = "";

    public DashboardVirtualFile(String myTitle) {
        super(myTitle);
        setFileType(DashboardFileType.INSTANCE);
        setInitialRoute("");
        setWritable(false);
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true);
    }

    public static boolean isDashboardVirtualFile(@NotNull VirtualFile file) {
        return file instanceof DashboardVirtualFile;
    }

    @NotNull
    public static DashboardVirtualFile createVirtualFile(@NotNull String dashboardName) {
        var file = new DashboardVirtualFile(dashboardName);
        file.setDashboardName(dashboardName);
        DASHBOARD_EDITOR_KEY.set(file, DashboardFileEditorProvider.DASHBOARD_EDITOR_TYPE);
        return file;
    }

    public void setDashboardName(String dashboardName) {
        this.dashboardName = dashboardName;
    }

    public String getDashboardName() {
        return dashboardName;
    }

    public void setInitialRoute(String newInitialRoute) {
        this.initialRoute = newInitialRoute;
    }

    @NotNull
    public String getInitialRoute() {
        return initialRoute;
    }
}
