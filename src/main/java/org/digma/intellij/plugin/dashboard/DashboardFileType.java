package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.*;

public class DashboardFileType  extends FakeFileType {

    private DashboardFileType() {
    }

    public static final DashboardFileType INSTANCE = new DashboardFileType();

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return DashboardVirtualFile.isDashboardVirtualFile(file);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "Dashboard";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "Dashboard";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Dashboard file type";
    }
}
