package org.digma.intellij.plugin.vf;

import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

public class DigmaStackTraceVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public DigmaStackTraceVirtualFile(@NotNull String name, @NotNull DigmaStackTraceFileType fileType, @NotNull String text) {
        super(name,fileType,text);
    }

    public DigmaStackTraceVirtualFile(String name, String stackTrace) {
        super(name,stackTrace);
    }
}
