package org.digma.intellij.plugin.settings;

import java.util.Objects;

public class SettingsUtils {


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
            return !Objects.equals(Objects.requireNonNull(SettingsState.getInstance().getState()).apiUrl, SettingsState.DEFAULT_API_URL);
        } catch (NullPointerException e) {
            return false;
        }
    }

}
