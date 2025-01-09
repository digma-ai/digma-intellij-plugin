package org.digma.intellij.plugin.ui.jcef.proxy

import org.digma.intellij.plugin.ui.jcef.ApiProxyResourceHandler
import org.digma.intellij.plugin.ui.jcef.ApiProxyResourceHandler.Companion.URL_PREFIX
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
        val apiUrl = ApiProxyResourceHandler.buildProxyUrl(baseUrl, rawRequestUrl)


        assertEquals(requestUrl.path.removePrefix(URL_PREFIX), apiUrl.path, "path does not match")
        assertEquals(requestUrl.query, apiUrl.query, "query does not match")

    }

    @Test
    fun testUriConstructor() {

        ///this method tests our assumptions about java.net.URI constructor


        //this url is encoded, the path is
        val rawRequestUrl = "http://dashboard/api-proxy/reports/myservice%23test/endpoints/issues?env=TTT%23123&service=AAA%2311232"
        //this will also test that this URI constructor does not encode
        val requestUrl = URI(rawRequestUrl).toURL()

        //this URI constructor will not encode
        val apiBaseUrl = URI("https://some.domain.com:8080").toURL()

        val encodedApiUrl = URI(
            apiBaseUrl.protocol,
            null,
            apiBaseUrl.host,
            apiBaseUrl.port,
            requestUrl.path?.let {
                URLDecoder.decode(it.removePrefix(URL_PREFIX), StandardCharsets.UTF_8)
            },
            requestUrl.query?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) },
            null
        ).toURL()

        val doubleEncodedApiUrl = URI(
            apiBaseUrl.protocol,
            null,
            apiBaseUrl.host,
            apiBaseUrl.port,
            requestUrl.path,
            requestUrl.query,
            null
        ).toURL()


        assertEquals(requestUrl.path.removePrefix(URL_PREFIX), encodedApiUrl.path, "path does not match")
        assertEquals(requestUrl.query, encodedApiUrl.query, "query does not match")
        assertNotEquals(requestUrl.path.removePrefix(URL_PREFIX), doubleEncodedApiUrl.path, "path does not match")
        assertNotEquals(requestUrl.query, doubleEncodedApiUrl.query, "query does not match")

    }

}