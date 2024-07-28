package org.digma.intellij.plugin.common

import org.apache.commons.validator.routines.UrlValidator
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

class URLValidator(
    private val spec: String,
    private val allowedSchemas: List<String>? = null,
    var allowQueryParams: Boolean = false,
    var allowFragments: Boolean = false,
    var allowTwoSlashes: Boolean = false
) {

    private val defaultAllowedSchemas = listOf("http", "https", "ftp")

    companion object {

        @JvmStatic
        fun create(spec: String): URLValidator {
            return URLValidator(spec)
        }

        @JvmStatic
        fun create(spec: String, schemas: List<String>): URLValidator {
            return URLValidator(spec, schemas)
        }

    }


    @Throws(
        MalformedURLException::class,
        URISyntaxException::class,
        InvalidUrlException::class,
        QueryNotAllowedException::class,
        IncorrectSchemaException::class
    )
    fun validate() {

        //we could check only with apache UrlValidator, but it only has true/false,
        // URI(spec).toURL() will throw a detailed exception.
        //but java URI and URL don't check illegal characters,apache UrlValidator does

        //in java 17 URL("http:// mydomain.com) will not throw MalformedURLException in java 21 it will.
        //URI("http:// mydomain.com) will throw URISyntaxException
        //URI("") will throw IllegalArgumentException , but URL("") will throw MalformedURLException
        //we prefer the MalformedURLException, so first check URL constructor then URI, and deal with all these exceptions

        //always allow localhost.

        val url = URL(spec)
        URI(spec).toURL()

        val schemas = allowedSchemas ?: defaultAllowedSchemas
        if (!schemas.contains(url.protocol)) {
            throw IncorrectSchemaException("Protocol must be one of [${schemas.joinToString(",")}]")
        }

        var options = UrlValidator.ALLOW_LOCAL_URLS //always allow localhost
        if (!allowFragments) {
            options += UrlValidator.NO_FRAGMENTS
        }
        if (allowTwoSlashes) {
            options += UrlValidator.ALLOW_2_SLASHES
        }


        val urlValidator = UrlValidator(schemas.toTypedArray(), options)

        if (!urlValidator.isValid(spec)) {
            throw InvalidUrlException("Url $spec is invalid")
        }

        if (!allowQueryParams && !url.query.isNullOrBlank()) {
            throw QueryNotAllowedException("Query params not allowed")
        }

    }


    class InvalidUrlException(message: String) : Exception(message)
    class IncorrectSchemaException(message: String) : Exception(message)
    class QueryNotAllowedException(message: String) : Exception(message)

}


