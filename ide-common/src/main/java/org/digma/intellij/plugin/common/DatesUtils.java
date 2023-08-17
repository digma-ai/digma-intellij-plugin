package org.digma.intellij.plugin.common;

import java.time.Instant;

public class DatesUtils {


    public static class Instants{

//        private static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault());

        public static String instantToString(Instant instant){
            return instant.toString();
        }

        public static Instant stringToInstant(String instantString){
            return Instant.parse(instantString);
        }
    }



}
