package org.digma.intellij.plugin.idea.psi.discovery.endpoint

class EndpointDiscoveryUtils {

    companion object {

        /**
         * combineUri. used for combining controller defined route and specific endpoint
         * the output of it will always start with / .
         */
        @JvmStatic
        fun combineUri(prefix: String?, suffix: String?): String {
            if (suffix.isNullOrBlank()) {
                return adjustUri(prefix)
            }
            if (prefix.isNullOrBlank()) {
                return adjustUri(suffix)
            }
            return adjustUri(adjustUri(prefix) + adjustUri(suffix))
        }

        /**
         * adjustUri.
         * 1. trim leading and trailing spaces
         * 2. make sure it starts with /
         * 3. make sure it does not end with / (unless its only /)
         * 4. replace all // to single /
         */
        @JvmStatic
        fun adjustUri(value: String?): String {
            if (value.isNullOrBlank()) return "/"
            var adjusted = value.trim().trimEnd('/').trimStart('/').trim()
            adjusted = adjusted.replace("/////", "/")
            adjusted = adjusted.replace("////", "/")
            adjusted = adjusted.replace("///", "/")
            adjusted = adjusted.replace("//", "/")
            return if (adjusted.startsWith("/")) {
                adjusted
            } else {
                "/$adjusted"
            }
        }
    }
}