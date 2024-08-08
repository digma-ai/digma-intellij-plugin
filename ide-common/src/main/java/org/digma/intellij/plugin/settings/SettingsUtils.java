package org.digma.intellij.plugin.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

public class SettingsUtils {

    private static final Logger LOGGER = Logger.getInstance(SettingsUtils.class);

    public static boolean isSettingsPointsToRemoteIp() {

        //todo: real IP check of the url, can be mapped in hosts file,for example cam map mymachine to localhost
//        try {
//            var inetAddress = InetAddress.getByName(new URL(Objects.requireNonNull(SettingsState.getInstance().getState()).apiUrl).getHost());
//            var localhost = CommonUtils.getLocalhost();
//            if (inetAddress.getHostAddress().equals("127.0.0.1")){
//                return true;
//            }
//
//            return false
//
//        }catch (Exception e){
//
//        }

        try {
            return !Objects.equals(Objects.requireNonNull(SettingsState.getInstance().getState()).getApiUrl(), SettingsState.DEFAULT_API_URL);
        } catch (NullPointerException e) {
            return false;
        }
    }


    public static boolean exportSettingsToFile(@NotNull File file) {
        try (OutputStream out = new FileOutputStream(file)) {
            var properties = SettingsState.getInstance().asProperties();
            properties.store(out, "Digma Plugin settings");
            return true;
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "error exporting settings {}", e);
            ErrorReporter.getInstance().reportError("SettingsUtils.exportSettingsToFile", e);
        }
        return false;
    }


    @Nullable
    public static Properties importSettingsFromFile(@NotNull VirtualFile virtualFile) {
        try (InputStream in = new FileInputStream(virtualFile.toNioPath().toFile())) {
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            Log.warnWithException(LOGGER, e, "error importing settings {}", e);
            ErrorReporter.getInstance().reportError("SettingsUtils.importSettingsFromFile", e);
        }
        return null;
    }
}
