package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.*;

public class JaegerUIFileType  extends FakeFileType {

    private JaegerUIFileType() {
    }

    public static final JaegerUIFileType INSTANCE = new JaegerUIFileType();

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return JaegerUIVirtualFile.isJaegerUIVirtualFile(file);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "JaegerUI";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "JaegerUI";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "JaegerUI file type";
    }
}
