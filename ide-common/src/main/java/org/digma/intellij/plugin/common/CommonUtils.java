package org.digma.intellij.plugin.common;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.log.Log;
import org.jetbrains.annotations.Nullable;
import org.ocpsoft.prettytime.PrettyTime;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class CommonUtils {

    private static final Logger LOGGER = Logger.getInstance(CommonUtils.class);

    private static InetAddress localhost;
    private static final String hostname;

    static {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            //ignore
        }
        hostname = initHostname();
    }

    private static String initHostname() {

        var stopWatch = StopWatch.createStarted();

        String hostname;
        try {
            if (localhost != null) {
                hostname = localhost.getHostName();
            } else {
                InetAddress localInetAddress = InetAddress.getLocalHost();
                hostname = localInetAddress.getHostName();
            }
        } catch (UnknownHostException e) {
            hostname = hostnameByEnvVar();
        }

        Log.log(LOGGER::trace, "initHostname took {}", stopWatch.getTime(TimeUnit.MILLISECONDS));
        return hostname;
    }

    private static String hostnameByEnvVar() {
        String hostname;
        hostname = System.getenv("COMPUTERNAME"); // windows
        if (hostname == null) {
            hostname = System.getenv("HOSTNAME"); // linux
        }
        if (hostname == null) {
            throw new RuntimeException("could not resolve hostname by environment variables");
        }
        return hostname;
    }


    private CommonUtils() {
    }

    @Nullable
    public static InetAddress getLocalhost() {
        return localhost;
    }


    public static String getLocalHostname() {
        return hostname;
    }

    public static String prettyTimeOf(Date date) {
        PrettyTime ptNow = new PrettyTime();
        return ptNow.format(date);
    }


    public static boolean isWelFormedUrl(String url){
        if (url == null || url.isBlank()){
            return false;
        }
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean isHttpsUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            return Objects.equals(new URL(url).getProtocol(), "https");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
