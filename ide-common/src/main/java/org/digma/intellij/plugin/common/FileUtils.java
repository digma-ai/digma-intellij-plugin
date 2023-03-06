package org.digma.intellij.plugin.common;

import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFileBase;
import org.jetbrains.annotations.Nullable;

public class FileUtils {

    public static boolean isLightVirtualFileBase(@Nullable VirtualFile virtualFile){
        return virtualFile instanceof LightVirtualFileBase;
    }


    public static boolean isVcsFile(VirtualFile virtualFile) {

        if (virtualFile instanceof LightVirtualFileBase lightVirtualFileBase) {
            return lightVirtualFileBase.getOriginalFile() instanceof ContentRevisionVirtualFile;

        }
        return false;
    }
}
