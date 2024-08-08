package org.digma.intellij.plugin.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.vfs.VirtualFile;
import org.digma.intellij.plugin.common.*;
import org.digma.intellij.plugin.errorreporting.ErrorReporter;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.*;

import java.io.File;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

public class SettingsUtils {

    private static final Logger LOGGER = Logger.getInstance(SettingsUtils.class);


    static void validateSettings(SettingsComponent settingsComponent) throws ConfigurationException {
        try {
            Objects.requireNonNull(settingsComponent.getApiUrl(), "Api url can not be null");
            Objects.requireNonNull(settingsComponent.getRuntimeObservabilityBackendUrl(), "Runtime observability url can not be null");
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

        if (settingsComponent.getApiUrl().isBlank()) {
            throw new ConfigurationException("Api url can not be empty");
        }
        try {
            URLValidator.create(settingsComponent.getApiUrl()).validate();
        } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                 URLValidator.IncorrectSchemaException e) {
            throw new ConfigurationException("Api url is not a well formed: " + e.getMessage());
        }
        if (!CommonUtils.isHttpsUrl(settingsComponent.getApiUrl())) {
            throw new ConfigurationException("Api url schema must be https");
        }

        if (settingsComponent.getRuntimeObservabilityBackendUrl().isBlank()) {
            throw new ConfigurationException("Runtime observability url can not be empty");
        }
        try {
            URLValidator.create(settingsComponent.getRuntimeObservabilityBackendUrl()).validate();
        } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                 URLValidator.IncorrectSchemaException e) {
            throw new ConfigurationException("Backend observability url is not a well formed: " + e.getMessage());
        }


        if (settingsComponent.getJaegerLinkMode() == JaegerLinkMode.Embedded) {
            if (settingsComponent.getJaegerQueryUrl() == null || settingsComponent.getJaegerQueryUrl().trim().isBlank()) {
                throw new ConfigurationException("Jaeger query url can not be empty in mode " + settingsComponent.getJaegerLinkMode());
            }
            try {
                URLValidator.create(settingsComponent.getJaegerQueryUrl()).validate();
            } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                     URLValidator.IncorrectSchemaException e) {
                throw new ConfigurationException("Jaeger query url is not a well formed: " + e.getMessage());
            }
        } else {
            if (settingsComponent.getJaegerUrl() == null || settingsComponent.getJaegerUrl().trim().isBlank()) {
                throw new ConfigurationException("Jaeger url can not be empty in mode " + settingsComponent.getJaegerLinkMode());
            }
            try {
                if (settingsComponent.getJaegerUrl() != null) {
                    URLValidator.create(settingsComponent.getJaegerUrl()).validate();
                }
            } catch (MalformedURLException | URISyntaxException | URLValidator.InvalidUrlException | URLValidator.QueryNotAllowedException |
                     URLValidator.IncorrectSchemaException e) {
                throw new ConfigurationException("Jaeger url is not a well formed: " + e.getMessage());
            }
        }
    }


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


    public static boolean exportSettingsToFile(@NotNull File file, @NotNull Map<String, String> properties) {
        try {
            //using new ArrayList because the steam may return immutable list
            var lines = new ArrayList<String>();
            lines.add("## Digma plugin settings " + new Date());
            lines.addAll(properties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
            Files.write(file.toPath(), lines, CREATE, TRUNCATE_EXISTING, WRITE);
            return true;
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "error exporting settings {}", e);
            ErrorReporter.getInstance().reportError("SettingsUtils.exportSettingsToFile", e);
        }
        return false;
    }


    @Nullable
    public static Map<String, String> importSettingsFromFile(@NotNull VirtualFile virtualFile) {
        try {
            //using new ArrayList because Files.readAllLines may return immutable list
            var lines = new ArrayList<>(Files.readAllLines(virtualFile.toNioPath()));
            if (lines.isEmpty()) {
                return null;
            }

            if (lines.get(0).contains("Digma plugin settings")) {
                lines.remove(0);
            }
            var properties = new HashMap<String, String>();
            lines.forEach(line -> {
                var entry = line.split("=", 2);
                if (entry.length == 2) {
                    properties.put(entry[0], entry[1]);
                }
            });
            return properties;
        } catch (Throwable e) {
            Log.warnWithException(LOGGER, e, "error importing settings {}", e);
            ErrorReporter.getInstance().reportError("SettingsUtils.importSettingsFromFile", e);
        }

        return null;
    }
}
