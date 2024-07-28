package org.digma.intellij.plugin.common

import org.junit.jupiter.api.assertThrows
import java.net.MalformedURLException
import java.net.URISyntaxException
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.fail

class URLValidatorTests {


    @Test
    fun testValid() {

        URLValidator.create("http://mydomain.com").validate()
        URLValidator.create("https://mydomain.com").validate()
        URLValidator.create("http://mydomain.com/").validate()
        URLValidator.create("http://mydomain.com/path1").validate()
        URLValidator.create("http://mydomain.com/path1/path2").validate()
        URLValidator.create("http://mydomain.com/path1/path2/").validate()

        URLValidator.create("http://20.128.165.123.com").validate()
        URLValidator.create("https://20.128.165.123.com").validate()
        URLValidator.create("http://20.128.165.123.com/").validate()
        URLValidator.create("http://20.128.165.123.com/path1").validate()
        URLValidator.create("http://20.128.165.123.com/path1/path2").validate()
        URLValidator.create("http://20.128.165.123.com/path1/path2/").validate()

        URLValidator.create("ftp://mydomain.com", listOf("ftp")).validate()
        URLValidator.create("ftp://mydomain.com/path1/path2", listOf("http", "https", "ftp")).validate()
        URLValidator.create("ftp://mydomain.com/path1/path2/", listOf("http", "https", "ftp")).validate()
        URLValidator.create("ftp://20.128.165.123.com", listOf("ftp")).validate()
        URLValidator.create("ftp://20.128.165.123.com/path1/path2", listOf("http", "https", "ftp")).validate()
        URLValidator.create("ftp://20.128.165.123.com/path1/path2/", listOf("http", "https", "ftp")).validate()



        URLValidator.create("https://mydomain.com:80").validate()
        URLValidator.create("https://mydomain.com:80/path1/path2").validate()
        URLValidator.create("https://20.128.165.123:80").validate()
        URLValidator.create("https://20.128.165.123:80/path1/path2").validate()


        var validator = URLValidator.create("http://mydomain.com/path1/path2?q1=1&q2=2")
        validator.allowQueryParams = true
        validator.validate()

        validator = URLValidator.create("http://mydomain.com/path1/path2#myFragment")
        validator.allowFragments = true
        validator.validate()

    }

    @Test
    fun testLocalhost() {
        URLValidator.create("http://localhost:5050").validate()
        URLValidator.create("http://localhost:5050/path1").validate()
        URLValidator.create("http://localhost:5050/path1/").validate()
        URLValidator.create("http://localhost:5050/path1/path2").validate()
        URLValidator.create("http://localhost:5050/path1/path2/").validate()

        URLValidator.create("http://127.0.0.1:5050").validate()
        URLValidator.create("http://127.0.0.1:5050/path1").validate()
        URLValidator.create("http://127.0.0.1:5050/path1/").validate()
        URLValidator.create("http://127.0.0.1:5050/path1/path2").validate()
        URLValidator.create("http://127.0.0.1:5050/path1/path2/").validate()
    }

    @Test
    fun testSlashesNotAllowed() {

        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://localhost:5050//").validate()
        }
        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://localhost:5050//path1").validate()
        }
        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://mydomain.com:5050//path1").validate()
        }
        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://localhost:5050/path1//").validate()
        }
        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://localhost:5050/path1//path2").validate()
        }
        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://localhost:5050/path1//path2/").validate()
        }

        val validator = URLValidator.create("http://mydomain.com//path1//")
        validator.allowTwoSlashes = true
        validator.validate()

    }


    @Test
    fun testInvalid() {
        assertThrows<MalformedURLException> {
            URLValidator.create("").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("http////:mydomain///.com").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("https//mydomain.com").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("https//mydomain.com#myfragment").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("http//mydomain.com/path1/?q1=1&q2=2").validate()
        }


        //in java 17 some will throw URISyntaxException, in java 21 some will throw MalformedURLException
        assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {
            URLValidator.create("https:// mydomain.com/ /").validate()
        }
        assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {
            URLValidator.create("https:// mydomain.com").validate()
        }
        assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {
            URLValidator.create("https: //mydomain.com").validate()
        }
        assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {
            URLValidator.create("https://mydomain. com").validate()
        }


        assertThrows<MalformedURLException> {
            URLValidator.create("htt:mydomain.com").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("htt//mydomain:.com///").validate()
        }

        assertThrows<MalformedURLException> {
            URLValidator.create("http://:mydomain.com").validate()
        }

        assertThrows<URLValidator.QueryNotAllowedException> {
            URLValidator.create("http://mydomain.com?q1=1&q2=2").validate()
        }

        assertThrows<URLValidator.QueryNotAllowedException> {
            URLValidator.create("http://mydomain.com/path1/?q1=1&q2=2").validate()
        }

        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("http://mydomain.com?#mtfragment").validate()
        }

        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("https://\$mydomain.com").validate()
        }

        assertThrows<URLValidator.InvalidUrlException> {
            URLValidator.create("https://+mydomain.com").validate()
        }

        assertThrows<URLValidator.IncorrectSchemaException> {
            URLValidator.create("http://mydomain.com", listOf("https")).validate()
        }

        assertThrows<URLValidator.IncorrectSchemaException> {
            URLValidator.create("https://mydomain.com", listOf("http")).validate()
        }

    }


    @Test
    fun testAssertThrowsAny() {
        assertFails {
            assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {
                throw NullPointerException("test")
            }
        }

        assertFails {
            assertThrowsAny(URISyntaxException::class.java, MalformedURLException::class.java) {

            }
        }

        assertThrowsAny(NullPointerException::class.java, MalformedURLException::class.java) {
            throw NullPointerException("test")
        }
    }


    private fun assertThrowsAny(vararg types: Class<*>, block: () -> Unit) {
        try {
            block.invoke()
            fail("expected exception any of ${types.map { it.name }}, but nothing was thrown")
        } catch (e: Throwable) {
            if (types.contains(e.javaClass)) {
                return
            } else {
                fail("expected exception any of ${types.map { it.name }}, but was ${e.javaClass.name}")
            }
        }
    }


}