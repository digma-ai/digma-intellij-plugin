package org.digma.intellij.plugin.model.rest.insights

import javax.swing.Icon

class GroupViewModel(val labelText: String, val icon: Icon)

class RouteInfo(val shortName: String, val schema: String)

class EndpointSchema {

    companion object {
        const val HTTP_SCHEMA: String = "epHTTP"
        const val RPC_SCHEMA: String = "epRPC"
        const val CONSUMER_SCHEMA: String = "epConsumer"
        const val SPAN_SCHEMA: String = "epSpan"


        @JvmStatic
        fun getRouteInfo(fullRouteName: String): RouteInfo {
            val schema = getSchema(fullRouteName)
            return RouteInfo(removeSchema(fullRouteName, schema), schema)
        }

        @JvmStatic
        private fun isOfType(fullRouteName: String, schema: String): Boolean{
            return fullRouteName.startsWith("$schema:")
        }

        @JvmStatic
        private fun getSchema(fullRouteName: String): String{
            if (isOfType(fullRouteName, HTTP_SCHEMA)) {
                return HTTP_SCHEMA
            }
            if (isOfType(fullRouteName, RPC_SCHEMA)) {
                return RPC_SCHEMA
            }
            if (isOfType(fullRouteName, CONSUMER_SCHEMA)) {
                return CONSUMER_SCHEMA
            }
            if (isOfType(fullRouteName, SPAN_SCHEMA)) {
                return SPAN_SCHEMA
            }
            return ""
        }

        @JvmStatic
        private fun removeSchema(fullRouteName: String, schema: String): String{
            if(schema == ""){
                return fullRouteName
            }
            return fullRouteName.replace("$schema:", "")
        }

        @JvmStatic
        fun adjustHttpRouteIfNeeded(endpointInsight: EndpointInsight) {
            val origValue = endpointInsight.route
            if (isOfType(origValue, HTTP_SCHEMA)) {
                return
            }
            if (isOfType(origValue, RPC_SCHEMA)) {
                return
            }
            if (isOfType(origValue, CONSUMER_SCHEMA)) {
                return
            }
            return
            // default behaviour, to be backward compatible, where did not have the scheme part of the route, so adding it as HTTP one
            //endpointInsight.route = "$HTTP_SCHEMA:$origValue"
        }
    }
}
