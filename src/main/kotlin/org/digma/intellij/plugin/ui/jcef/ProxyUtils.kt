package org.digma.intellij.plugin.ui.jcef

import com.intellij.openapi.diagnostic.Logger
import org.cef.network.CefPostData
import org.cef.network.CefPostDataElement
import org.cef.network.CefRequest
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import java.util.Vector


private val logger = Logger.getInstance("org.digma.intellij.plugin.ui.jcef.ProxyUtils")

//this method will create a byte array containing all byte arrays from all elements.
// when calling this method we make sure there are no file elements and that the number of elements not more than 1
fun postDataToByteArray(request: CefRequest, postData: CefPostData): ByteArray {

    Log.log(logger::trace, "collecting post data for {}, [request id:{}]", request.url, request.identifier)

    return try {
        val elements = Vector<CefPostDataElement>()
        postData.getElements(elements)

        var allBytes = ByteArray(0)

        elements.forEach { e ->
            val bytesCnt: Int = e.bytesCount
            if (bytesCnt > 0) {
                val bytes = ByteArray(bytesCnt)
                e.getBytes(bytes.size, bytes)
                allBytes = allBytes.plus(bytes)
                if (logger.isTraceEnabled) {
                    //check if trace enabled because this log message may build large strings
                    Log.log(
                        logger::trace, "collected cef post data from element, data is: {}, all post data is:{}, for {}, [request id:{}]",
                        String(bytes, Charsets.UTF_8),
                        String(allBytes, Charsets.UTF_8),
                        request.url, request.identifier
                    )
                }
            }
        }

        if (logger.isTraceEnabled) {
            Log.log(
                logger::trace,
                "built post data {}, for {}, [request id:{}]",
                String(allBytes, Charsets.UTF_8),
                request.url,
                request.identifier
            )
        }
        allBytes

    } catch (e: Throwable) {
        Log.warnWithException(logger, e, "postDataToByteArray for {} failed, [request id:{}]", request.url, request.identifier)
        ErrorReporter.getInstance().reportError("ApiProxyResourceHandler.postDataToByteArray", e)
        ByteArray(0)
    }
}




fun hasFileElements(postData: CefPostData?): Boolean {
    return postData?.let {
        val elements = Vector<CefPostDataElement>()
        postData.getElements(elements)
        elements.any { it.type == CefPostDataElement.Type.PDE_TYPE_FILE }
    } ?: false
}