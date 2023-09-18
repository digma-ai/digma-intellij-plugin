package org.digma.intellij.plugin.test.system.framework

import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.common.JBCefBrowserBuilderCreator
import org.digma.intellij.plugin.log.Log
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.lang.reflect.Field



fun createSpyBrowsers(): Pair<JBCefBrowser, CefBrowser> {
    val browserBuilder = JBCefBrowserBuilderCreator.create()
    val browser = browserBuilder
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

fun prepareDefaultSpyCalls(jbCaf: JBCefBrowser, caf: CefBrowser) {

    // mocking calls of JBCefBrowser
    Mockito.`when`(jbCaf.cefBrowser)
        .thenAnswer {
//            Log.test(logger::info, "getCefBrowser - of mockJBBrowser before real call, returning spy of CefBrowser")
            return@thenAnswer caf
        }
//        .thenReturn(caf)

    // mocking calls of CefBrowser
    Mockito.`when`(caf.url)
        .thenAnswer {
//            Log.test(logger::info, "getURL - mock URL")
            return@thenAnswer "http://mockURL/index.html"
        }

    Log.test(logger::info, "Spy default behavior set")
}

fun replaceExecuteJSWithAssertionFunction(spiedCaf: CefBrowser, assertion: (String) -> Unit) {
    Mockito.doAnswer { invocationOnMock ->
        Log.test(logger::info, "executeJS - of mock before real call ", Thread.currentThread().stackTrace)
        invocationOnMock.getArgument(0, String::class.java)
            .also { props ->
                val stripedPayload = props.substringAfter("window.postMessage(").substringBeforeLast(");")
                assertion(stripedPayload)
            }
    }.`when`(spiedCaf).executeJavaScript(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
}

fun clearSpyAssertion(spiadCaf: CefBrowser) {
    Mockito.doAnswer { invocationOnMock ->
        invocationOnMock.callRealMethod()
    }.`when`(spiadCaf).executeJavaScript(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())
}
    