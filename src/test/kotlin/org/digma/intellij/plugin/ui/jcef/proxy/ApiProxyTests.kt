package org.digma.intellij.plugin.ui.jcef.proxy

import org.digma.intellij.plugin.ui.jcef.ApiProxyResourceHandler
import org.digma.intellij.plugin.ui.jcef.ApiProxyResourceHandler.Companion.URL_PREFIX
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiProxyTests {


    @Test
    fun testUrls() {

        //this url is encoded, the path is
        val rawRequestUrl = "http://dashboard/api-proxy/reports/myservice%23test/endpoints/issues?env=TTT%23123&service=AAA%2311232"
        //this will also test that this URI constructor does not encode
        val requestUrl = URI(rawRequestUrl).toURL()

        //this URI constructor will not encode
        val baseUrl = URI("https://some.domain.com:8080").toURL()

        //we expect the result of ApiProxyResourceHandler.buildProxyUrl to have the same path and query as
        //the original requestUrl
        val apiUrl = ApiProxyResourceHandler.buildProxyUrl(baseUrl,rawRequestUrl)


        assertEquals(requestUrl.path.removePrefix(URL_PREFIX), apiUrl.path,"path does not match")
//        assertEquals(requestUrl.path, apiUrl.path,"path does not match")
        assertEquals(requestUrl.query, apiUrl.query,"query does not match")

    }

}