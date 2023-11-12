package org.digma.intellij.plugin.test.system.framework

import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.log.Log
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.lang.reflect.Field


/**
 * creates a spy of JBCefBrowser and CefBrowser, sets the default behavior of the spy and returns the pair.
 * this function will inject the spy CefBrowser into the spy JBCefBrowser.
 *
 * @return Pair of JBCefBrowser and CefBrowser
 */
fun createSpyBrowsers(): Pair<JBCefBrowser, CefBrowser> {
    val browserBuilder = JBCefBrowserBuilderCreator.create()
    val browser: JBCefBrowser = browserBuilder
        .setUrl("http://mockURL/index.html")
        .build()

    val cefBrowserField = try {
        browser.javaClass.getDeclaredField("myCefBrowser")
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "NoSuchFieldException: trying supper class")
        val superClass = browser.javaClass.superclass
        superClass.getDeclaredField("myCefBrowser")
    }
    cefBrowserField.isAccessible = true

    val cefBrowser: CefBrowser = cefBrowserField.get(browser) as CefBrowser
    val jbBrowserSpy = Mockito.spy(browser)
    val spiedCefBrowser: CefBrowser = Mockito.spy(cefBrowser)
    cefBrowserField.set(browser, spiedCefBrowser)

    prepareDefaultSpyCalls(jbBrowserSpy, spiedCefBrowser)

    return jbBrowserSpy to spiedCefBrowser
}


/**
 * injects the spy JBCefBrowser into the containingInstance.
 * @param containingInstance - the instance that contains the JBCefBrowser field.
 * @param jbBrowserFieldName - the name of the JBCefBrowser field.
 * @param jbBrowserSpy - the spy JBCefBrowser to be injected.
 *
 * @throws Exception if the field was not found in the containingInstance.
 */
fun <T> injectSpyBrowser(
    containingInstance: T,
    jbBrowserFieldName: String,
    jbBrowserSpy: JBCefBrowser,
) {

    // getting the browser field and setting it accessible to be able to get the browser
    val jbBrowserField: Field = try {
        containingInstance!!::class.java.getDeclaredField(jbBrowserFieldName)
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "No field of name: {} was found in class: {}", jbBrowserFieldName, containingInstance!!::class.java.name)
        throw Exception("No field of name: $jbBrowserFieldName was found in class: ${containingInstance!!::class.java.name}")
    }
    jbBrowserField.isAccessible = true
    jbBrowserField.set(containingInstance, jbBrowserSpy)
}

/**
 * sets the default behavior of the spy JBCefBrowser and CefBrowser.
 * @param jbCaf - spy JBCefBrowser
 * @param caf - spy CefBrowser that is the embedded browser in the JBCefBrowser.
 */
fun prepareDefaultSpyCalls(jbCaf: JBCefBrowser, caf: CefBrowser) {
    // mocking calls of JBCefBrowser
    Mockito.`when`(jbCaf.cefBrowser)
        .thenAnswer {
            return@thenAnswer caf
        }

    // mocking calls of CefBrowser
    Mockito.`when`(caf.url)
        .thenAnswer {
            return@thenAnswer "http://mockURL/index.html"
        }

    Log.test(logger::info, "Spy default behavior set")
}
    