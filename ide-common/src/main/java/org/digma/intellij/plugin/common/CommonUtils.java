package org.digma.intellij.plugin.common;

import org.ocpsoft.prettytime.PrettyTime;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

public final class CommonUtils {

    private CommonUtils() {
    }

    public static String getLocalHostname() {
        String hostname;
        try {
            InetAddress localInetAddress = InetAddress.getLocalHost();
            hostname = localInetAddress.getHostName();
        } catch (UnknownHostException e) {
            hostname = hostnameByEnvVar();
        }
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
}
