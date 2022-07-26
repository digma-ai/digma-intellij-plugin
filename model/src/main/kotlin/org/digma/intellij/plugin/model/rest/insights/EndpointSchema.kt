package org.digma.intellij.plugin.model.rest.insights

class EndpointSchema {

    companion object {
        const val HTTP_SCHEMA: String = "epHTTP:"
        const val RPC_SCHEMA: String = "epRPC:"

        // strips the scheme and returns the rest of the of name
        @JvmStatic
        fun getShortRouteName(fullRouteName: String): String {
            if (fullRouteName.startsWith(HTTP_SCHEMA)) {
                return fullRouteName.replace(HTTP_SCHEMA, "");
            }
            if (fullRouteName.startsWith(RPC_SCHEMA)) {
                return fullRouteName.replace(RPC_SCHEMA, "");
            }
            // did not manage to find relevant Scheme, so returning value as is
            return fullRouteName;
        }

        @JvmStatic
        fun adjustHttpRouteIfNeeded(endpointInsight: EndpointInsight) {
            val origValue = endpointInsight.route
            if (origValue.startsWith(HTTP_SCHEMA)) {
                return;
            }
            if (origValue.startsWith(RPC_SCHEMA)) {
                return;
            }
            // default behaviour, to be backward compatible, where did not have the scheme part of the route, so adding it as HTTP one
            endpointInsight.route = HTTP_SCHEMA + origValue;
        }
    }
}
