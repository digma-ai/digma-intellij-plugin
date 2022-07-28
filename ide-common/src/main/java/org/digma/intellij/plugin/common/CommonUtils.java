package org.digma.intellij.plugin.common;

import org.apache.commons.lang.StringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.net.InetAddress;
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
        hostname = StringUtils.removeEnd(hostname, ".local");
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
}
