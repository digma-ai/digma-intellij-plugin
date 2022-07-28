package org.digma.intellij.plugin.model.rest.insights

class EndpointSchema {

    companion object {
        const val HTTP_SCHEMA: String = "epHTTP:"
        const val RPC_SCHEMA: String = "epRPC:"
        const val CONSUMER_SCHEMA: String = "epConsumer:"

        // strips the scheme and returns the rest of the of name
        @JvmStatic
        fun getShortRouteName(fullRouteName: String): Pair<String, String> {
            if (fullRouteName.startsWith(HTTP_SCHEMA)) {
                return Pair(fullRouteName.replace(HTTP_SCHEMA, ""), HTTP_SCHEMA);
            }
            if (fullRouteName.startsWith(RPC_SCHEMA)) {
                return Pair(fullRouteName.replace(RPC_SCHEMA, ""), RPC_SCHEMA);
            }
            if (fullRouteName.startsWith(CONSUMER_SCHEMA)) {
                return Pair(fullRouteName.replace(CONSUMER_SCHEMA, ""), CONSUMER_SCHEMA);
            }
            // did not manage to find relevant Scheme, so returning value as is
            return Pair(fullRouteName, "");
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
            if (origValue.startsWith(CONSUMER_SCHEMA)) {
                return;
            }
            // default behaviour, to be backward compatible, where did not have the scheme part of the route, so adding it as HTTP one
            endpointInsight.route = HTTP_SCHEMA + origValue;
        }
    }
}
