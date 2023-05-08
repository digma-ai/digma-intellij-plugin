package org.digma.intellij.plugin.htmleditor;

import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

public class DigmaHtmlEditorFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public DigmaHtmlEditorFile(@NotNull String name,@NotNull DigmaHtmlFileType fileType,@NotNull String htmlContent) {
        super(name,fileType,htmlContent);
    }
}
