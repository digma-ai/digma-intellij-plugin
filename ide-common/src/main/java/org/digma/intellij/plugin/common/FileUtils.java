package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.vfs.ContentRevisionVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFileBase;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class FileUtils {

    private static final Logger LOGGER = Logger.getInstance(FileUtils.class);

    public static boolean isLightVirtualFileBase(@Nullable VirtualFile virtualFile){
        return virtualFile instanceof LightVirtualFileBase;
    }


    public static boolean isVcsFile(VirtualFile virtualFile) {

        if (virtualFile instanceof LightVirtualFileBase lightVirtualFileBase) {
            return lightVirtualFileBase.getOriginalFile() instanceof ContentRevisionVirtualFile;

        }
        return false;
    }

    public static String convertWinToWslPath(String path){
        // Converting From: C:\Users\XXXXX\AppData\Local\Temp\digma-otel-jars\opentelemetry-javaagent.jar
        // To:              /mnt/c/Users/XXXXX/AppData/Local/Temp/digma-otel-jars/opentelemetry-javaagent.jar
        var regex = Pattern.compile("^([a-z,A-Z]):\\\\(.*)$");
        var match = regex.matcher(path);
        if(!match.matches()){
            Log.log(LOGGER::warn, "Failed to convert win path '" + path + "' to wsl");
            return path;
        }

        return "/mnt/" + match.group(1).toLowerCase() + "/" + match.group(2).replace("\\", "/");
    }
}
