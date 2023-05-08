package org.digma.intellij.plugin.htmleditor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.ex.FakeFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DigmaHtmlFileType extends FakeFileType {

    public static final DigmaHtmlFileType INSTANCE = new DigmaHtmlFileType();

    private DigmaHtmlFileType() {
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "HtmlPreview";
    }

    @Override
    public @NotNull @Nls String getDisplayName() {
        return "HtmlPreview";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Used for file preview in embedded browser";
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        return file instanceof DigmaHtmlEditorFile;
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Nodes.PpWeb;
    }
}
