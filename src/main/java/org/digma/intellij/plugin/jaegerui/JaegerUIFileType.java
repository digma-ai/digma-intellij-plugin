package org.digma.intellij.plugin.jaegerui;

import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class JaegerUIFileType  extends FakeFileType {

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
    public @NlsContexts.Label @NotNull String getDescription() {
        return "JaegerUI file type";
    }
}
