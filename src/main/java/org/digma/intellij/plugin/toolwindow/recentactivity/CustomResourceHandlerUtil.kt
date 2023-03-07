package org.digma.intellij.plugin.toolwindow.recentactivity

import org.cef.callback.CefCallback
import org.cef.misc.IntRef
import java.io.InputStream
import kotlin.math.min

class CustomResourceHandlerUtil {

    companion object {
        fun readResponse(
                inputStream: InputStream,
                dataOut: ByteArray,
                designedBytesToRead: Int,
                bytesRead: IntRef,
                callback: CefCallback
        ): Boolean {
            val availableSize = inputStream.available()
            return if (availableSize > 0) {
                val maxBytesToRead = min(availableSize, designedBytesToRead)
                val realNumberOfReadBytes =
                        inputStream.read(dataOut, 0, maxBytesToRead)
                bytesRead.set(realNumberOfReadBytes)
                true
            } else {
                inputStream.close()
                false
            }
        }
    }
}