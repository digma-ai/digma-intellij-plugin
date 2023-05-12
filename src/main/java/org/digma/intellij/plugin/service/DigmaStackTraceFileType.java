package org.digma.intellij.plugin.service;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DigmaStackTraceFileType extends FakeFileType {

    public static final DigmaStackTraceFileType INSTANCE = new DigmaStackTraceFileType();

    private DigmaStackTraceFileType() {
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "StackTrace";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "StackTrace";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Used for opening stack trace in the editor";
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return file instanceof DigmaStackTraceVirtualFile;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.PpWeb;
    }
}
