package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class DashboardFileType  extends FakeFileType {

    private DashboardFileType() {
    }

    public static final DashboardFileType INSTANCE = new DashboardFileType();

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return DashboardVirtualFile.isDocumentationVirtualFile(file);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "DashboardUI";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "DashboardUI";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "DashboardUI file type";
    }
}
