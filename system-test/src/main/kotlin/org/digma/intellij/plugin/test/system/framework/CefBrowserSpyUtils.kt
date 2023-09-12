package org.digma.intellij.plugin.test.system.framework

import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.log.Log
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.lang.reflect.Field


@Suppress("UNCHECKED_CAST")
fun <T, H> replaceCefBrowserWithSpy(
    containingService: T,
    messageHandlerFieldName: String,
    messageHandlerType: Class<H>,
    jbBrowserFieldName: String,
): Pair<JBCefBrowser, CefBrowser> {
    


    //getting the handler field and setting it accessible to be able to get the handler
    val handlerField: Field = try {
        containingService!!::class.java.getDeclaredField(messageHandlerFieldName)
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "No field of name: {} was found in class: {}", messageHandlerFieldName, containingService!!::class.java.name)
        throw Exception("No field of name: $messageHandlerFieldName was found in class: ${containingService!!::class.java.name}")
    }
    handlerField.isAccessible = true
    val routerHandler: H = handlerField.get(containingService) as H

    // getting the browser field and setting it accessible to be able to get the browser
    val jbBrowserField: Field = try {
        routerHandler!!::class.java.getDeclaredField(jbBrowserFieldName)
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "No field of name: {} was found in class: {}", jbBrowserFieldName, messageHandlerType.name)
        throw Exception("No field of name: $jbBrowserFieldName was found in class: ${messageHandlerType.name}")
    }
    jbBrowserField.isAccessible = true


    val browser: JBCefBrowser = jbBrowserField.get(routerHandler) as JBCefBrowser
    val spiedJBCefBrowser: JBCefBrowser = Mockito.spy(browser)
    jbBrowserField.set(routerHandler, spiedJBCefBrowser)

    val cefBrowserField = try {
        browser.javaClass.getDeclaredField("myCefBrowser")
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "NoSuchFieldException: trying supper class")
        val superClass = browser.javaClass.superclass
        superClass.getDeclaredField("myCefBrowser")
    }
    cefBrowserField.isAccessible = true


    val cefBrowser: CefBrowser = cefBrowserField.get(browser) as CefBrowser
    val spiedCefBrowser: CefBrowser = Mockito.spy(cefBrowser)
    cefBrowserField.set(browser, spiedCefBrowser)

    // return the spies
    return spiedJBCefBrowser to spiedCefBrowser // returning the spies to be able to mock the calls

}

@Suppress("UNCHECKED_CAST")
fun <T> replaceJBCefWithExistingSpy(
    containingService: T,
    jbBrowserFieldName: String,
    jbBrowserSpy: JBCefBrowser,
){

    // getting the browser field and setting it accessible to be able to get the browser
    val jbBrowserField: Field = try {
        containingService!!::class.java.getDeclaredField(jbBrowserFieldName)
    } catch (ex: NoSuchFieldException) {
        Log.test(logger::info, "No field of name: {} was found in class: {}", jbBrowserFieldName, containingService!!::class.java.name)
        throw Exception("No field of name: $jbBrowserFieldName was found in class: ${containingService!!::class.java.name}")
    }
    jbBrowserField.isAccessible = true
    jbBrowserField.set(containingService, jbBrowserSpy)
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
    